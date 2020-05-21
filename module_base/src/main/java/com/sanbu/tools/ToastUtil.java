package com.sanbu.tools;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastUtil {

    private static final long SHORT_MIN_SHOWN_INTERVAL_MS = 500;
    private static final long LONG_MIN_SHOWN_INTERVAL_MS = 1000;

    private static Context mContext = null;
    private static ToastUtil mImpl = null;

    public static void init(Context context) {
        mContext = context;
        mImpl = new ToastUtil();
    }

    public static void show(String message) {
        show(message, false);
    }

    public static void show(String message, boolean keepLong) {
        mImpl.showImpl(message, keepLong);
    }

    private long mLastTime = 0;
    private String mLastMsg = null;

    private Toast mLastToast = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private ToastUtil() {

    }

    public void showImpl(final String message, boolean keepLong) {
        final int duration = keepLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        long minIntervalMs =  keepLong ? LONG_MIN_SHOWN_INTERVAL_MS : SHORT_MIN_SHOWN_INTERVAL_MS;

        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            if (mLastMsg != null && mLastMsg.equals(message) && currentTime - mLastTime < minIntervalMs)
                return;
            mLastTime = currentTime;
            mLastMsg = message;
        }

        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mLastToast != null)
                    mLastToast.cancel();

                mLastToast = Toast.makeText(mContext, message, duration);
                mLastToast.show();
            }
        });
    }
}

