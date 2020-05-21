package com.sanbu.discarded.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sanbu.tools.AsyncResult;
import com.sanbu.tools.LogUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tuyj on 2018/3/19.
 */

public class LooperWorker {

    private static final String TAG = LooperWorker.class.getSimpleName();

    private static final long DEFAULT_REMOVE_CALLBACK_TIMEOUT_MS = 500;

    public interface SubHandler {
        boolean handleMessage(Message msg);
    }

    private String mThreadName;
    private Thread mWorkingThread = null;
    private WorkHandler mWorkHandler = null;

    private Object mRunningLock = new Object();
    private boolean mRunning = false;

    private List<SubHandler> mSubHandlers = new LinkedList<>();

    public LooperWorker(String name) {
        mThreadName = name;
    }

    public boolean startLoopInNewThreadUntilReady() {
        if (mWorkHandler == null) {
            mWorkingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    mWorkHandler = new WorkHandler();
                    notifyReady();

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

                    notifyOver();
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

    public boolean post(Runnable r) {
        return mWorkHandler.post(r);
    }

    public boolean postAtTime(Runnable r, long uptimeMillis) {
        return mWorkHandler.postAtTime(r, uptimeMillis);
    }

    public boolean postAtTime(Runnable r, Object token, long uptimeMillis) {
        return mWorkHandler.postAtTime(r, token, uptimeMillis);
    }

    public boolean postDelayed(Runnable r, long delayMillis) {
        return mWorkHandler.postDelayed(r, delayMillis);
    }

    public void removeCallbacks(Runnable r) {
        mWorkHandler.removeCallbacks(r);
    }

    public void removeCallbacks(Runnable r, Object token) {
        mWorkHandler.removeCallbacks(r, token);
    }

    // safely, but may be blocked
    public boolean removeCallbacks2(Runnable r) {
        return removeCallbacks2(r, DEFAULT_REMOVE_CALLBACK_TIMEOUT_MS);
    }

    // safely, but may be blocked
    public boolean removeCallbacks2(Runnable r, Object token) {
        return removeCallbacks2(r, token, DEFAULT_REMOVE_CALLBACK_TIMEOUT_MS);
    }

    // safely, but may be blocked
    public boolean removeCallbacks2(Runnable r, long timeoutMS) {
        mWorkHandler.removeCallbacks(r);
        return waitPendingCallback(timeoutMS);
    }

    // safely, but may be blocked
    public boolean removeCallbacks2(Runnable r, Object token, long timeoutMS) {
        mWorkHandler.removeCallbacks(r, token);
        return waitPendingCallback(timeoutMS);
    }

    public void addSubHandler(SubHandler subHandler) {
        synchronized (mSubHandlers) {
            if (!mSubHandlers.contains(subHandler))
                mSubHandlers.add(subHandler);
        }
    }

    public void removeSubHandler(SubHandler subHandler) {
        synchronized (mSubHandlers) {
            mSubHandlers.remove(subHandler);
        }
    }

    public boolean sendMessageDelayed(int what, long delayMillis) {
        return mWorkHandler.sendMessageDelayed(mWorkHandler.obtainMessage(what), delayMillis);
    }

    public boolean sendMessageDelayed(int what, Object obj, long delayMillis) {
        return mWorkHandler.sendMessageDelayed(mWorkHandler.obtainMessage(what, obj), delayMillis);
    }

    public boolean sendMessageDelayed(int what, int arg1, int arg2, long delayMillis) {
        return mWorkHandler.sendMessageDelayed(mWorkHandler.obtainMessage(what, arg1, arg2), delayMillis);
    }

    public boolean sendMessageDelayed(int what, int arg1, int arg2, Object obj, long delayMillis) {
        return mWorkHandler.sendMessageDelayed(mWorkHandler.obtainMessage(what, arg1, arg2, obj), delayMillis);
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

    private boolean waitPendingCallback(long timeoutMS) {
        final AsyncResult result = new AsyncResult();
        mWorkHandler.post(new Runnable() {
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

                    boolean done = false;
                    synchronized (mSubHandlers) {
                        try {
                            for (SubHandler subHandler : mSubHandlers) {
                                done = subHandler.handleMessage(msg);
                                if (done)
                                    break;
                            }
                        } catch (Exception e) {
                            LogUtil.e(TAG, "WorkHandler::handleMessage error: ");
                            e.printStackTrace();
                        }
                    }
                    if (!done)
                        LogUtil.w(TAG, "WorkHandler::handleMessage get unknow message: " + msg.what);
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "WorkHandler::handleMessage error2: ");
                e.printStackTrace();
            }
        }
    }
}
