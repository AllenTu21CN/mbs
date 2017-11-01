package sanp.mp100.integration;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import java8.util.concurrent.CompletableFuture;
import sanp.avalon.libs.base.utils.LogManager;

import sanp.mp100.integration.BusinessPlatform.Callback;

/**
 * Created by Tuyj on 2017/10/27.
 */

public class BusinessPlatformPostman implements Runnable {

    public static class BPError {
        public static final int ACTION_SUCCESS = 0;
        public static final int ERROR_IMPORTANT_START = -100;
        public static final int ERROR_IMPORTANT_LOGICAL_ERROR   =  ERROR_IMPORTANT_START - 1;
        public static final int ERROR_IMPORTANT_ACTION_SKIP     =  ERROR_IMPORTANT_START - 2;
        public static final int ERROR_IMPORTANT_ACTION_FAIL     =  ERROR_IMPORTANT_START - 3;
        public static final int ERROR_IMPORTANT_INTERRUPTED     =  ERROR_IMPORTANT_START - 4;
        public static final int ERROR_IMPORTANT_DELIVER_FAIL    =  ERROR_IMPORTANT_START - 5;
    }

    public enum State {
        NONE(0),
        CONNECTING(1),
        READY(2),
        DISCONNECTING(3);

        private int value = -1;
        State(int value) {
            this.value = value;
        }
        public int toValue() {
            return value;
        }
    };

    private static final int MSG_CONNECT = 0;
    private static final int MSG_STOP_LOOP = -1;

    private static final long THREAD_EXIT_TIMEOUT_MS = 1000;

    private String mWebSocketURL = "";
    private String mRealm = "";
    private Session mWAMPSession = null;
    private State mState = State.NONE;
    private Object mLock = new Object();

    private Thread mThread = null;
    private boolean mRunning = false;
    private Object mRunningLock = new Object();
    private Handler mHandler = null;

    public BusinessPlatformPostman() {
        if (mThread == null) {
            mThread = new Thread(this, "BusinessPlatformPostman");
            mThread.start();
            waitUntilReady();
        }
    }

