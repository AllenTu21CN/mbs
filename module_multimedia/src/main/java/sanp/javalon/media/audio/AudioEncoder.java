package sanp.javalon.media.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.test.SimpleTesting;
import sanp.tools.utils.LogManager;
import sanp.javalon.media.base.AVChannel;
import sanp.javalon.media.base.AVDefines;
import sanp.javalon.media.base.AVPacket;

/**
 * Created by Tuyj on 2017/6/9.
 */

public class AudioEncoder implements AudioCapturer.Callback {

    public interface Callback {
        void onData(ByteBuffer data, long ptsUs, int flags);
    }

    public static final class Supporting {
        Supporting() {
        }
        static public final String ENC_NAME_AAC = MediaFormat.MIMETYPE_AUDIO_AAC;
        static public final String ENC_NAME_G711A = MediaFormat.MIMETYPE_AUDIO_G711_ALAW;
        static public final String ENC_NAME_G711U = MediaFormat.MIMETYPE_AUDIO_G711_MLAW;
        static public final List<String> ENC_NAMEs = Arrays.asList(ENC_NAME_AAC, ENC_NAME_G711A, ENC_NAME_G711U);

        static public final int ENC_AAC_SAMPLERATE_MIN = 8000;
        static public final int ENC_AAC_SAMPLERATE_MAX = 96000;
        static public final int ENC_AAC_PROFILE_LC = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        static public final int ENC_AAC_PROFILE_HE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;
        static public final int ENC_AAC_PROFILE_HEv2 = MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS;
        static public final int ENC_AAC_PROFILE_LD = MediaCodecInfo.CodecProfileLevel.AACObjectLD;
        static public final int ENC_AAC_PROFILE_RECOMMENDED = ENC_AAC_PROFILE_LC;
        static public final List<Integer> ENC_AAC_PROFILEs = Arrays.asList(ENC_AAC_PROFILE_LC, ENC_AAC_PROFILE_HE, ENC_AAC_PROFILE_HEv2, ENC_AAC_PROFILE_LD);

        static public final int ENC_G711_SAMPLERATE = 8000;
        static public final int ENC_G711_CHANNEL_CNT = 1;
        static public final int ENC_G711_SAMPLE_WIDTH_IN_BYTES = 2;
        static public final int ENC_G711_BITRATE = 64000;
    }

    public static class Format {
        public String CodecName = null;
        public int Bitrate = -1;
        public int Profile = -1;
        public Format() {
        }
        public Format(String name, int bitrate, int profile) {
            CodecName = name;
            Bitrate = bitrate;
            Profile = profile;
        }
        public void from(Format other) {
            CodecName = other.CodecName;
            Bitrate = other.Bitrate;
            Profile = other.Profile;
        }
        public boolean isEqual(Format other) {
            return (CodecName.equals(other.CodecName) &&
                    Bitrate == other.Bitrate &&
                    Profile == other.Profile);
        }
    }

    static private final long DefaultThreadExitTimeoutMs = 1000;
    static private final int DefaultEncodeTimeoutMs = 50;
    static private final String DefaultFileTestingPath = "/sdcard/audio_enc.%s";

    Format mOutFormat = null;
    AudioCapturer.Format mInFormat = null;

    private MediaFormat mMediaFormat = null;
    private CommonEncoder mEncoder = null;
    private Thread mOutputThread = null;
    private boolean mRunning = false;
    private List<Callback> mCallbacks = new ArrayList<>();
    private Testing mTesting = null;

    public AudioEncoder() {
    }

    public int init(String codeName) {
        return init(codeName, -1, -1, -1, -1, -1, -1);
    }

    public int init(AudioCapturer.Format inFormat, Format outFormat) {
        return init(outFormat.CodecName, outFormat.Bitrate, outFormat.Profile,
                inFormat.SampleRate, inFormat.ChannelsCnt, inFormat.SampleWidthInBytes, inFormat.FrameDurationMs);
    }

