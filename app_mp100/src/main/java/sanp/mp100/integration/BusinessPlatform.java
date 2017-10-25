package sanp.mp100.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import java8.util.concurrent.CompletableFuture;
import sanp.avalon.libs.base.utils.LogManager;

/**
 * Created by Tuyj on 2017/10/25.
 */

public class BusinessPlatform {

    public interface Callback {
        void done(int value, List<Object> args, Map<String, Object> kwargs);
    }

    public class Province {
        public String id;
        public String province;
    }

    public class City {
        public String id;
        public String province;
        public String city;
    }

    public class District {
        public String id;
        public String province;
        public String city;
        public String district;
    }

    public class School {
        public String id;
        public String name;
        public String type;
        public String role;
    }

    public class SchoolClass {
        public String id;
        public String name;
        public String type;
    }

    public class TimeTable {
        public String id;
        public String type;
        public String subject_id;
        public String subject_name;
        public String title;
        public String teacher_id;
        public String teacher_name;
        public String date;
        public String section;
        public String duration;
        public String status;
    }

    private static BusinessPlatform gBusinessPlatform = null;
    public static BusinessPlatform getInstance() {
        if (gBusinessPlatform == null) {
            synchronized (BusinessPlatform.class) {
                if (gBusinessPlatform == null) {
                    gBusinessPlatform = new BusinessPlatform();
                }
            }
        }
        return gBusinessPlatform;
    }

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

    private String mWebSocketURL = "";
    private String mRealm = "";
    private Session mWAMPSession = null;
    private State mState = State.NONE;
    private Object mLock = new Object();

    BusinessPlatform() {

    }

