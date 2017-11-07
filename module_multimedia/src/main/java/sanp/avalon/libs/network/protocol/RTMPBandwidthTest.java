package sanp.avalon.libs.network.protocol;

import android.content.Context;

import sanp.avalon.libs.SimpleTesting;
import sanp.avalon.libs.base.utils.FileSaveUtils;
import sanp.avalon.libs.base.utils.LogManager;

import sanp.multimedia.R;

public class RTMPBandwidthTest {
    static{
        System.loadLibrary("MPX_JNI");
    }

    public interface Callback {
        void onStatis(long momentBitrate, long avgBitrate);
    }

    static private RTMPBandwidthTest mRTMPBandwidthTest = null;
    static private final String resPath = System.getenv("EXTERNAL_STORAGE") +"/MPX";
    static private final String h264StreamFile = "speed0_h264";
    static private final String h264IndexFile  = h264StreamFile + "_idx";
    static private final String h264SPSFile    = h264StreamFile + "_sps";
    static private final int minFrameInvaMs = 1;
    static private final int statisPeriodMs = 1000;

    static public RTMPBandwidthTest createInstance(Context context) {
        if(mRTMPBandwidthTest == null) {
            synchronized (RTMPBandwidthTest.class) {
                if(mRTMPBandwidthTest == null) {
                    mRTMPBandwidthTest = new RTMPBandwidthTest(context);
                }
            }
        }
        return mRTMPBandwidthTest;
    }

    static public RTMPBandwidthTest getInstance() {
        return mRTMPBandwidthTest;
    }

    private int mCallbackIntervalMs = -1;
    private boolean mRunning = false;
    private Callback mCallback = null;
    private Thread mCallbackThread = null;
    private RTMPBandwidthTest(Context context) {
        try {
            FileSaveUtils.saveToSDCard(context, resPath, h264StreamFile, R.raw.speed0_h264);
            FileSaveUtils.saveToSDCard(context, resPath, h264IndexFile, R.raw.speed0_h264_idx);
            FileSaveUtils.saveToSDCard(context, resPath, h264SPSFile, R.raw.speed0_h264_sps);
        } catch (Throwable throwable) {
            throw new RuntimeException("RTMPBandwidthTest copy resource to sdcard failed");
        }

        String streamFile = String.format("%s/%s", resPath, h264StreamFile);
        if(jniInit(streamFile, minFrameInvaMs, statisPeriodMs) != 0)
            throw new RuntimeException("RTMPBandwidthTest init failed");
    }

    public void close() {
        stop();
        jniClose();
    }

    public void start(String rtmpServer) {
        start(rtmpServer, -1, null);
    }

    public void start(String rtmpServer, int callbackIntervalMs, Callback cb) {
        if(jniStart(rtmpServer) != 0)
            throw new RuntimeException("RTMPBandwidthTest start failed");

        if(callbackIntervalMs > 0 && cb != null) {
            if(mRunning || mCallbackThread != null)
                return;

            mCallbackIntervalMs = callbackIntervalMs;
            mCallback = cb;
            mCallbackThread = new Thread(new Runnable() {
                public void run() { callbackThreadLoop(); }
            }, "RTMPBandwidthTest callback thread");
            mCallbackThread.start();
        }
    }

    public void stop() {
        mRunning = false;
        if(mCallbackThread != null) {
            try {
                mCallbackThread.join(mCallbackIntervalMs*3);
                //mOutputThread.interrupt();
            } catch (InterruptedException e) {
               LogManager.e(e);
            }
            mCallbackThread = null;
        }
        jniStop();
    }

    private void callbackThreadLoop() {
        mRunning = true;
        LogManager.i("Start to RTMPBandwidthTest callback loop");
        while (mRunning) {
            try { Thread.sleep(mCallbackIntervalMs); } catch (InterruptedException e) {LogManager.e(e); break;}
            long[] statis = jniGetStatistics();
            mCallback.onStatis(statis[0], statis[1]);
        }
        mRunning = false;
        LogManager.i("Exit to RTMPBandwidthTest callback loop");
    }

    private native int jniInit(String streamFile, int minInvaMs, int statisPeriodMs);
    private native int jniClose();
    private native int jniStart(String rtmpServer);
    private native int jniStop();
    private native long[] jniGetStatistics();

    static public class Tester implements SimpleTesting.Tester, Callback {

        private int mTestingStep = 4;
        private RTMPBandwidthTest mRTMPBandwidthTest = null;
        private String mRTMPServer = "rtmp://10.1.36.5:11935/live/688d5ca7-11df-4c61-a51f-50063af82f7c?s=tuyj";
        public void start(Object obj) {
            mRTMPBandwidthTest = RTMPBandwidthTest.createInstance((Context)obj);
            mRTMPBandwidthTest.start(mRTMPServer, 1000, this);
            --mTestingStep;
        }
        public void next() {
            if (mTestingStep == 0) {
                if (mRTMPBandwidthTest != null) {
                    mRTMPBandwidthTest.close();
                    mRTMPBandwidthTest = null;
                }
                return;
            }
            if (mTestingStep % 2 == 0) {
                mRTMPBandwidthTest.start(mRTMPServer, 1000, this);
            } else {
                mRTMPBandwidthTest.stop();
            }
            --mTestingStep;
        }
        public void onStatis(long momentBitrate, long avgBitrate) {
            LogManager.i(String.format("moment bitrate:%d kbps   avg bitrate:%d kbps", momentBitrate / 1000, avgBitrate / 1000));
        }
    }
}
