package sanp.avalon.libs.media.format;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import sanp.avalon.libs.media.base.AVDefines;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVPacket;
import sanp.avalon.libs.media.base.AVDefines.DataFlag;
import sanp.tools.utils.LogManager;

public class MP4Muxer implements Runnable {
    
    private static final long THREAD_EXIT_TIMEOUT_MS = 1000;
    private static final int  BUFFERED_PACKET_COUNT = 5;
    private static final long WAIT_PKT_TIMEOUT_MS = 33; // = 1000ms/30fps
    private static final long DEBUG_FILE_LITMIT_MS = 600 * 1000;
    private static final boolean DEBUG_FILE_LITMIT = false;

    private boolean m_track_completed = false;
    private boolean m_started = false;

    private MediaMuxer m_mp4_muxer = null;
    private Thread m_thread = null;
    private AVChannel m_writing_que = new AVChannel(BUFFERED_PACKET_COUNT);
    private Map<Integer, MediaFormat> m_formats = new HashMap<>();
    private Map<Integer, Integer> m_trackId2mp4Ids = new HashMap<>();
    
    private int track_id_ref = 0;
    
    public MP4Muxer(String filename) throws IOException {
        LogManager.i("Write MP4 file to " + filename);
        m_mp4_muxer = new MediaMuxer(filename,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        m_thread = new Thread(this, "MP4Muxer");
    }
    
    public void release() {
        stop();
        
        if(m_mp4_muxer != null) {
            m_mp4_muxer.release();
            m_mp4_muxer = null;
        }
        
        m_thread = null;
        m_writing_que = null;
        
        if(m_formats != null) {
            m_formats.clear();
            m_formats = null;
        }
        
        if(m_trackId2mp4Ids != null) {
            m_trackId2mp4Ids.clear();
            m_trackId2mp4Ids = null;
        }
    }
    
    public Map<Integer, MediaFormat> enumTracks() {
        return m_formats;
    }
    
    private ByteBuffer[] splitCSD(ByteBuffer csd) {
        int MG4 = ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x00, 0x01}).getInt();
        int offset = 0;
        try {
            csd.position(0);
            if(csd.getInt(offset) != MG4)
                return null;
            offset = 4;
            while(csd.getInt(offset) != MG4)
                ++offset;
        } catch(BufferUnderflowException | IndexOutOfBoundsException e) {
            return null;
        }
        
