package sanp.mp100.integration;

import android.content.Context;
import android.content.SharedPreferences;
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

import sanp.mp100.R;

/**
 * Created by Tuyj on 2017/10/27.
 */

public class BusinessPlatformPostman implements Runnable {

    private enum State {
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

    BusinessPlatformPostman() {
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

    public int init(Context context) {
        synchronized (mInited) {
            if(!mInited) {
                mContext = context;
                mSharedPref = mContext.getSharedPreferences(mContext.getString(R.string.my_preferences), Context.MODE_PRIVATE);

                mActivated = mSharedPref.getBoolean(mContext.getString(R.string.has_activated), false);
                if(mActivated) {

                    ...; 将BusinessPlatform拆分成postman和BusinessPlatforml

                            mOrganization = new Organization();
                }

                mInited = true;
            }
            return 0;
        }
    }

    public boolean inited() {
        return mInited;
    }

    public boolean activated() {
        return mActivated;
    }

    public boolean bound() {
        return mBound;
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
                return -1;
            }
            if(mState != State.NONE) {
                LogManager.i("in busy");
                return -2;
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
                return -3;
            }

            while (mState == State.CONNECTING) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogManager.e("connect interrupted");
                    mState = State.NONE;
                    mWAMPSession = null;
                    return 4;
                }
            }

            if(mState != State.READY) {
                mState = State.NONE;
                mWAMPSession = null;
                return -5;
            }

