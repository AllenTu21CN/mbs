package cn.sanbu.avalon.media;

import android.os.Build;
import android.util.Log;

import com.sanbu.board.BoardSupportClient;
import com.sanbu.board.HDMIFormat;

public class HdmiInputDevice {

    private static final String TAG = "avalon_" + HdmiInputDevice.class.getSimpleName();

    private static BoardSupportClient mBoardSupportClient = null;

    private static HdmiInputDevice mInstance = null;

    public static void init(BoardSupportClient client) {
        synchronized (HdmiInputDevice.class) {
            if (mBoardSupportClient == null)
                mBoardSupportClient = client;
        }
    }

    public static HdmiInputDevice getInstance() {
        if (mBoardSupportClient == null)
            throw new RuntimeException("call HdmiInputDevice.init first");

        if (mInstance == null) {
            synchronized (HdmiInputDevice.class) {
                if (mInstance ==null) {
                    mInstance = new HdmiInputDevice();
                }
            }
        }

        return mInstance;
    }

    private boolean mSupportedPlatform = false;
    private volatile boolean[] mCachedIsPlugged = new boolean[2];
    private volatile HDMIFormat[] mCachedFormat = new HDMIFormat[2];
    private Thread mQueryThread;

    private HdmiInputDevice() {
        //Log.d(TAG, "Initialize HDMI input device object.");

        if (false) {
            // Detect board type
            if (Build.MODEL.equals("SOM-9X20_VT6105")) {
                mSupportedPlatform = true;
            } else {
                // TODO: RK3399
                Log.e(TAG, "NOT IMPLEMENTED ON CURRENT HARDWARE PLATFORM.");
            }
        } else {
            if (mBoardSupportClient != null && mBoardSupportClient.isSupportHdmiChecking()) {
                mSupportedPlatform = true;
            } else {
                Log.e(TAG, "NOT IMPLEMENTED ON CURRENT HARDWARE PLATFORM.2");
            }
        }
    }

    private synchronized void startQueryUntilPlugged(int port) {
        if (mQueryThread != null && mQueryThread.isAlive()) {
            //Log.w(TAG, "Query thread already exists!");
            return;
        }

        Runnable runnable = () -> {
            Log.d(TAG, "Start HDMI-IN device query thread.");
            while (true) {
                boolean isPlugged = mBoardSupportClient.invokeVideoInputPortIsPlugged(port);
                Log.d(TAG, "HDMI-IN #" + (port + 1) + (isPlugged ? " plugged" : " unplugged"));
                mCachedIsPlugged[port] = isPlugged;

                if (isPlugged) {
                    HDMIFormat fmt = mBoardSupportClient.invokeVideoInputPortQueryFormat(port);
                    Log.d(TAG, "HDMI-IN #" + (port + 1) + " format=" + fmt.toString());
                    mCachedFormat[port] = fmt;

                    return;
                }

                try {
                    Thread.sleep(1000 * 3);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        };

        mQueryThread = new Thread(runnable);
        mQueryThread.start();
    }

    public void clearCache(int port) {
        mCachedIsPlugged[port] = false;
        mCachedFormat[port] = null;
    }

    public boolean isPlugged(int port) {
        if (!mSupportedPlatform) {
            return true;
        }

        if (!mCachedIsPlugged[port]) {
            startQueryUntilPlugged(port);
        }

        return mCachedIsPlugged[port];
    }

    public HDMIFormat queryFormat(int port) {
        if (!mSupportedPlatform) {
            return new HDMIFormat(1920, 1080, 30);
        }

        return mCachedFormat[port];
    }

    public int cameraIdToHdmiDeviceId(int cameraId) {
        return mBoardSupportClient.cameraIdToHdmiDeviceId(cameraId);
    }
}