        ByteBuffer[] csds = {
            ByteBuffer.wrap(csd.array(), 0, offset),
            ByteBuffer.wrap(csd.array(), offset, csd.limit()-offset),
        };
        return csds;
    }
    
    public int cfgTrack(int track_id, ByteBuffer csd /*codec-specific data:sps+pps*/) {
        if(m_trackId2mp4Ids.containsKey(track_id)) {
            throw new RuntimeException("csd had been added while calling addTrack");
        }
        if(!m_formats.containsKey(track_id)) {
            throw new RuntimeException("call addTrack first");
        }

        MediaFormat format = m_formats.get(track_id);
        String name = format.getString(MediaFormat.KEY_MIME);
        if(name.startsWith("video")) {
            ByteBuffer[] csds = splitCSD(csd);
            if(csds == null)
                throw new RuntimeException("logical error, csd's format is invalid" + csd.array());
            format.setByteBuffer("csd-0", csds[0]);
            format.setByteBuffer("csd-1", csds[1]);
        } else if (name.startsWith("audio")) {
            format.setByteBuffer("csd-0", csd);
        } else {
            throw new RuntimeException("logical error, unknow codec name:" + name);
        }

        int mp4_id = m_mp4_muxer.addTrack(format);
        if(mp4_id < 0)
            return mp4_id;
        
        m_trackId2mp4Ids.put(track_id, mp4_id);
        if(m_trackId2mp4Ids.size() == m_formats.size())
            m_track_completed = true;
        return track_id;
    }

    public int addTrack(MediaFormat format) {
        if(m_started) {
            throw new RuntimeException("addTrack MUST be before start");
        }

        if(!format.containsKey("csd-0")) {
            m_formats.put(track_id_ref, format);
            m_track_completed = false;
            return track_id_ref++;
        }
        
        int mp4_id = m_mp4_muxer.addTrack(format);
        if(mp4_id < 0)
            return mp4_id;
        
        m_formats.put(track_id_ref, format);
        m_trackId2mp4Ids.put(track_id_ref, mp4_id);
        if(m_trackId2mp4Ids.size() == m_formats.size())
            m_track_completed = true;
        return track_id_ref++;
    }

    public AVChannel getChannel() {
        return m_writing_que;
    }

    public void start() {
        if(m_formats.size() == 0) {
            throw new RuntimeException("addTrack first");
        }
        
        m_started = true;
        m_thread.start();
        if(m_track_completed)
            m_mp4_muxer.start();
    }
    
    public void stop() {
        m_started = false;
        {
            AVPacket pkt = m_writing_que.pollIdlePacket();
            if(pkt != null) {
                pkt.reset();
                m_writing_que.offerBusyPacket(pkt);
            }
            try {  m_thread.join(THREAD_EXIT_TIMEOUT_MS); } catch (InterruptedException e) {LogManager.e(e); }
        }
        //stop m_mp4_muxer in the loop thread
    }
    
    class BreakoutThread extends Exception {}
    private void checkState() throws BreakoutThread {
        if(!m_started) throw new BreakoutThread();
    }
    
    private AVPacket resetPkt(AVPacket pkt) {
        pkt.reset();
        m_writing_que.putIdlePacket(pkt);
        return null;
    }
    
    @Override
    public void run() {
        
        LogManager.i("MP4Muxer thread started!");
        AVPacket pkt = null;
        long start_pts = -1;
        MediaCodec.BufferInfo buff_info = new MediaCodec.BufferInfo();
        boolean is_wait_iframe = true;
        long encoded_frames = 0;
        long start_time_ms = 0;

        try {
            while(true) {
                checkState();
                
                pkt = m_writing_que.pollBusyPacket(WAIT_PKT_TIMEOUT_MS);
                if(pkt == null)
                    continue;
                checkState();

                ByteBuffer payload = pkt.getPayload();
                int track_id = pkt.getTrackIndex();

                if(!m_track_completed && pkt.getDataFlag() == DataFlag.CODEC_SPECIFIC_DATA) {
                    int position = payload.position();
                    byte[] csd = new byte[payload.limit()-position];
                    payload.get(csd);
                    payload.position(position);
                    cfgTrack(track_id, ByteBuffer.wrap(csd));
                    if(m_track_completed) {
                        m_mp4_muxer.start();
                        start_pts = -1;
                        start_time_ms = 0;
                        checkState();
                    }

                    pkt = resetPkt(pkt);
                    continue;
                }

                if(is_wait_iframe && pkt.getMediaType() == AVDefines.DataType.VIDEO) {
                    if(!pkt.isKeyFrame()) {
                        pkt = resetPkt(pkt);
                        continue;
                    }
                    is_wait_iframe = false;
                }
                
                if(m_track_completed) {
                    if(start_time_ms == 0)
                        start_time_ms = System.currentTimeMillis();
                    if(DEBUG_FILE_LITMIT && System.currentTimeMillis() - start_time_ms > DEBUG_FILE_LITMIT_MS) {
                        // drop frames
                    } else {
                        if(start_pts < 0)
                            start_pts = pkt.getPts();
                        long pts = pkt.getPts();
                        // long pts = pkt.getPts() - start_pts;
                        buff_info.set(payload.position(), payload.limit()-payload.position(), pts, pkt.getCodecFlags());
                        int mp4_id = m_trackId2mp4Ids.get(track_id);
                        m_mp4_muxer.writeSampleData(mp4_id, payload, buff_info);
                        // LogManager.d(String.format("track-%d offset-%d size-%d pts-%d flags-%d", track_id, buff_info.offset, buff_info.size, buff_info.presentationTimeUs, buff_info.flags));
                        if(pkt.getMediaType() == AVDefines.DataType.VIDEO) {
                            ++encoded_frames;
                        }
                    }
                }
            
                pkt = resetPkt(pkt);
            }
        } catch (BreakoutThread | InterruptedException e) {
            if(pkt != null) {
                pkt = resetPkt(pkt);
            }
        }
        if(start_pts >= 0) {
            // don't call stop() if we haven't written anything.
            m_mp4_muxer.stop();    
        }
        LogManager.i("MP4Muxer thread exit...! with " + encoded_frames + " frames");
    }
}