    public int init(String codeName, int bitrate, int profile, int sampleRate, int channelsCnt, int pcmSampleWidthInBytes, int pcmFrameDurationMs) {
        if(mEncoder != null) {
            LogManager.w("had inited");
            return 0;
        }

        mOutFormat = new Format(codeName, bitrate, profile);
        mInFormat = new AudioCapturer.Format(sampleRate, channelsCnt, pcmSampleWidthInBytes, pcmFrameDurationMs);
        if(!isValid(mOutFormat))
            return -1;
        if(!isValid(codeName, mInFormat))
            return -1;

        mMediaFormat = new MediaFormat();
        mMediaFormat.setString(MediaFormat.KEY_MIME, codeName);
        if(codeName.equals(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || codeName.equals(MediaFormat.MIMETYPE_AUDIO_G711_MLAW)) {
            mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, G711Encoder.DEFAULT_RAW_FRAME_SIZE_IN_BYTES);
            mEncoder = new G711Encoder();
        } else {
            if(codeName.equals(Supporting.ENC_NAME_AAC)) {
                mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
                mMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
                mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelsCnt);
                if(pcmFrameDurationMs > 0 && pcmSampleWidthInBytes > 0)
                    mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, sampleRate * pcmFrameDurationMs / 1000 * channelsCnt * pcmSampleWidthInBytes * 2);
            }
            mEncoder = new AndroidEncoder();
        }
        return mEncoder.init(mMediaFormat);
    }

    public int close() {
        if(mTesting != null)
            enableTesting(false);
        stop();
        if(mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }
        mOutFormat = null;
        mInFormat = null;
        mCallbacks.clear();
        return 0;
    }

    public int start() {
        if(mEncoder == null) {
            LogManager.e("audio encoder init first");
            return -1;
        }
        if(mRunning && mOutputThread != null)
            return 0;
        mOutputThread = new Thread(new Runnable() {
            public void run() { outputThreadLoop(); }
        }, String.format("AudioEncoder(%s) OutputThread", mOutFormat.CodecName));
        mOutputThread.start();
        return 0;
    }

    public int stop() {
        mRunning = false;
        if(mOutputThread != null) {
            try {
                mOutputThread.join(DefaultThreadExitTimeoutMs);
                //mOutputThread.interrupt();
            } catch (InterruptedException e) {
               LogManager.e(e);
            }
            mOutputThread = null;
        }
        return 0;
    }

    public int reset() {
        stop();
        mEncoder.reinit(mMediaFormat);
        return 0;
    }

    public void addCallback(Callback cb) {
        synchronized(mCallbacks) {
            if(!mCallbacks.contains(cb))
                mCallbacks.add(cb);
        }
    }

    public void removeCallback(Callback cb) {
        synchronized(mCallbacks) {
            mCallbacks.remove(cb);
        }
    }

    // Implementation of AudioCapturer.Callback
    public void onData(ByteBuffer pcmFrame, AudioCapturer.Format format, long ptsUs) {
        // TODO: check `format` with `mInFormat`
        if (!mRunning)
            return;
        if (mEncoder != null)
            mEncoder.encode(pcmFrame, ptsUs);
    }

    private void outputThreadLoop() {
        mEncoder.start();

        mRunning = true;
        LogManager.i("Start to audio encode");
        while (mRunning) {

            ByteBuffer encodedData = mEncoder.getEncodedFrame(DefaultEncodeTimeoutMs);
            if(encodedData == null)
                continue;
            MediaCodec.BufferInfo bufferInfo = mEncoder.getEncodedInfo();

            //LogManager.d(String.format("Encoder output %d bytes with pts %d", encodedData.limit()-encodedData.position(), bufferInfo.presentationTimeUs));
            int pos = encodedData.position();
            synchronized(mCallbacks) {
                for(Callback cb: mCallbacks) {
                    cb.onData(encodedData, bufferInfo.presentationTimeUs, bufferInfo.flags);
                    encodedData.position(pos);
                }
            }

            mEncoder.releaseEncodedFrame();
        }
        mRunning = false;
        mEncoder.stop();
        LogManager.i("Exit to audio encode loop");
    }

    public void enableTesting(boolean on) {
        if(mEncoder == null) {
            LogManager.e("audio encoder init first");
            return;
        }
        if(on) {
            if(mTesting != null) {
                removeCallback(mTesting);
                mTesting.stop();
                mTesting = null;
            }

            if(mOutFormat.CodecName.equals(Supporting.ENC_NAME_AAC)) {
                String filePath = String.format(DefaultFileTestingPath, "aac");
                mTesting = new Debug2FileTesting(filePath, mInFormat.SampleRate, mInFormat.ChannelsCnt, mOutFormat.Profile);
            } else if(mOutFormat.CodecName.equals(Supporting.ENC_NAME_G711A)) {
                String filePath = String.format(DefaultFileTestingPath, "g711a");
                mTesting = new Debug2FileTesting(filePath);
            } else if(mOutFormat.CodecName.equals(Supporting.ENC_NAME_G711U)) {
                String filePath = String.format(DefaultFileTestingPath, "g711u");
                mTesting = new Debug2FileTesting(filePath);
            }

            try {
                mTesting.start();
            } catch (Exception e) {
               LogManager.e(e);
                mTesting.stop();
                mTesting = null;
                return;
            }
            addCallback(mTesting);
        } else {
            if(mTesting != null) {
                removeCallback(mTesting);
                mTesting.stop();
                mTesting = null;
            }
        }
    }

    static public boolean isValid(Format format) {
        if(format.CodecName.equals(Supporting.ENC_NAME_AAC)) {
            if(!Supporting.ENC_AAC_PROFILEs.contains(format.Profile)) {
                LogManager.e(String.format("Invalid profile: %d", format.Profile));
                return false;
            }
            if(format.Bitrate < 0) {
                LogManager.e(String.format("Invalid bitrate: %d", format.Bitrate));
                return false;
            }
        } else if(format.CodecName.equals(Supporting.ENC_NAME_G711A) || format.CodecName.equals(Supporting.ENC_NAME_G711U)) {
            if(format.Bitrate >= 0 && format.Bitrate != Supporting.ENC_G711_BITRATE) {
                LogManager.e(String.format("Invalid bitrate: %d(MUST be %d)", format.Bitrate, Supporting.ENC_G711_BITRATE));
                return false;
            }
        } else {
            LogManager.e("Non-supported encode name: " + format.CodecName);
            return false;
        }
        return true;
    }

    static public boolean isValid(String codeName, AudioCapturer.Format format) {
        if(codeName.equals(Supporting.ENC_NAME_AAC)) {
            if(format.SampleRate > Supporting.ENC_AAC_SAMPLERATE_MAX || format.SampleRate < Supporting.ENC_AAC_SAMPLERATE_MIN) {
                LogManager.e(String.format("Invalid sampleRate: %d", format.SampleRate));
                return false;
            }
            if(format.ChannelsCnt <= 0) {
                LogManager.e(String.format("Invalid channelsCnt: %d", format.ChannelsCnt));
                return false;
            }
        } else if(codeName.equals(Supporting.ENC_NAME_G711A) || codeName.equals(Supporting.ENC_NAME_G711U)) {
            if(format.SampleRate >= 0 && format.SampleRate != Supporting.ENC_G711_SAMPLERATE) {
                LogManager.e(String.format("Invalid samplerate: %d(MUST be %d)", format.SampleRate, Supporting.ENC_G711_SAMPLERATE));
                return false;
            }
            if(format.ChannelsCnt >= 0 && format.ChannelsCnt != Supporting.ENC_G711_CHANNEL_CNT) {
                LogManager.e(String.format("Invalid channelsCnt: %d(MUST be %d)", format.ChannelsCnt, Supporting.ENC_G711_CHANNEL_CNT));
                return false;
            }
            if(format.SampleWidthInBytes >= 0 && format.SampleWidthInBytes != Supporting.ENC_G711_SAMPLE_WIDTH_IN_BYTES) {
                LogManager.e(String.format("Invalid pcm sample width: %d bytes(MUST be %d)", format.SampleWidthInBytes, Supporting.ENC_G711_SAMPLE_WIDTH_IN_BYTES));
                return false;
            }
        } else {
            LogManager.e("Non-supported encode name: " + codeName);
            return false;
        }
        return true;
    }

    interface CommonEncoder {
        int init(MediaFormat format);
        int reinit(MediaFormat format);
        void release();
        void start();
        void stop();
        void encode(ByteBuffer pcmFrame, long ptsUs);
        ByteBuffer getEncodedFrame(int timeoutMs);
        void releaseEncodedFrame();
        MediaCodec.BufferInfo getEncodedInfo();
    };

    class AndroidEncoder implements CommonEncoder {
        private MediaCodec mMediaCodec = null;
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        private int mIndex = -1;

        @Override
        public int init(MediaFormat format) {
            if(mMediaCodec == null) {
                String mime = format.getString(MediaFormat.KEY_MIME);
                try {
                    mMediaCodec = MediaCodec.createEncoderByType(mime);
                } catch (IOException e) {
                    LogManager.e(String.format("Create encode(%s) failed:", mime));
                    e.printStackTrace();
                    return -1;
                } catch (IllegalArgumentException e) {
                    LogManager.e(String.format("Create encode(%s) failed:", mime));
                    e.printStackTrace();
                    return -1;
                }
            }
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return 0;
        }

        @Override
        public int reinit(MediaFormat format) {
            return init(format);
        }

        @Override
        public void release() {
            if(mMediaCodec != null) {
                mMediaCodec.release();
                mMediaCodec = null;
            }
        }

        @Override
        public void start() {
            if(mMediaCodec != null)
                mMediaCodec.start();
        }

        @Override
        public void stop() {
            if(mMediaCodec != null)
                mMediaCodec.stop();
        }

        @Override
        public void encode(ByteBuffer pcmFrame, long ptsUs) {
            if(mMediaCodec == null)
                return;
            int index = mMediaCodec.dequeueInputBuffer(DefaultEncodeTimeoutMs*1000);
            if(index < 0) {
                LogManager.e("get encoder input buffer failed, drop this frame");
                return;
            }
            int dataLen = pcmFrame.remaining();
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(pcmFrame);
            inputBuffer.flip();
            inputBuffer.order(pcmFrame.order());
            //LogManager.d(String.format("Push into encoder %d bytes", inputBuffer.limit()-inputBuffer.position()));
            mMediaCodec.queueInputBuffer(index, 0, dataLen, ptsUs, 0);
        }

        @Override
        public ByteBuffer getEncodedFrame(int timeoutMs) {
            mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, timeoutMs*1000);
            if(mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //do nothing
                LogManager.d("wait encoded data");
                return null;
            } else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                LogManager.w("audio encoder output format changed: " + newFormat);
                return null;
            } else if (mIndex < 0) {
                LogManager.w("unexpected result from encoder.dequeueOutputBuffer: " + mIndex);
                // let's ignore it
                return null;
            }

            ByteBuffer encodedData = mMediaCodec.getOutputBuffer(mIndex);
            if (encodedData == null) {
                throw new RuntimeException("encoderOutputBuffer " + mIndex + " was null");
            }
            if (mBufferInfo.size == 0) {
                LogManager.d("logical error: audio encoded frame is none");
                releaseEncodedFrame();
                return null;
            }

            // adjust the ByteBuffer values to match BufferInfo (not needed?)
            encodedData.position(mBufferInfo.offset);
            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
            return encodedData;
        }

        @Override
        public void releaseEncodedFrame() {
            mMediaCodec.releaseOutputBuffer(mIndex, false);
            mIndex = -1;
        }

        @Override
        public MediaCodec.BufferInfo getEncodedInfo() {
            return mBufferInfo;
        }
    }

    class G711Encoder implements CommonEncoder {
        static final int DEFAULT_RAW_FRAME_SIZE_IN_BYTES = 320;
        static final int BUFFERED_FRAME_COUNT = 3;
        static final int G711_TYPE_PCMA = 0;
        static final int G711_TYPE_PCMU = 1;
        static private final int TIMEOUT_MS = AudioCapturer.Supporting.FRAME_DURATION_MS_RECOMMENDED;

        private int mType = -1;
        private int mRawSizeInBytes = -1;
        private ByteBuffer mEncodedFrame = null;
        private MediaCodec.BufferInfo mEncodedInfo = null;
        private AVChannel mChannel = null;
        private AVPacket mPendingFrame = null;

        @Override
        public int init(MediaFormat format) {
            if(mChannel != null) {
                LogManager.w("had inited");
                return 0;
            }
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.equals(MediaFormat.MIMETYPE_AUDIO_G711_ALAW)) {
                mType = G711_TYPE_PCMA;
            } else if(mime.equals(MediaFormat.MIMETYPE_AUDIO_G711_MLAW)) {
                mType = G711_TYPE_PCMU;
            } else {
                LogManager.e("invalid G711Encoder mine: " + mime);
                return -1;
            }
            if(format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                mRawSizeInBytes = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            else
                mRawSizeInBytes = DEFAULT_RAW_FRAME_SIZE_IN_BYTES;
            mEncodedFrame = ByteBuffer.allocateDirect(mRawSizeInBytes/2);
            mEncodedFrame.clear();
            mEncodedInfo = new MediaCodec.BufferInfo();
            mChannel = new AVChannel(BUFFERED_FRAME_COUNT, DEFAULT_RAW_FRAME_SIZE_IN_BYTES);
            return 0;
        }

        @Override
        public int reinit(MediaFormat format) {
            release();
            return init(format);
        }

        @Override
        public void release() {
            if(mChannel != null){
                mType = -1;
                mRawSizeInBytes = -1;
                mEncodedFrame = null;
                mEncodedInfo = null;
                mChannel.clearBusyPackets();
                mChannel = null;
                mPendingFrame = null;
            }
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public void encode(ByteBuffer pcmFrame, long ptsUs) {
            while(pcmFrame.remaining() > 0) {
                if(mPendingFrame == null) {
                    try {
                        mPendingFrame = mChannel.pollIdlePacket(TIMEOUT_MS);
                        if(mPendingFrame == null) {
                            LogManager.w("G711Encoder poll idle packet failed, drop this pcm frame");
                            return;
                        }
                        mPendingFrame.getPayload().clear();
                        mPendingFrame.getPayload().order(pcmFrame.order());
                        mPendingFrame.setPts(ptsUs);
                        mPendingFrame.setDts(ptsUs);
                        mPendingFrame.setMediaType(AVDefines.DataType.AUDIO);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        LogManager.w("G711Encoder poll idle packet on channel was interrupted!");
                        return;
                    }
                }

                ByteBuffer payload = mPendingFrame.getPayload();
                int leftSpace = mRawSizeInBytes - payload.position();
                if(leftSpace < 0) {
                    throw new RuntimeException("logical error");
                } else if(leftSpace > 0) {
                    if(pcmFrame.remaining() >= leftSpace) {
                        payload.put(pcmFrame.array(), pcmFrame.position(), leftSpace);
                        pcmFrame.position(pcmFrame.position() + leftSpace);
                        payload.flip();
                        mChannel.putBusyPacket(mPendingFrame);
                        mPendingFrame = null;
                        continue;
                    } else {
                        payload.put(pcmFrame);
                        // return;
                    }
                }
            }
        }

        @Override
        public ByteBuffer getEncodedFrame(int timeoutMs) {
            AVPacket pcmFrame = null;
            try {
                pcmFrame = mChannel.pollBusyPacket(timeoutMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
                LogManager.w("G711Encoder poll busy packet on channel was interrupted!");
                return null;
            }

            if(pcmFrame == null) {
                LogManager.w("G711Encoder poll busy packet failed");
                return null;
            }

            ByteBuffer pcm = pcmFrame.getPayload();
            if(pcm.remaining() != mRawSizeInBytes) {
                mChannel.putIdlePacket(pcmFrame);
                throw new RuntimeException("logical error: PCM data size is invalid (need " + mRawSizeInBytes + "bytes, got " + pcm.remaining() + "bytes)");
            }

            mEncodedFrame.clear();
            if(mType == G711_TYPE_PCMA) {
                for(int i = 0 ; i < mRawSizeInBytes ; i += 2) {
                    int sample = pcm.getShort();
                    mEncodedFrame.put((byte) G711.linear2alaw(sample));
                }
            } else if(mType == G711_TYPE_PCMU) {
                for(int i = 0 ; i < mRawSizeInBytes ; i += 2) {
                    int sample = pcm.getShort();
                    mEncodedFrame.put((byte) G711.linear2ulaw(sample));
                }
            }
            mEncodedFrame.flip();

            mEncodedInfo.set(mEncodedFrame.position(), mEncodedFrame.limit(), pcmFrame.getPts(), 0);
            pcmFrame.reset();
            mChannel.putIdlePacket(pcmFrame);
            return mEncodedFrame;
        }

        @Override
        public void releaseEncodedFrame() {

        }

        @Override
        public MediaCodec.BufferInfo getEncodedInfo() {
            return mEncodedInfo;
        }
    }

    private abstract class Testing implements Callback {
        abstract void start();
        abstract void stop();
    }

    private class Debug2FileTesting extends Testing {
        private String mFilePath = null;
        private FileOutputStream mOutFile = null;
        int mProfile = -1;
        int mChannelCnt = -1;
        int mSampleRateIndex = -1;
        boolean mIsAAC = false;
        private byte mG711Data[] = null;

        public Debug2FileTesting(String path) {
            mIsAAC = false;
            mFilePath = path;
        }

        public Debug2FileTesting(String path, int samplerate, int channelCnt, int profile) {
            mIsAAC = true;
            mFilePath = path;
            mSampleRateIndex = Samplerate2Index(samplerate);
            mChannelCnt = channelCnt;
            mProfile = profile;
        }

        public void start() {
            if(mOutFile != null)
                return;
            try {
                mOutFile = new FileOutputStream(mFilePath);
                LogManager.i("Start to Debug2FileTesting");
            } catch (FileNotFoundException e) {
                LogManager.e("Create FileOutputStream failed");
               LogManager.e(e);
                mOutFile = null;
            }
        }

        public void stop() {
            if(mOutFile != null) {
                try { mOutFile.close(); } catch (IOException e) {LogManager.e(e); }
                mOutFile = null;
                LogManager.i("End of Debug2FileTesting");
            }
        }

        public void onData(ByteBuffer data, long ptsUs, int flags) {
            if(mOutFile == null)
                return;
            if(mIsAAC)
                onAACRawData(data, ptsUs, flags);
            else
                onG711RawData(data, ptsUs, flags);
        }

        private void onG711RawData(ByteBuffer data, long ptsUs, int flags) {
            if(mG711Data == null)
                mG711Data = new byte[data.limit() - data.position()];
            data.get(mG711Data);
            //LogManager.d(String.format("Write to g711 file %d bytes", mG711Data.length));
            try { mOutFile.write(mG711Data, 0, mG711Data.length); } catch (IOException e) {LogManager.e(e); }
        }

        private void onAACRawData(ByteBuffer data, long ptsUs, int flags) {
            int rawLen = data.limit() - data.position();
            int pktLen = rawLen + 7;
            byte bData[] = new byte[pktLen];
            addADTStoPacket(bData, pktLen);
            data.get(bData, 7, rawLen);
            //LogManager.d(String.format("Write to file %d bytes", pktLen));
            try { mOutFile.write(bData, 0, pktLen); } catch (IOException e) {LogManager.e(e); }
        }
        /**
         *  Add ADTS header at the beginning of each and every AAC packet.
         *  This is needed as MediaCodec encoder generates a packet of raw
         *  AAC data.
         *  Note: the packetLen must count in the ADTS header itself.
         *  Refer to https://wiki.multimedia.cx/index.php?title=ADTS
         **/
        private void addADTStoPacket(byte[] packet, int packetLen) {
            packet[0] = (byte)0xFF;
            packet[1] = (byte)0xF9;
            packet[2] = (byte)(((mProfile-1)<<6) + (mSampleRateIndex<<2) +(mChannelCnt>>2));
            packet[3] = (byte)(((mChannelCnt&3)<<6) + (packetLen>>11));
            packet[4] = (byte)((packetLen&0x7FF) >> 3);
            packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
            packet[6] = (byte)0xFC;
        }

        private final Map<Integer, Integer> mIndexs = new HashMap<Integer, Integer>() {{
            put(96000, 0); put(88200, 1);
            put(64000, 2); put(48000, 3);
            put(44100, 4); put(32000, 5);
            put(24000, 6); put(22050, 7);
            put(16000, 8); put(12000, 9);
            put(11025, 10); put(8000 , 11);
            put(7350 , 12);
        }};
        private int Samplerate2Index(int samplerate) {
            if(mIndexs.containsKey(samplerate))
                return mIndexs.get(samplerate);
            return -1;
        }
    }

    static public class Tester implements SimpleTesting.Tester {

        private int mTestingStep = 5;
        private AudioCapturer mAudioCapturer = null;
        private AudioEncoder mAudioEncoder = null;
        public void start(Object obj) {
            mAudioCapturer = AudioCapturer.getInstance();
            if (mAudioCapturer.init(
                    AudioCapturer.Supporting.DEFAULT_AUDIO_SOURCE,
                    AudioCapturer.Supporting.HZ_44100,              // HZ_44100/HZ_8000
                    AudioCapturer.Supporting.CHANNEL_STEREO,        // CHANNEL_STEREO/CHANNEL_MONO
                    AudioCapturer.Supporting.DEFAULT_SAMPLE_FORMAT,
                    AudioCapturer.Supporting.FRAME_DURATION_MS_RECOMMENDED
            ) != 0)
                return;
            mAudioEncoder = new AudioEncoder();
            if (mAudioEncoder.init(mAudioCapturer.PCMFormat,
                    new AudioEncoder.Format(
                            ///*
                            AudioEncoder.Supporting.ENC_NAME_AAC,
                            64000,
                            AudioEncoder.Supporting.ENC_AAC_PROFILE_LC
                            //*/
                            /*
                            Supporting.ENC_NAME_G711U, -1, -1
                            //*/
                            )) != 0)
                return;
            mAudioEncoder.enableTesting(true);
            mAudioCapturer.addCallback(mAudioEncoder);
            mAudioCapturer.enableTesting(true, AudioCapturer.Supporting.TESTING_METHOD_TOFILE, AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_MEDIA);
            --mTestingStep;
        }
        public void next() {
            if(mAudioCapturer == null || mAudioEncoder == null)
                return;
            if (mTestingStep == 0) {
                mAudioCapturer.removeCallback(mAudioEncoder);
                mAudioEncoder.close();
                mAudioEncoder = null;
                AudioCapturer.releaseInstance();
                mAudioCapturer = null;
                return;
            }
            if (mTestingStep % 2 == 0) {
                mAudioEncoder.start();
                mAudioCapturer.resume();
                mAudioCapturer.enableTesting(true, AudioCapturer.Supporting.TESTING_METHOD_TOFILE, AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_MEDIA);
            } else {
                mAudioCapturer.enableTesting(false, AudioCapturer.Supporting.TESTING_METHOD_NONE, AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_NONE);
                mAudioCapturer.pause();
                mAudioEncoder.reset();
            }
            --mTestingStep;
        }
    }
}