    void release() {
        if (mThread != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_LOOP));
            waitUntilOver();
            try {
                mThread.join(THREAD_EXIT_TIMEOUT_MS);
            } catch (InterruptedException e) {
                LogManager.e(e);
            }
            mThread = null;
        }
    }

    /**
     * @param onStateChanged, been called while state has been changed
     *      @param onStateChanged::value, 0-disconnected, otherwise connected
     *      @param onStateChanged::args, useless
     *      @param onStateChanged::kwargs, useless
     * */
    public int syncConnect(String websocketURL, String realm, Callback onStateChanged) {
        synchronized(mLock) {
            if(mState == State.READY) {
                LogManager.i("had connected");
                return BPError.ERROR_IMPORTANT_ACTION_SKIP;
            }
            if(mState != State.NONE) {
                LogManager.i("in busy");
                return BPError.ERROR_IMPORTANT_LOGICAL_ERROR;
            }

            mRealm = realm;
            mWebSocketURL = websocketURL;
            mState = State.CONNECTING;

            mWAMPSession = new Session();
            mWAMPSession.addOnJoinListener((session, details) ->
                    onReady(session, details, null, onStateChanged)
            );
            ISession.OnDisconnectListener dis = mWAMPSession.addOnDisconnectListener((session, wasClean) ->
                    onDisconnected(session, wasClean, null)
            );
            mWAMPSession.addOnConnectListener(this::onConnecting);
            mWAMPSession.addOnLeaveListener(this::onDisconnecting);

            if(!mHandler.sendMessage(mHandler.obtainMessage(MSG_CONNECT))) {
                LogManager.e("send looper message to try to connect failed");
                mState = State.NONE;
                mWAMPSession = null;
                return BPError.ERROR_IMPORTANT_DELIVER_FAIL;
            }

            while (mState == State.CONNECTING) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogManager.e("connect interrupted");
                    mState = State.NONE;
                    mWAMPSession = null;
                    return BPError.ERROR_IMPORTANT_INTERRUPTED;
                }
            }

            if(mState != State.READY) {
                mState = State.NONE;
                mWAMPSession = null;
                return BPError.ERROR_IMPORTANT_ACTION_FAIL;
            }

            mWAMPSession.removeOnDisconnectListener(dis);
            return BPError.ACTION_SUCCESS;
        }
    }

    /**
     * @param onStateChanged, been called while state has been changed
     *      @param onStateChanged::value, 0-disconnected, otherwise connected
     *      @param onStateChanged::args, useless
     *      @param onStateChanged::kwargs, useless
     * */
    public int asyncConnect(String websocketURL, String realm, Callback onStateChanged) {
        return asyncConnect(websocketURL, realm, onStateChanged, 0);
    }
    public int asyncConnect(String websocketURL, String realm, Callback onStateChanged, long actionDelayMs) {
        synchronized(mLock) {
            if(mState == State.READY) {
                LogManager.i("had connected");
                return BPError.ERROR_IMPORTANT_ACTION_SKIP;
            }
            if(mState != State.NONE) {
                LogManager.i("in busy");
                return BPError.ERROR_IMPORTANT_LOGICAL_ERROR;
            }

            mRealm = realm;
            mWebSocketURL = websocketURL;
            mState = State.CONNECTING;

            mWAMPSession = new Session();
            mWAMPSession.addOnJoinListener((session, details) ->
                    onReady(session, details, onStateChanged, null)
            );
            mWAMPSession.addOnDisconnectListener((session, wasClean) ->
                    onDisconnected(session, wasClean, onStateChanged)
            );
            mWAMPSession.addOnConnectListener(this::onConnecting);
            mWAMPSession.addOnLeaveListener(this::onDisconnecting);

            if(!mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CONNECT), actionDelayMs)) {
                mState = State.NONE;
                mWAMPSession = null;
                return BPError.ERROR_IMPORTANT_DELIVER_FAIL;
            }
            return BPError.ACTION_SUCCESS;
        }
    }

    public int disconnect() {
        synchronized(mLock) {
            if (mState == State.NONE) {
                LogManager.i("had disconnected");
                return BPError.ERROR_IMPORTANT_ACTION_SKIP;
            }
            if (mState != State.READY) {
                LogManager.i("in busy");
                return BPError.ERROR_IMPORTANT_LOGICAL_ERROR;
            }

            mState = State.DISCONNECTING;
            mWAMPSession.leave();
            mWAMPSession = null;
            return BPError.ACTION_SUCCESS;
        }
    }

    public boolean ready() {
        return mState == State.READY;
    }

    public boolean disconnected() {
        return mState == State.NONE;
    }

    public State state() {
        return mState;
    }

    public CallResult syncInvoke(String procedureName, List<Object> args, Map<String, Object> kwargs)
            throws RuntimeException, InterruptedException, InternalError {

        Object lock = new Object();
        List<Object> rets = new ArrayList<>();
        CallResult result = new CallResult(null, null);

        synchronized(mLock) {
            if (mState != State.READY) {
                throw new RuntimeException("connnet first");
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args, kwargs, (Class<CallResult>) null);
            f.whenComplete((callResult, throwable) -> {
                synchronized (lock) {
                    if (throwable == null) {
                        result.results = callResult.results;
                        result.kwresults = callResult.kwresults;
                        rets.add(BPError.ACTION_SUCCESS);
                    } else {
                        LogManager.e(String.format("invoke procedure(%s) with args(%s) kwargs(%s) fail: %s", procedureName, args, kwargs, throwable.getMessage()));
                        rets.add(BPError.ERROR_IMPORTANT_ACTION_FAIL);
                        rets.add(throwable.getMessage());
                    }
                    lock.notify();
                }
            });
        }

        synchronized(lock) {
            while (rets.size() == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogManager.e(String.format("invoke procedure(%s) with args(%s) kwargs(%s) interrupted", procedureName, args, kwargs));
                    throw e;
                }
            }
            if((Integer)rets.get(0) != 0)
                throw new InternalError((String) rets.get(1));
            return result;
        }
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result from resultCallback::args and resultCallback::kwargs
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int asyncInvoke(String procedureName, List<Object> args, Map<String, Object> kwargs, Callback resultCallback) {
        synchronized(mLock) {
            if(mState != State.READY) {
                LogManager.w("connnet first");
                return BPError.ERROR_IMPORTANT_LOGICAL_ERROR;
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args, kwargs, (Class<CallResult>) null);
            f.whenComplete((callResult, throwable) -> {
                if (throwable == null) {
                    resultCallback.done(0, callResult.results, callResult.kwresults);
                } else {
                    resultCallback.done(-2, null, new HashMap<String, Object>(){{put("message", throwable.getMessage());}});
                }
            });
            return BPError.ACTION_SUCCESS;
        }
    }

    /**
     * @param result, keep the results
     * */
    public <T> void syncInvokeResultAsList(List<T> result, Class<T> classof, String procedureName, Object... args)
            throws RuntimeException, InterruptedException, InternalError {
        Object lock = new Object();
        List<Object> rets = new ArrayList<>();

        synchronized(mLock) {
            if (mState != State.READY) {
                throw new RuntimeException("connnet first");
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args);
            f.whenComplete((callResult, throwable) -> {
                synchronized (lock) {
                    if (throwable == null) {
                        Gson gson = new Gson();
                        for (Object item : callResult.results) {
                            result.add(gson.fromJson(item.toString(), classof));
                        }
                        rets.add(BPError.ACTION_SUCCESS);
                    } else {
                        LogManager.e(String.format("procedure(%s) error: %s", procedureName, throwable.getMessage()));
                        rets.add(BPError.ERROR_IMPORTANT_ACTION_FAIL);
                        rets.add(throwable.getMessage());
                    }
                    lock.notify();
                }
            });
        }

        synchronized(lock) {
            while (rets.size() == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogManager.e(procedureName + " interrupted");
                    throw e;
                }
            }
            if((Integer)rets.get(0) != 0)
                throw new InternalError((String) rets.get(1));
        }
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get results from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public <T> int asyncInvokeResultAsList(Callback resultCallback, Class<T> classof, String procedureName, Object... args) {
        synchronized(mLock) {
            if(mState != State.READY) {
                LogManager.w("connnet first");
                return BPError.ERROR_IMPORTANT_LOGICAL_ERROR;
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args);
            f.whenComplete((callResult, throwable) -> onCallCompleted(callResult, throwable, classof, resultCallback));
            return BPError.ACTION_SUCCESS;
        }
    }

    @Override
    public void run() {
        LogManager.i("BusinessPlatformPostman thread started!");
        Looper.prepare();
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CONNECT: {
                        tryConnect();
                        break;
                    }
                    case MSG_STOP_LOOP: {
                        Looper.myLooper().quit();
                        break;
                    }
                    default: {
                        LogManager.e("Unknown message type: " + msg.what);
                    }
                }
            }
        };
        notifyReady();
        Looper.loop();
        notifyOver();
        LogManager.i("BusinessPlatformPostman thread exit...!");
    }

    private void tryConnect() {
        Client client = new Client(mWAMPSession, mWebSocketURL, mRealm);
        client.connect();
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

    private void onCallCompleted(CallResult callResult, Throwable throwable, Class classof, Callback resultCallback) {
        if (throwable == null) {
            List<Object> objs = new ArrayList<>();
            Gson gson = new Gson();
            for(Object item: callResult.results) {
                objs.add(gson.fromJson(item.toString(), classof));
            }
            resultCallback.done(0, objs, null);
        } else {
            resultCallback.done(-2, null, new HashMap<String, Object>(){{put("message", throwable.getMessage());}});
        }
    }

    private void onReady(Session session, SessionDetails details, Callback actionCallback, Callback onStateChanged) {
        LogManager.i("BusinessPlatformPostman(" + session.getID() + ") onReady");
        synchronized (mLock) {
            mState = State.READY;
            if(onStateChanged != null) {
                mWAMPSession.addOnDisconnectListener((ses, wasClean) ->
                        onDisconnected(ses, wasClean, onStateChanged)
                );
            }
            if(actionCallback != null) {
                actionCallback.done(mState.toValue(), null, null);
            } else {
                mLock.notify();
            }
        }
    }

    private void onDisconnected(Session session, boolean wasClean, Callback cb) {
        LogManager.i("BusinessPlatformPostman(" + session.getID() + ") onDisconnected with" + (wasClean?"clean":"un-clean"));
        synchronized (mLock) {
            mState = State.NONE;
            if(cb != null) {
                cb.done(mState.toValue(), null, null);
            } else {
                mLock.notify();
            }
        }
    }

    private void onConnecting(Session session) {
        LogManager.i("BusinessPlatformPostman(" + session.getID() + ") onConnecting");
    }

    private void onDisconnecting(Session session, CloseDetails closeDetails) {
        LogManager.i(String.format("BusinessPlatformPostman(%d) onDisconnecting with [reason=%s] [message=%s]", session.getID(), closeDetails.reason, closeDetails.message));
    }
}
