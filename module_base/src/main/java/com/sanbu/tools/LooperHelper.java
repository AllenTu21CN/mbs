package com.sanbu.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Tuyj on 2018/3/19.
 */

public class LooperHelper {

    private static final String TAG = LooperHelper.class.getSimpleName();

    private static final int STATE_NONE = 0;
    private static final int STATE_PREPARED = 1;
    private static final int STATE_PAUSING = 2;
    private static final int STATE_PAUSED = 3;
    private static final int STATE_STARTING = 4;
    private static final int STATE_READY = 5;
    private static final int STATE_OVER = 6;

    private String mThreadName;
    private Thread mWorkingThread = null;
    private WorkHandler mWorkHandler = null;

    private Object mStateLock = new Object();
    private int mState = STATE_NONE;

    public LooperHelper(String name) {
        mThreadName = name;
    }

    public boolean startLoopInNewThreadUntilReady() {
        if (mWorkingThread != null || mWorkHandler != null || mState != STATE_NONE) {
            LogUtil.e(TAG, "logical error: had created thread or handler1");
            return false;
        }

        mWorkingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mWorkHandler = new WorkHandler();

                LooperHelper.this.notifyState(STATE_READY);
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

                LooperHelper.this.notifyState(STATE_OVER);
            }
        }, mThreadName);

        mWorkingThread.setDaemon(true);
        mWorkingThread.start();
        waitState(STATE_READY);

        return true;
    }

    public boolean prepareLoopThread() {
        if (mWorkingThread != null || mWorkHandler != null || mState != STATE_NONE) {
            LogUtil.e(TAG, "logical error: had created thread or handler2");
            return false;
        }

        mWorkingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mWorkHandler = new WorkHandler();
                LooperHelper.this.notifyState(STATE_PREPARED);
                LooperHelper.this.waitState(STATE_STARTING);

                LooperHelper.this.notifyState(STATE_READY);
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

                LooperHelper.this.notifyState(STATE_OVER);
            }
        }, mThreadName);

        mWorkingThread.setDaemon(true);
        mWorkingThread.start();
        waitState(STATE_PREPARED);

        return true;
    }

    public boolean startPreparedThreadUntilReady() {
        if (mWorkHandler == null || mWorkingThread == null || mState < STATE_PREPARED) {
            LogUtil.e(TAG, "logical error: had created thread or handler3");
            return false;
        }

        notifyState(STATE_STARTING);
        waitState(STATE_READY);
        return true;
    }

    public void stopTheLoopThreadUntilOver(boolean forced) {
        stopTheLoopThreadUntilOver(forced, -1);
    }

    public void stopTheLoopThreadUntilOver(boolean forced, long timeoutMs) {
        if (mWorkHandler != null && mWorkingThread != null) {
            mWorkHandler.sendEmptyMessage(WorkHandler.STOP_LOOPER);

            if (forced)
                mWorkingThread.interrupt();
            waitState(STATE_OVER, timeoutMs);
            mWorkHandler = null;
            mWorkingThread = null;
        }
    }

    public boolean attachLoopInCurrentThread() {
        if (mWorkingThread != null || mWorkHandler != null || mState != STATE_NONE) {
            LogUtil.e(TAG, "logical error: had created thread or handler4");
            return false;
        }
        if (mWorkHandler == null)
            mWorkHandler = new WorkHandler();
        return true;
    }

    public boolean pauseLooper() {
        if (mWorkHandler == null || mState < STATE_STARTING) {
            LogUtil.e(TAG, "logical error: had created thread or handler5");
            return false;
        }

        LogUtil.i(TAG, "pause looper");
        synchronized (mStateLock) {
            mState = STATE_PAUSING;
        }
        mWorkHandler.sendEmptyMessage(WorkHandler.PAUSE_LOOPER);
        waitState(STATE_PAUSED);
        return true;
    }

    public boolean unPauseLooper() {
        if (mWorkHandler == null || mState < STATE_PREPARED) {
            LogUtil.e(TAG, "logical error: had created thread or handler5");
            return false;
        }

        notifyState(STATE_STARTING);
        waitState(STATE_READY);
        LogUtil.i(TAG, "unPaused looper");
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

    private boolean waitState(int state) {
        return waitState(state, -1);
    }

    private boolean waitState(int state, long timeoutMs) {
        long timeoutPointMs = (timeoutMs > 0) ?
                (System.currentTimeMillis() + timeoutMs) : -1;

        synchronized (mStateLock) {
            while (mState < state) {
                try {
                    if (timeoutPointMs > 0) {
                        timeoutMs = timeoutPointMs - System.currentTimeMillis();
                        if (timeoutMs > 0)
                            mStateLock.wait(timeoutMs);
                        else
                            return false;
                    } else {
                        mStateLock.wait();
                    }
                } catch (InterruptedException ie) {
                    LogUtil.w(TAG, "waitState interrupted", ie);
                    return false;
                }
            }

            // reset state
            if (mState == STATE_OVER)
                mState = STATE_NONE;
        }

        return true;
    }

    private void notifyState(int state) {
        synchronized (mStateLock) {
            if (mState < state)
                mState = state;
            mStateLock.notify();
        }
    }

    private static boolean waitPendingCallback(Handler h, long timeoutMS) {
        final AsyncHelper result = new AsyncHelper();
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
        static final int PAUSE_LOOPER = -2;

        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == STOP_LOOPER) {
                    Looper.myLooper().quit();
                } else if (msg.what == PAUSE_LOOPER) {
                    LooperHelper.this.notifyState(STATE_PAUSED);
                    LooperHelper.this.waitState(STATE_STARTING);
                    LooperHelper.this.notifyState(STATE_READY);
                } else {
                    LogUtil.w(TAG, "get unknown message: " + msg.what);
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "error2: ", e);
            }
        }
    }
}