    /**
     * return values:
     *      0: connected
     *      < 0 : action fail
     *      > 0 : connect fail
     * onStateChanged: value: 0-disconnected
     * */
    public int syncConnect(String websocketURL, String realm, Callback onStateChanged) {
        synchronized(mLock) {
            if(mState == State.READY) {
                LogManager.i("had connected");
                return -1;
            }
            if(mState != State.NONE) {
                LogManager.i("in busy");
                return -1;
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
            Client client = new Client(mWAMPSession, mWebSocketURL, mRealm);
            client.connect();

            while (mState == State.CONNECTING) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogManager.e("connect interrupted");
                    mState = State.NONE;
                    mWAMPSession = null;
                    return -2;
                }
            }

            mWAMPSession.removeOnDisconnectListener(dis);
            return (mState == State.READY ? 0 : 1);
        }
    }

    /**
     * return values:
     *      0: success to trigger connecting action
     *      others: fail to do
     * onStateChanged: value: 0-disconnected
     * */
    public int asyncConnect(String websocketURL, String realm, Callback onStateChanged) {
        synchronized(mLock) {
            if(mState == State.READY) {
                LogManager.i("had connected");
                return -1;
            }
            if(mState != State.NONE) {
                LogManager.i("in busy");
                return -1;
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
            Client client = new Client(mWAMPSession, mWebSocketURL, mRealm);
            client.connect();

            return 0;
        }
    }

    /**
     *  return values: 0-success to trigger disconnect, others-fail
     *  */
    public int disconnect() {
        synchronized(mLock) {
            if (mState == State.NONE) {
                LogManager.i("had disconnected");
                return -1;
            }
            if (mState != State.READY) {
                LogManager.i("in busy");
                return -1;
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


    static private final String PROCEDURE_NAME_AREA_GET_PROVINCES   = "area.getProvinces";
    static private final String PROCEDURE_NAME_AREA_GET_CITIES      = "area.getCitiesByProvince";
    static private final String PROCEDURE_NAME_AREA_GET_DISTRICTS   = "area.getDistrictsByCity";
    static private final String PROCEDURE_NAME_ORG_GET_SCHOOLS      = "org.getSchoolsByArea";
    static private final String PROCEDURE_NAME_ORG_GET_CLASSES      = "org.getClassesBySchoolId";
    static private final String PROCEDURE_NAME_LESSON_GET_TIMETABLE = "lesson.getTimetable";

    Boolean mProvincesReturened = false;
    List<Province> mProvinces = null;
    public List<Province> getAreaProvinces() {
        synchronized(mLock) {
            if(mState != State.READY) {
                LogManager.w("connnet first");
                return null;
            }

            mProvincesReturened = false;
            mProvinces = null;
            CompletableFuture<CallResult> f = mWAMPSession.call(PROCEDURE_NAME_AREA_GET_PROVINCES);
            f.whenComplete((callResult, throwable) -> {
                synchronized(mProvincesReturened) {
                    mProvincesReturened = true;
                    if (throwable == null) {
                        mProvinces = new ArrayList<>();
                        Gson gson = new Gson();
                        for(Object item: callResult.results) {
                            mProvinces.add(gson.fromJson(item.toString(), Province.class));
                        }
                    } else {
                        LogManager.e("getAreaProvinces error: " + throwable.getMessage());
                    }
                    mProvincesReturened.notify();
                }
            });

            synchronized(mProvincesReturened) {
                while (!mProvincesReturened) {
                    try {
                        mProvincesReturened.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        LogManager.e("getAreaProvinces interrupted");
                        return null;
                    }
                }
                return mProvinces;
            }
        }
    }

    /**
     * return values:
     *      0:  success to trigger invoking this rpc,
     *      otherwise: fail
     * resultCallback:
     *      if value is 0, it means the action has been done successfully. get List<Province> from args
     *      otherwise, value is error code, and get error message from kwargs ({"message": "..."})
     * */
    public int getAreaProvinces(Callback resultCallback) {
        synchronized(mLock) {
            if(mState != State.READY) {
                LogManager.w("connnet first");
                return -1;
            }

            CompletableFuture<CallResult> f = mWAMPSession.call(PROCEDURE_NAME_AREA_GET_PROVINCES);
            f.whenComplete((callResult, throwable) -> {
                if (throwable == null) {
                    List<Object> provinces = new ArrayList<>();
                    Gson gson = new Gson();
                    for(Object item: callResult.results) {
                        provinces.add(gson.fromJson(item.toString(), Province.class));
                    }
                    resultCallback.done(0, provinces, null);
                } else {
                    resultCallback.done(-2, null, new HashMap<String, Object>(){{put("message", throwable.getMessage());}});
                }
            });
            return 0;
        }
    }

    public List<City> getAreaCitiesByProvince(String province) {
        return null;
    }

    public int getAreaCitiesByProvince(Callback cb) {
        return -1;
    }

    public List<District> getAreaDistrictsByCity(String province, String city) {
        return null;
    }

    public int getAreaDistrictsByCity(Callback cb) {
        return -1;
    }

    public List<School> getOrgSchoolsByArea(String province, String city, String district) {
        return null;
    }

    public int getOrgSchoolsByArea(Callback cb) {
        return -1;
    }

    public List<SchoolClass> getOrgClassesBySchoolId(long school_id) {
        return null;
    }

    public int getOrgClassesBySchoolId(Callback cb) {
        return -1;
    }

    public List<TimeTable> getLessonTimetable(long class_id, String start_date, String end_date) {
        return null;
    }

    public int getLessonTimetable(Callback cb) {
        return -1;
    }

    private Boolean mInvokeReturened = false;
    private CallResult mCallResult = null;
    public CallResult syncInvoke(String procedureName, List<Object> args, Map<String, Object> kwargs) {
        synchronized(mLock) {
            if(mState != State.READY) {
                LogManager.w("connnet first");
                return null;
            }

            mInvokeReturened = false;
            mCallResult = null;
            CompletableFuture<CallResult> f = mWAMPSession.call(procedureName, args, kwargs, (Class<CallResult>) null);
            f.whenComplete((callResult, throwable) -> {
                synchronized(mInvokeReturened) {
                    mInvokeReturened = true;
                    if (throwable == null) {
                        mCallResult = callResult;
                    } else {
                        LogManager.e(String.format("invoke procedure(%s) with args(%s) kwargs(%s) fail: %s", procedureName, args, kwargs, throwable.getMessage()));
                    }
                    mInvokeReturened.notify();
                }
            });

            synchronized(mInvokeReturened) {
                while (!mInvokeReturened) {
                    try {
                        mInvokeReturened.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        LogManager.e(String.format("invoke procedure(%s) with args(%s) kwargs(%s) interrupted", procedureName, args, kwargs));
                        return null;
                    }
                }
                return mCallResult;
            }
        }
    }

    /**
     * return values:
     *      0:  success to trigger invoking this rpc,
     *      otherwise: fail
     * resultCallback:
     *      if value is 0, it means the action has been done successfully. get result from args and kwargs
     *      otherwise, value is error code, and get error message from kwargs ({"message": "..."})
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

    private void onReady(Session session, SessionDetails details, Callback actionCallback, Callback onStateChanged) {
        LogManager.i("BusinessPlatform(" + session.getID() + ") onReady");
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
        LogManager.i("BusinessPlatform(" + session.getID() + ") onDisconnected with" + (wasClean?"clean":"un-clean"));
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
        LogManager.i("BusinessPlatform(" + session.getID() + ") onConnecting");
    }

    private void onDisconnecting(Session session, CloseDetails closeDetails) {
        LogManager.i(String.format("BusinessPlatform(%lld) onDisconnecting with [reason=%s] [message=%s]", session.getID(), closeDetails.reason, closeDetails.message));
    }
}
