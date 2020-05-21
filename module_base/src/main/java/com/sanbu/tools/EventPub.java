package com.sanbu.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventPub {

    private static final String TAG = EventPub.class.getSimpleName();

    private static final int MSG_STOP = 0;
    private static final int MSG_POST = 1;
    private static final int MSG_SYNC = 2;

    public static class Event {
        public int id;
        public int arg1;
        public int arg2;
        public Object obj;

        public Event(int id) {
            this(id, -1, -1, null);
        }

        public Event(int id, int arg1) {
            this(id, arg1, -1, null);
        }

        public Event(int id, int arg1, int arg2, Object obj) {
            this.id = id;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.obj = obj;
        }
    }

    public enum Priority {
        Highest, // 最高级,仅允许有一个
        Higher,
        Normal,
    }

    public interface Callback {
        boolean/*eaten*/ onEvent(int evtId, int arg1, int arg2, Object obj);
    }

    private static EventPub gDefaultPub = null;

    public static EventPub getDefaultPub() {
        if (gDefaultPub == null) {
            synchronized (EventPub.class) {
                if (gDefaultPub == null) {
                    gDefaultPub = new EventPub();
                }
            }
        }
        return gDefaultPub;
    }

    private Object mLock = new Object();
    private Map<Integer, Subs> mSubs = new LinkedHashMap<>();

    private boolean mRunning = false;
    private Thread mThread = null;
    private Handler mHandler = null;

    public EventPub() {

    }

    public void init() {
        if (mHandler == null) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    mHandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            switch (msg.what) {
                            case MSG_STOP:
                                Looper.myLooper().quit();
                                break;
                            case MSG_POST:
                                dispatch((Event) msg.obj);
                                break;
                            case MSG_SYNC:
                                ((Runnable) msg.obj).run();
                                break;
                            default:
                                break;
                            }
                        }
                    };
                    notifyReady();
                    try {
                        Looper.loop();
                    } catch (Exception e) {
                        LogUtil.e(TAG, "Looper.loop error:");
                        e.printStackTrace();
                    }
                    notifyOver();
                }
            }, "looper@EventPub");

            // mThread.setDaemon(true);
            mThread.start();
            waitUntilReady();
        }
    }

    public void release() {
        if (mHandler != null && mThread != null) {
            mHandler.sendEmptyMessage(MSG_STOP);
            waitUntilOver();
            mHandler = null;
            mThread = null;
        }
        synchronized (mLock) {
            mSubs.clear();
        }
    }

    public Event post(int evtId) {
        Event event = new Event(evtId);
        post(event);
        return event;
    }

    public Event post(int evtId, int arg1, int arg2, Object obj) {
        Event event = new Event(evtId, arg1, arg2, obj);
        post(event);
        return event;
    }

    public void post(Event event) {
        if (mHandler == null)
            LogUtil.w(TAG, "init EventPub first");
        else
            mHandler.sendMessage(Message.obtain(null, MSG_POST, 0, 0, event));
    }

    public void postDelayed(Event event, long delayMillis) {
        if (mHandler == null)
            LogUtil.w(TAG, "init EventPub first");
        else
            mHandler.sendMessageDelayed(Message.obtain(null, MSG_POST, 0, 0, event), delayMillis);
    }

    public void cancel(Event event) {
        if (mHandler != null)
            mHandler.removeMessages(MSG_POST, event);
    }

    public boolean subscribe(int evtId, String subTag, Callback subCallback) {
        return subscribe(evtId, subTag, Priority.Normal, subCallback);
    }

    public boolean subscribe(int evtId, String subTag, Priority subPriority, Callback subCallback) {
        synchronized (mLock) {
            Subs subs = mSubs.get(evtId);
            if (subs == null) {
                subs = new Subs();
                mSubs.put(evtId, subs);
            }

            List<Sub> subList;
            if (subPriority == Priority.Highest) {
                if (subs.subList[0].size() > 0)
                    return false;
                subList = subs.subList[0];
            } else {
                subList = (subPriority == Priority.Higher ? subs.subList[1] : subs.subList[2]);
            }

            subList.add(new Sub(subTag, subCallback));
            return true;
        }
    }

    public void unsubscribe(int evtId, String subTag) {
        synchronized (mLock) {
            Subs subs = mSubs.get(evtId);
            if (subs != null) {
                for (List<Sub> subList: subs.subList) {
                    for (int i = 0; i < subList.size(); ++i) {
                        if (subList.get(i).tag.equals(subTag)) {
                            subList.remove(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    public int syncPending() {
        return Pending.sync(mHandler);
    }

    @Deprecated
    public static int syncPending(Handler handler) {
        return Pending.sync(handler);
    }

    private void dispatch(Event event) {
        synchronized (mLock) {
            Subs subs = mSubs.get(event.id);
            if (subs != null) {
                for (List<Sub> subList: subs.subList) {
                    for (Sub sub: subList) {
                        if (sub.callback.onEvent(event.id, event.arg1, event.arg2, event.obj))
                            return;
                    }
                }
            }
        }
    }

    private void waitUntilReady() {
        synchronized (mLock) {
            while (!mRunning) {
                try {
                    mLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void notifyReady() {
        synchronized (mLock) {
            mRunning = true;
            mLock.notify();
        }
    }

    private void waitUntilOver() {
        synchronized (mLock) {
            while (mRunning) {
                try {
                    mLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void notifyOver() {
        synchronized (mLock) {
            mRunning = false;
            mLock.notify();
        }
    }

    private class Sub {
        public String tag;
        public Callback callback;

        Sub(String tag, Callback callback) {
            this.tag = tag;
            this.callback = callback;
        }
    }

    private class Subs {
        public List<Sub>[] subList = new List[]{new LinkedList(), new LinkedList<>(), new LinkedList<>()};
    }

}
