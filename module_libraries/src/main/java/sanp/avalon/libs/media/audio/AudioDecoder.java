package sanp.avalon.libs.media.audio;

import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVPacket;

/**
 * Created by Tuyj on 2017/10/19.
 */

public class AudioDecoder {
    public interface Callback {
        void onData(ByteBuffer data, long ptsUs);
    }

    static private final int THREAD_EXIT_TIMEOUT_MS = 50;
    static private final int PKT_WAIT_TIMEOUT_MS = 30;

    MediaFormat mMediaFormat = null;
    private AVChannel mAVChannel = null;
    private CommonDecoder mDecoder = null;

    private boolean mRunning = false;
    private Thread mOutputThread = null;
    private List<Callback> mCallbacks = new ArrayList<>();

    public AudioDecoder() {
    }

    public int init(MediaFormat format, AVChannel channel) {
        if(mDecoder != null) {
            LogManager.w("had inited");
            return 0;
        }

        String mime = format.getString(MediaFormat.KEY_MIME);
        if(mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
            LogManager.e("TODO: not implement aac deocoder");
            return -1;
        } else if(mime == MediaFormat.MIMETYPE_AUDIO_G711_MLAW || mime == MediaFormat.MIMETYPE_AUDIO_G711_ALAW) {
            mDecoder = new G711Decoder();
        } else {
            LogManager.e("non-support audio decoder mime " + mime);
            return -1;
        }

        mMediaFormat = format;
        mAVChannel = channel;
        return mDecoder.init(format);
    }

    public int close() {
        stop();
        if(mDecoder != null) {
            mDecoder.release();
            mDecoder = null;
        }
        mCallbacks.clear();
        mAVChannel = null;
        return 0;
    }

    public int start() {
        if(mDecoder == null) {
            LogManager.e("init first");
            return -1;
        }
        if(mRunning && mOutputThread != null)
            return 0;

        mOutputThread = new Thread(new Runnable() {
            public void run() { outputThreadLoop(); }
        }, "AudioDecoder OutputThread");
        mRunning = true;
        mDecoder.start();
        mOutputThread.start();
        return 0;
    }

    public int stop() {
        mRunning = false;
        if(mOutputThread != null) {
            AVPacket pkt = mAVChannel.pollIdlePacket();
            if(pkt != null) {
                pkt.reset();
                mAVChannel.offerBusyPacket(pkt);
            }
            try {  mOutputThread.join(THREAD_EXIT_TIMEOUT_MS); } catch (InterruptedException e) {LogManager.e(e); }
            mOutputThread = null;
        }
        return 0;
    }

    public void reset() {
        stop();
        if(mDecoder != null)
            mDecoder.reinit(mMediaFormat);
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

    class BreakoutThread extends Exception {}
    private void checkState() throws BreakoutThread {
        if(!mRunning) throw new BreakoutThread();
    }
    private void resetPkt(AVPacket pkt) {
        pkt.reset();
        mAVChannel.putIdlePacket(pkt);
    }

    private void outputThreadLoop() {
        LogManager.i("AudioDecoder thread started!");

        AVPacket pkt = null;
        try {
            while(true) {
                checkState();

                pkt = mAVChannel.pollBusyPacket(PKT_WAIT_TIMEOUT_MS);
                if(pkt == null)
                    continue;
                checkState();

                long pts = pkt.getPts();
                ByteBuffer payload = pkt.getPayload();
                ByteBuffer pcmFrame = mDecoder.decode(payload);
                resetPkt(pkt);
                payload = null;

                //LogManager.d(String.format("Decode output %d bytes with pts %d", pcmFrame.remaining(), pts));
                int pos = pcmFrame.position();
                synchronized(mCallbacks) {
                    for(Callback cb: mCallbacks) {
                        cb.onData(pcmFrame, pts);
                        pcmFrame.position(pos);
                    }
                }
            }
        } catch (BreakoutThread | InterruptedException e) {
            if(pkt != null) {
                resetPkt(pkt);
            }
        }
        LogManager.i("AudioDecoder thread exit...!");
    }


    interface CommonDecoder {
        int init(MediaFormat format);
        int reinit(MediaFormat format);
        void release();
        void start();
        void stop();
        ByteBuffer decode(ByteBuffer pcmFrame);
    };

    class G711Decoder implements CommonDecoder {
        static final int G711_TYPE_PCMA = 0;
        static final int G711_TYPE_PCMU = 1;
        static final int DEFAULT_FRAME_MAX_DELAY_MS = 50;
        static final int DEFAULT_FRAME_MAX_SIZE_IN_BYTES = 8000 * 2 / 1000 * DEFAULT_FRAME_MAX_DELAY_MS;

        private int mType = -1;
        private ByteBuffer mDecodedFrame = null;

        @Override
        public int init(MediaFormat format) {
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime == MediaFormat.MIMETYPE_AUDIO_G711_ALAW) {
                mType = G711_TYPE_PCMA;
            } else if(mime == MediaFormat.MIMETYPE_AUDIO_G711_MLAW) {
                mType = G711_TYPE_PCMU;
            } else {
                LogManager.e("invalid G711Decoder mine: " + mime);
                return -1;
            }

            mDecodedFrame = ByteBuffer.allocateDirect(DEFAULT_FRAME_MAX_SIZE_IN_BYTES);
            mDecodedFrame.clear();
            mDecodedFrame.order(ByteOrder.LITTLE_ENDIAN);
            return 0;
        }

        @Override
        public int reinit(MediaFormat format) {
            release();
            return init(format);
        }

        @Override
        public void release() {
            mType = -1;
            mDecodedFrame.clear();
            mDecodedFrame = null;
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public ByteBuffer decode(ByteBuffer pcmFrame) {
            mDecodedFrame.clear();
            while(pcmFrame.remaining() > 0) {
                byte sample = pcmFrame.get();
                if(mType == G711_TYPE_PCMA) {
                    mDecodedFrame.putShort((short) G711.alaw2linear(sample));
                } else if(mType == G711_TYPE_PCMU) {
                    mDecodedFrame.putShort((short) G711.ulaw2linear(sample));
                }
            }
            mDecodedFrame.flip();
            return mDecodedFrame;
        }
    };
}