            mWAMPSession.removeOnDisconnectListener(dis);
            return 0;
        }
    }

    /**
     * @param onStateChanged, been called while state has been changed
     *      @param onStateChanged::value, 0-disconnected, otherwise connected
     *      @param onStateChanged::args, useless
     *      @param onStateChanged::kwargs, useless
     * */
    public int asyncConnect(String websocketURL, String realm, Callback onStateChanged) {
        synchronized(mLock) {
            if(mState == State.READY) {
                LogManager.i("had connected");
                return -1;
            }
            if(mState != State.NONE) {
                LogManager.i("in busy");
                return -2;
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

            if(!mHandler.sendMessage(mHandler.obtainMessage(MSG_CONNECT))) {
                mState = State.NONE;
                mWAMPSession = null;
                return -3;
            }
            return 0;
        }
    }

    public int disconnect() {
        synchronized(mLock) {
            if (mState == State.NONE) {
                LogManager.i("had disconnected");
                return -1;
            }
            if (mState != State.READY) {
                LogManager.i("in busy");
                return -2;
            }

            mState = State.DISCONNECTING;
            mWAMPSession.leave();
            mWAMPSession = null;
            return 0;
        }
    }

    public boolean ready() {
        return mState == State.READY;
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
                        rets.add(0);
                    } else {
                        LogManager.e(String.format("invoke procedure(%s) with args(%s) kwargs(%s) fail: %s", procedureName, args, kwargs, throwable.getMessage()));
                        rets.add(-1);
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
                return -1;
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args, kwargs, (Class<CallResult>) null);
            f.whenComplete((callResult, throwable) -> {
                if (throwable == null) {
                    resultCallback.done(0, callResult.results, callResult.kwresults);
                } else {
                    resultCallback.done(-2, null, new HashMap<String, Object>(){{put("message", throwable.getMessage());}});
                }
            });
            return 0;
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
                        rets.add(0);
                    } else {
                        LogManager.e(String.format("procedure(%s) error: %s", procedureName, throwable.getMessage()));
                        rets.add(-1);
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
                return -1;
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args);
            f.whenComplete((callResult, throwable) -> onCallCompleted(callResult, throwable, classof, resultCallback));
            return 0;
        }
    }

    static private final String PROCEDURE_NAME_AREA_GET_PROVINCES   = "area.getProvinces";
    static private final String PROCEDURE_NAME_AREA_GET_CITIES      = "area.getCitiesByProvince";
    static private final String PROCEDURE_NAME_AREA_GET_DISTRICTS   = "area.getDistrictsByCity";
    static private final String PROCEDURE_NAME_ORG_GET_SCHOOLS      = "org.getSchoolsByArea";
    static private final String PROCEDURE_NAME_ORG_GET_CLASSES      = "org.getClassesBySchoolId";
    static private final String PROCEDURE_NAME_LESSON_GET_TIMETABLE = "lesson.getTimetable";

    // ---- get Provinces
    public List<Province> getAreaProvinces()
            throws RuntimeException, InterruptedException, InternalError {
        List<Province> provinces = new ArrayList<>();
        syncInvokeResultAsList(provinces, Province.class, PROCEDURE_NAME_AREA_GET_PROVINCES);
        return provinces;
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result(List<Province>) from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int getAreaProvinces(Callback resultCallback) {
        return asyncInvokeResultAsList(resultCallback, Province.class, PROCEDURE_NAME_AREA_GET_PROVINCES);
    }


    // ---- get Cities
    public List<City> getAreaCitiesByProvince(String province)
            throws RuntimeException, InterruptedException, InternalError {
        List<City> cities = new ArrayList<>();
        syncInvokeResultAsList(cities, City.class, PROCEDURE_NAME_AREA_GET_CITIES, province);
        return cities;
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result(List<City>) from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int getAreaCitiesByProvince(String province, Callback resultCallback) {
        return asyncInvokeResultAsList(resultCallback, City.class, PROCEDURE_NAME_AREA_GET_CITIES, province);
    }


    // ---- get Districts
    public List<District> getAreaDistrictsByCity(String province, String city)
            throws RuntimeException, InterruptedException, InternalError {
        List<District> districts = new ArrayList<>();
        syncInvokeResultAsList(districts, District.class, PROCEDURE_NAME_AREA_GET_DISTRICTS, province, city);
        return districts;
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result(List<District>) from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int getAreaDistrictsByCity(String province, String city, Callback resultCallback) {
        return asyncInvokeResultAsList(resultCallback, District.class, PROCEDURE_NAME_AREA_GET_DISTRICTS, province, city);
    }


    // ---- get Schools
    /**
     * e.g. ("北京市", "", "")
     * e.g. ("北京市", "北京市", "")
     * e.g. ("北京市", "北京市", "海淀区")
     * */
    public List<School> getOrgSchoolsByArea(String province, String city, String district)
            throws RuntimeException, InterruptedException, InternalError {
        List<School> schools = new ArrayList<>();
        syncInvokeResultAsList(schools, School.class, PROCEDURE_NAME_ORG_GET_SCHOOLS, province, city, district);
        return schools;
    }

    /**
     * e.g. ("北京市", "", "", resultCallback)
     * e.g. ("北京市", "北京市", "", resultCallback)
     * e.g. ("北京市", "北京市", "海淀区", resultCallback)
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result(List<School>) from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int getOrgSchoolsByArea(String province, String city, String district, Callback resultCallback) {
        return asyncInvokeResultAsList(resultCallback, School.class, PROCEDURE_NAME_ORG_GET_SCHOOLS, province, city, district);
    }


    // ---- get Classes
    public List<SchoolClass> getOrgClassesBySchoolId(long school_id)
            throws RuntimeException, InterruptedException, InternalError {
        List<SchoolClass> classes = new ArrayList<>();
        syncInvokeResultAsList(classes, SchoolClass.class, PROCEDURE_NAME_ORG_GET_CLASSES, school_id);
        return classes;
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result(List<SchoolClass>) from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int getOrgClassesBySchoolId(long school_id, Callback resultCallback) {
        return asyncInvokeResultAsList(resultCallback, SchoolClass.class, PROCEDURE_NAME_ORG_GET_CLASSES, school_id);
    }


    // ---- get LessonTimetable
    /**
     * @param start_date: e.g. "2017-10-16"
     * @param end_date: e.g. "2017-10-22"
     * */
    public List<TimeTable> getLessonTimetable(long class_id, String start_date, String end_date)
            throws RuntimeException, InterruptedException, InternalError {
        List<TimeTable> tables = new ArrayList<>();
        syncInvokeResultAsList(tables, TimeTable.class, PROCEDURE_NAME_LESSON_GET_TIMETABLE, class_id, start_date, end_date);
        return tables;
    }

    /**
     * @param start_date: e.g. "2017-10-16"
     * @param end_date: e.g. "2017-10-22"
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *  get result(List<TimeTable>) from resultCallback::args
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int getLessonTimetable(long class_id, String start_date, String end_date, Callback resultCallback) {
        return asyncInvokeResultAsList(resultCallback, TimeTable.class, PROCEDURE_NAME_LESSON_GET_TIMETABLE, class_id, start_date, end_date);
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
        LogManager.i(String.format("BusinessPlatformPostman(%lld) onDisconnecting with [reason=%s] [message=%s]", session.getID(), closeDetails.reason, closeDetails.message));
    }
}
