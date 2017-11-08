package sanp.javalon.media.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

import sanp.tools.utils.LogManager;
import sanp.javalon.media.base.AVChannel;
import sanp.javalon.media.base.AVPacket;

public class VideoDecoderWithChannel implements Runnable {
    
    private static final byte[] START_CODE = {0,0,0,1};
    private static final long   THREAD_EXIT_TIMEOUT_MS = 1000;
    private static final long   WAIT_PKT_TIMEOUT_MS = 33; // = 1000ms/60fps*2
    private static final long   WAIT_PKT_TIMEOUT_US = WAIT_PKT_TIMEOUT_MS * 1000;
    
    private Surface m_output_surface = null;
    private AVChannel m_reading_channel = null;
    private MediaCodec m_codec = null;
    private Thread m_thread = null;

    private boolean m_decoding_started = false;
    private long m_first_in_time_ms = -1;
    
    public VideoDecoderWithChannel(Surface output_surface, AVChannel rChannel) {
        
        m_output_surface = output_surface;
        m_reading_channel = rChannel;
        
        // Create a MediaCodec decoder, and configure it with the MediaFormat from the
        // extractor.  It's very important to use the format from the extractor because
        // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
        MediaFormat format = m_reading_channel.getMediaFormat();
        String mime = format.getString(MediaFormat.KEY_MIME);
        try {
            m_codec = MediaCodec.createDecoderByType(mime);
            m_codec.configure(format, m_output_surface, null, 0);
        } catch (IOException e) {
            throw new RuntimeException("Create decoder for MIME type " + mime + " failed!");
        }
        
        m_thread = new Thread(this, "VideoDecoder");
    }
    
    public void start() {
        m_first_in_time_ms = -1;
        m_codec.start();
        m_decoding_started = true;
        m_thread.start();
    }

    public void stop() {
        
        m_decoding_started = false;
        {
            // Stop stream -- send empty frame.
            int inputBufIndex = m_codec.dequeueInputBuffer(0);
            if(inputBufIndex >= 0) {
                m_codec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
            m_codec.stop();
        }
        
        {
            AVPacket pkt = m_reading_channel.pollIdlePacket();
            if(pkt != null) {
                pkt.reset();
                m_reading_channel.offerBusyPacket(pkt);
            }
            try {  m_thread.join(THREAD_EXIT_TIMEOUT_MS); } catch (InterruptedException e) {LogManager.e(e); }
        }
        
        m_first_in_time_ms = -1;
    }
    
    public void reset() {
        if(m_decoding_started) {
            m_codec.flush();    // reset decoder state
            m_first_in_time_ms = -1;
        }
    }
    
    class BreakoutThread extends Exception {}
    void checkState() throws BreakoutThread {
        if(!m_decoding_started) throw new BreakoutThread();
    }
    
    @Override
    public void run() {
        
        LogManager.i("VideoDecoder thread started!");
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] codec_input_buffers = m_codec.getInputBuffers();
        AVPacket pkt = null;
        int input_buf_index = -1;
        int frame_cnt = 0;
        
        try {
            while(true) {
                
                do{ //put-in encoded frame to MC
                    
                    checkState();
                    if(pkt == null) {
                        pkt = m_reading_channel.pollBusyPacket(WAIT_PKT_TIMEOUT_MS);
                        if(pkt == null)
                            break; //goto `get-out decoded frame from MC`
                        checkState();
                    }
                    
                    input_buf_index = m_codec.dequeueInputBuffer(WAIT_PKT_TIMEOUT_US);
                    if(input_buf_index < 0) {
                        LogManager.w("decoder input buffer is full, try later");
                        break; //goto `get-out decoded frame from MC`
                    }
                    checkState();

                    if(pkt.isEmpty()) {
                        // it indicates AVChannel will be closed
                        throw new BreakoutThread();
                    } else {
                        long timestamp_us = pkt.getPts();
                        int pkt_length = pkt.getPayload().limit();
                        ByteBuffer inputBuf = codec_input_buffers[input_buf_index];
                        try {
                            // Read the sample data into the ByteBuffer.  This neither respects nor
                            // updates inputBuf's position, limit, etc.
                            inputBuf.clear();
                            inputBuf.put(pkt.getPayload());
                            inputBuf.flip();
                        } catch (BufferOverflowException e) { 
                            LogManager.w("this ByteBuffer in MediaCodec is not enough for the frame[" + pkt_length + "]");
                        }
                        
                        pkt.reset();
                        m_reading_channel.putIdlePacket(pkt);
                        pkt = null;
                        
                        if (m_first_in_time_ms == -1) {
                            m_first_in_time_ms = System.currentTimeMillis();
                        }
                        
                        m_codec.queueInputBuffer(input_buf_index, 0, pkt_length, timestamp_us, 0/*flags*/);
                        frame_cnt++;
                        input_buf_index = -1;
                        //LogManager.d("Put " + new_datas.length + "bytes to MC, totally " + frame_cnt + "frames");
                    }
                    
                } while(false);
                

                while(true) { //get-out all decoded frames from MC
                    
                    checkState();
                    int decoderStatus = m_codec.dequeueOutputBuffer(bufferInfo, 0);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        break;
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not important for us, since we're using Surface
                        LogManager.d("Decoder output buffers changed");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = m_codec.getOutputFormat();
                        LogManager.d("Decoder output format changed: " + newFormat);
                    } else if (decoderStatus < 0) {
                        throw new RuntimeException(
                                "unexpected result from decoder.dequeueOutputBuffer: " +
                                        decoderStatus);
                    } else { // decoderStatus >= 0
                        
                        //LogManager.d("Decoded buffer with status-" + decoderStatus +
                        //        " (size=" + bufferInfo.size + ")");
                        if (m_first_in_time_ms != 0) {
                            // Log the delay from the first buffer of input to the first buffer
                            // of output.
                            LogManager.i("First decoded frame delays " + (System.currentTimeMillis()-m_first_in_time_ms) + " ms");
                            m_first_in_time_ms = 0;
                        }
                        
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            LogManager.i("End of decoding");
                        }
                        
                        m_codec.releaseOutputBuffer(decoderStatus, bufferInfo.size != 0);
                    }
                }
            }
        } catch (BreakoutThread | InterruptedException e) {
            if(pkt != null) {
                pkt.reset();
                m_reading_channel.putIdlePacket(pkt);
                pkt = null;
            }
            
            if(input_buf_index >= 0) {
                m_codec.queueInputBuffer(input_buf_index, 0, 0, 0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                input_buf_index = -1;
                LogManager.i("Mark end of stream for MediaCodec");
            }
        }
        LogManager.i("VideoDecoder thread exit...!");
    }
    
    public static boolean addStartCode(byte []src, ByteBuffer dst) {
        if(dst.capacity() < (src.length + START_CODE.length))
            return false;
        dst.put(START_CODE);
        dst.put(src);
        dst.flip();
        return true;
    }
}

