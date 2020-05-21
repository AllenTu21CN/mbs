package com.sanbu.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Tuyj on 2018/3/19.
 */

public class LooperHelper {

    private static final String TAG = LooperHelper.class.getSimpleName();

    private String mThreadName;
    private Thread mWorkingThread = null;
    private WorkHandler mWorkHandler = null;

    private Object mRunningLock = new Object();
    private boolean mRunning = false;

    public LooperHelper(String name) {
        mThreadName = name;
    }

    public boolean startLoopInNewThreadUntilReady() {
        if (mWorkHandler == null) {

            mWorkingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    mWorkHandler = new WorkHandler();
                    LooperHelper.this.notifyReady();

                    while (true) {
                        try {
                            Looper.loop();
                            break;
                        } catch (Exception e) {
                            LogUtil.e(TAG, "Looper.loop error:");
                            e.printStackTrace();
                            LogUtil.w(TAG, "try Looper.loop again");
                        }
                    }

                    LooperHelper.this.notifyOver();
                }
            }, mThreadName);

            mWorkingThread.setDaemon(true);
            mWorkingThread.start();
            waitUntilReady();
        }
        return true;
    }

    public void stopTheLoopThreadUntilOver(boolean forced) {
        if (mWorkHandler != null && mWorkingThread != null) {
            mWorkHandler.sendEmptyMessage(WorkHandler.STOP_LOOPER);

            if (forced)
                mWorkingThread.interrupt();
            waitUntilOver();
            mWorkHandler = null;
            mWorkingThread = null;
        }
    }

    public boolean attachLoopInCurrentThread() {
        if (mWorkingThread != null)
            throw new RuntimeException("logical error: cannot call startLoopInNewThreadUntilReady and attachLoopInCurrentThread at the same time");
        if (mWorkHandler == null)
            mWorkHandler = new WorkHandler();
        return true;
    }

    public Handler getHandler() {
        return mWorkHandler;
    }

    public boolean removeCallbacks(Runnable r, long timeoutMS) {
        return removeCallbacks(mWorkHandler, r, timeoutMS);
    }

    public boolean removeCallbacks(Runnable r, Object token, long timeoutMS) {
        return removeCallbacks(mWorkHandler, r, token, timeoutMS);
    }

    public static boolean removeCallbacks(Handler h, Runnable r, long timeoutMS) {
        h.removeCallbacks(r);
        return waitPendingCallback(h, timeoutMS);
    }

    public static boolean removeCallbacks(Handler h, Runnable r, Object token, long timeoutMS) {
        h.removeCallbacks(r, token);
        return waitPendingCallback(h, timeoutMS);
    }

    private void waitUntilReady() {
        synchronized (mRunningLock) {
            while (!mRunning) {
                try {
                    mRunningLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void notifyReady() {
        synchronized (mRunningLock) {
            mRunning = true;
            mRunningLock.notify();
        }
    }

    private void waitUntilOver() {
        synchronized (mRunningLock) {
            while (mRunning) {
                try {
                    mRunningLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void notifyOver() {
        synchronized (mRunningLock) {
            mRunning = false;
            mRunningLock.notify();
        }
    }

    private static boolean waitPendingCallback(Handler h, long timeoutMS) {
        final AsyncResult result = new AsyncResult();
        h.post(new Runnable() {
            @Override
            public void run() {
                result.notify2(true);
            }
        });
        return (boolean) result.wait2(timeoutMS, false);
    }

    private class WorkHandler extends Handler {
        static final int STOP_LOOPER = -1;

        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == STOP_LOOPER) {
                    Looper.myLooper().quit();
                } else {
                    LogUtil.w(TAG, "get unknown message: " + msg.what);
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "error2: ", e);
            }
        }
    }
}
