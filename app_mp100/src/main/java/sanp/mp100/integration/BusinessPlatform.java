package sanp.mp100.integration;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.JsonToken;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.Tuple3;
import sanp.mp100.MP100Application;
import sanp.mp100.R;
import sanp.mp100.integration.BusinessPlatformPostman.BPError;

/**
 * Created by Tuyj on 2017/10/25.
 */

public class BusinessPlatform {

    public interface Callback {
        void done(int value, List<Object> args, Map<String, Object> kwargs);
    }

    public interface Observer {
        void onConnectingState(boolean connected);
        void onActivatingState(boolean activated);
        void onBindingState(boolean bound);
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
        public long id;
        public String name;
        public String type;
        public String role;
    }

    public class SchoolClass {
        public long id;
        public String name;
        public String type;
    }

    public static class TimeTable {
        public long id;
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

    public class LessonInfo {
        public String program_uuid;
        public String stream_name;
        public LessonInfo(String pgm_uuid, String stream_name) {
            this.program_uuid = pgm_uuid;
            this.stream_name = stream_name;
        }
    };

    static public class ConnectionSettings {
        public String WebSocketURL;
        public String Realm;

        public boolean RetryConnectEnable;
        public int RetryConnectTimes;
        public long RetryConnectIntervalMS;

        public boolean KeepAliveEnable;
        public int KeepAliveRetryTimes;
        public long KeepAliveIntervalMS;
        public long KeepAliveTimeoutMS;

        boolean equals(ConnectionSettings other) {
            return (
                    WebSocketURL == other.WebSocketURL && Realm == other.Realm &&
                    RetryConnectEnable == other.RetryConnectEnable &&
                    RetryConnectTimes == other.RetryConnectTimes &&
                    RetryConnectIntervalMS == other.RetryConnectIntervalMS &&
                    KeepAliveEnable == other.KeepAliveEnable &&
                    KeepAliveRetryTimes == other.KeepAliveRetryTimes &&
                    KeepAliveIntervalMS == other.KeepAliveIntervalMS &&
                    KeepAliveTimeoutMS == other.KeepAliveTimeoutMS
            );
        }
    }

    static public class ActivatingConfig {
        // TODO:

        boolean equals(ActivatingConfig other) {
            return true;
        }
    }

    static public class Organization {
        public Province province;
        public City city;
        public District district;
        public School school;
        public SchoolClass schoolClass;
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

    public enum State {
        None(0),
        Doing(1),
        Done(2);

        private int value;
        private State(int value) {
            this.value = value;
        }
        public int toValue() {
            return value;
        }
    };

    private static final long THREAD_EXIT_TIMEOUT_MS = 5000;

    static private final String PROCEDURE_NAME_AREA_GET_PROVINCES   = "area.getProvinces";
    static private final String PROCEDURE_NAME_AREA_GET_CITIES      = "area.getCitiesByProvince";
    static private final String PROCEDURE_NAME_AREA_GET_DISTRICTS   = "area.getDistrictsByCity";
    static private final String PROCEDURE_NAME_ORG_GET_SCHOOLS      = "org.getSchoolsByArea";
    static private final String PROCEDURE_NAME_ORG_GET_CLASSES      = "org.getClassesBySchoolId";
    static private final String PROCEDURE_NAME_LESSON_GET_TIMETABLE = "lesson.getTimetable";
    static private final String PROCEDURE_NAME_LESSON_START_PLANNED = "lesson.startPlanned";
    static private final String PROCEDURE_NAME_LESSON_STOP_PLANNED  = "lesson.stopPlanned";

    private boolean mInited = false;

    private boolean mAllowConnect;
    private State mActivating;
    private State mBinding;

    private boolean mHasConnectionSettings;
    private boolean mHasActivatingConfig;
    private boolean mHasOrganization;

    private ConnectionSettings mConnectionSettings;
    private ActivatingConfig mActivatingConfig;
    private Organization mOrganization;
    private String mRtmpPrefix;

    private Object mLock = new Object();
    private Context mContext;
    private SharedPreferences mSharedPref;
    private BusinessPlatformPostman mPlatformPostman;

    private List<Observer> mObservers = new ArrayList<>();

    private int mCurrentRetryConnectTimes;

    private Tuple3<Long/*timetable_id*/, Integer/*output_id*/, String/*url*/> inClassLesson = null;

    BusinessPlatform() {
        reset();
    }

    private void reset() {
        mAllowConnect = true;
        mActivating = State.None;
        mBinding = State.None;

        mHasConnectionSettings = false;
        mHasActivatingConfig = false;
        mHasOrganization = false;

        mConnectionSettings = null;
        mActivatingConfig = null;
        mOrganization = null;
        mRtmpPrefix = null;

        mContext = null;
        mSharedPref = null;
        mPlatformPostman = null;

        mCurrentRetryConnectTimes = -1;
    }

    public void init(Context context) {
        if(mInited)
            return;

        mContext = context;
        mSharedPref = mContext.getSharedPreferences(mContext.getString(R.string.my_platform_preferences), Context.MODE_PRIVATE);
        mPlatformPostman = new BusinessPlatformPostman();

        loadPreferences();
        mInited = true;

        tryConnect();
    }

    public void release() {
        disconnect();

        synchronized (mLock) {
            if(!mInited)
                return;

            if (!mPlatformPostman.disconnected()) {
                try {
                    mLock.wait(THREAD_EXIT_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mPlatformPostman.release();
            reset();

            mInited = false;
        }
    }

    public boolean isInited() {
        return mInited;
    }

    public void addStateObserver(Observer ob) {
        synchronized (mLock) {
            if(!mObservers.contains(ob))
                mObservers.add(ob);
        }
    }

    public void removeStateObserver(Observer ob) {
        synchronized (mLock) {
            if(mObservers.contains(ob))
                mObservers.remove(ob);
        }
    }

    public int connect() {
        mAllowConnect = true;
        return tryConnect();
    }

    public int connect(ConnectionSettings connectionSettings) {
        if(mInited && mPlatformPostman.ready())
            throw new RuntimeException("logical error: had connected");
        synchronized (mLock) {
            if(mConnectionSettings == null || !mConnectionSettings.equals(connectionSettings)) {
                mConnectionSettings = connectionSettings;
                saveConnectionSettings();
            }
        }
        return connect();
    }

    public int disconnect() {
        mAllowConnect = false;
        if(mPlatformPostman != null)
            return mPlatformPostman.disconnect();
        return 0;
    }

    public void activate() {
        if(!mInited || mActivating == State.Doing || mActivating == State.Done || !mPlatformPostman.ready())
            return;
        LogManager.w("TODO: try to activate on platform");
        // mPlatformPostman.activate();
        onActivating(true);
    }

    public void activate(ActivatingConfig activatingConfig) {
        if(mActivating == State.Doing)
            throw new RuntimeException("logical error: in activating");
        if(mActivating == State.Done)
            throw new RuntimeException("logical error: had activated");

        synchronized (mLock) {
            if(mActivatingConfig == null || !mActivatingConfig.equals(activatingConfig)) {
                mActivatingConfig = activatingConfig;
                saveActivatingConfig();
            }
        }
        activate();
    }

    public void deactivate() {
        if(!mInited || mActivating == State.None || !mPlatformPostman.ready())
            throw new RuntimeException("logical error");

        unbind();
        LogManager.w("TODO: try to deactivate on platform");
        // mPlatformPostman.deactivate();
        onActivating(false);
    }

    public void bind() {
        LogManager.w("TODO: try to bind the organization on platform");
    }

    public void unbind() {
        LogManager.w("TODO: try to unbind the organization on platform");
    }

    public BusinessPlatformPostman.State connectingState() {
        if(!mInited)
            return BusinessPlatformPostman.State.NONE;
        return mPlatformPostman.state();
    }

    public State activatingState() {
        return mActivating ;
    }

    public State bindingState() {
        return mBinding;
    }

    public BusinessPlatformPostman getmPlatformPostman() {
        return mPlatformPostman;
    }

    public ConnectionSettings getMyConnectionSettings() {
        return mConnectionSettings;
    }

    public ActivatingConfig getMyActivatingConfig() {
        return mActivatingConfig;
    }

    public Organization getMyOrganization() {
        return mOrganization;
    }

    public List<TimeTable> getMyLessonTimetable(String start_date, String end_date)
            throws RuntimeException, InterruptedException, InternalError {
        if(mBinding != State.Done)
            throw new RuntimeException("logical error: bind first");
        return getLessonTimetable(mOrganization.schoolClass.id, start_date, end_date);
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
    public int getMyLessonTimetable(long class_id, String start_date, String end_date, Callback resultCallback) {
        if(mBinding != State.Done)
            throw new RuntimeException("logical error: bind first");
        return getLessonTimetable(mOrganization.schoolClass.id, start_date, end_date, resultCallback);
    }

    // ---- get Provinces
    public List<Province> getAreaProvinces()
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        List<Province> provinces = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(provinces, Province.class, PROCEDURE_NAME_AREA_GET_PROVINCES);
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
        if (!mInited)
            throw new RuntimeException("init first");
        return mPlatformPostman.asyncInvokeResultAsList(resultCallback, Province.class, PROCEDURE_NAME_AREA_GET_PROVINCES);
    }


    // ---- get Cities
    public List<City> getAreaCitiesByProvince(String province)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        List<City> cities = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(cities, City.class, PROCEDURE_NAME_AREA_GET_CITIES, province);
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
        if (!mInited)
            throw new RuntimeException("init first");
        return mPlatformPostman.asyncInvokeResultAsList(resultCallback, City.class, PROCEDURE_NAME_AREA_GET_CITIES, province);
    }


    // ---- get Districts
    public List<District> getAreaDistrictsByCity(String province, String city)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        List<District> districts = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(districts, District.class, PROCEDURE_NAME_AREA_GET_DISTRICTS, province, city);
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
        if (!mInited)
            throw new RuntimeException("init first");
        return mPlatformPostman.asyncInvokeResultAsList(resultCallback, District.class, PROCEDURE_NAME_AREA_GET_DISTRICTS, province, city);
    }


    // ---- get Schools
    /**
     * e.g. ("北京市", "", "")
     * e.g. ("北京市", "北京市", "")
     * e.g. ("北京市", "北京市", "海淀区")
     * */
    public List<School> getOrgSchoolsByArea(String province, String city, String district)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        List<School> schools = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(schools, School.class, PROCEDURE_NAME_ORG_GET_SCHOOLS, province, city, district);
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
        if (!mInited)
            throw new RuntimeException("init first");
        return mPlatformPostman.asyncInvokeResultAsList(resultCallback, School.class, PROCEDURE_NAME_ORG_GET_SCHOOLS, province, city, district);
    }


    // ---- get Classes
    public List<SchoolClass> getOrgClassesBySchoolId(long school_id)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        List<SchoolClass> classes = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(classes, SchoolClass.class, PROCEDURE_NAME_ORG_GET_CLASSES, school_id);
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
        if (!mInited)
            throw new RuntimeException("init first");
        return mPlatformPostman.asyncInvokeResultAsList(resultCallback, SchoolClass.class, PROCEDURE_NAME_ORG_GET_CLASSES, school_id);
    }


    // ---- get LessonTimetable
    /**
     * @param start_date: e.g. "2017-10-16"
     * @param end_date: e.g. "2017-10-22"
     * */
    public List<TimeTable> getLessonTimetable(long class_id, String start_date, String end_date)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        List<TimeTable> tables = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(tables, TimeTable.class, PROCEDURE_NAME_LESSON_GET_TIMETABLE, class_id, start_date, end_date);
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
        if (!mInited)
            throw new RuntimeException("init first");
        return mPlatformPostman.asyncInvokeResultAsList(resultCallback, TimeTable.class, PROCEDURE_NAME_LESSON_GET_TIMETABLE, class_id, start_date, end_date);
    }


    // ---- start planned
    public void startPlanned(long timetable_id)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        if(inClassLesson != null) {
            if(inClassLesson.first == timetable_id) {
                LogManager.w(String.format("the class[%lld] had been started", timetable_id));
                return;
            } else {
                throw new RuntimeException(String.format("logical error: has been in class(timetable_id-%ld output_id-%d url-%s)!!!can't start new class(%lld)",
                        inClassLesson.first, inClassLesson.second, inClassLesson.third, timetable_id));
            }
        }

        List<LessonInfo> infos = new ArrayList<>();
        mPlatformPostman.syncInvokeResultAsList(infos, LessonInfo.class, PROCEDURE_NAME_LESSON_START_PLANNED, timetable_id);
        startRtmpOutput(infos.get(0), timetable_id);
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int startPlanned(long timetable_id, Callback resultCallback) {
        if (!mInited)
            throw new RuntimeException("init first");
        if(inClassLesson != null) {
            if(inClassLesson.first == timetable_id) {
                LogManager.w(String.format("the class[%lld] had been started", timetable_id));
                return BPError.ERROR_IMPORTANT_ACTION_SKIP;
            } else {
                throw new RuntimeException(String.format("logical error: has been in class(timetable_id-%ld output_id-%d url-%s)!!!can't start new class(%lld)",
                        inClassLesson.first, inClassLesson.second, inClassLesson.third, timetable_id));
            }
        }

        return mPlatformPostman.asyncInvokeResultAsList(
                ((value, args, kwargs) -> {
                    if(value == 0) {
                        try {
                            startRtmpOutput((LessonInfo) args.get(0), timetable_id);
                            resultCallback.done(0, null, null);
                        } catch (InternalError e) {
                            resultCallback.done(-3, null, new HashMap<String, Object>(){{put("message", e.getMessage());}});
                        }
                    } else {
                        resultCallback.done(value, args, kwargs);
                    }
                }),
                LessonInfo.class,
                PROCEDURE_NAME_LESSON_START_PLANNED,
                timetable_id);
    }


    // ---- stop planned
    public void stopPlanned(long timetable_id)
            throws RuntimeException, InterruptedException, InternalError {
        if (!mInited)
            throw new RuntimeException("init first");
        if(inClassLesson == null)
            throw new RuntimeException(String.format("logical error: the class[%lld] has not been started", timetable_id));
        if(inClassLesson.first != timetable_id)
            throw new RuntimeException(String.format("logical error: the timetable_id[%lld] is not match current class(timetable_id-%ld output_id-%d url-%s)!!!",
                    timetable_id, inClassLesson.first, inClassLesson.second, inClassLesson.third));

        RBUtil.getInstance().removeOutput(inClassLesson.second);
        mPlatformPostman.syncInvoke(PROCEDURE_NAME_LESSON_STOP_PLANNED, Arrays.asList(timetable_id), null);
        inClassLesson = null;
    }

    /**
     * @param resultCallback:
     *      if the resultCallback::value is 0, that means the action has been done successfully,
     *      otherwise, the resultCallback::value indicates error code, and get error message from
     *  resultCallback::kwargs ({"message": "..."})
     * */
    public int stopPlanned(long timetable_id, Callback resultCallback) {
        if (!mInited)
            throw new RuntimeException("init first");
        if(inClassLesson == null)
            throw new RuntimeException(String.format("logical error: the class[%lld] has not been started", timetable_id));
        if(inClassLesson.first != timetable_id)
            throw new RuntimeException(String.format("logical error: the timetable_id[%lld] is not match current class(timetable_id-%ld output_id-%d url-%s)!!!",
                    timetable_id, inClassLesson.first, inClassLesson.second, inClassLesson.third));

        RBUtil.getInstance().removeOutput(inClassLesson.second);
        return mPlatformPostman.asyncInvoke(PROCEDURE_NAME_LESSON_STOP_PLANNED, Arrays.asList(timetable_id), null, resultCallback);
    }


    private void loadPreferences() {
        boolean hadActivated = mSharedPref.getBoolean(
                mContext.getString(R.string.platform_had_activated),
                mContext.getString(R.string.platform_had_activated_default).equals("true"));
        mActivating = hadActivated ? State.Done : State.None;

        boolean hadBound = mSharedPref.getBoolean(
                mContext.getString(R.string.platform_had_bound),
                mContext.getString(R.string.platform_had_bound_default).equals("true"));
        mBinding = hadBound ? State.Done : State.None;

        loadConnectionSettings();
        loadActivatingConfig();
        loadOrganization();

        if(hadActivated) {
            if(!mHasConnectionSettings ||
               !mHasActivatingConfig ||
               mRtmpPrefix == null ||
               mConnectionSettings == null ||
               mActivatingConfig == null)
                throw new RuntimeException("loadPreferences logical error 1");
        } else {
            if(mHasConnectionSettings && mConnectionSettings == null)
                throw new RuntimeException("loadPreferences logical error 2");
            if(mHasActivatingConfig && mActivatingConfig == null)
                throw new RuntimeException("loadPreferences logical error 3");
        }

        if(hadBound) {
            if(!mHasOrganization || mOrganization == null)
                throw new RuntimeException("loadPreferences logical error 4");
            if(!hadActivated)
                throw new RuntimeException("loadPreferences logical error 5");
        } else {
            if(mHasOrganization && mOrganization == null)
                throw new RuntimeException("loadPreferences logical error 6");
        }
    }

    private void loadConnectionSettings() {
        mHasConnectionSettings = mSharedPref.getBoolean(
                mContext.getString(R.string.platform_has_connection_settings),
                mContext.getString(R.string.platform_has_connection_settings_default).equals("true"));
        if(!mHasConnectionSettings)
            return;

        String connection = mSharedPref.getString(mContext.getString(R.string.platform_connection), null);
        if(connection == null) {
            mConnectionSettings = MP100Application.loadSettingsFromTmpFile("connection.json", ConnectionSettings.class);
        } else {
            mConnectionSettings = new Gson().fromJson(connection, ConnectionSettings.class);
        }
        LogManager.i("loaded connection settings: \n" + new Gson().toJson(mConnectionSettings));
        mCurrentRetryConnectTimes = mConnectionSettings.RetryConnectTimes;

        mRtmpPrefix = mSharedPref.getString(mContext.getString(R.string.platform_rtmp_prefix), null);
        if(mRtmpPrefix == null) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> rtmpConfig = MP100Application.loadSettingsFromTmpFile("rtmp_config.json", type);
            mRtmpPrefix = rtmpConfig.get("rtmp_prefix");
        }
        LogManager.i("loaded rtmp prefix: " + mRtmpPrefix);
    }

    private void saveConnectionSettings() {
        if(mConnectionSettings == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(mContext.getString(R.string.platform_has_connection_settings), true);
        editor.putString(mContext.getString(R.string.platform_connection), new Gson().toJson(mConnectionSettings));
        editor.commit();
    }

    private void loadActivatingConfig() {
        mHasActivatingConfig = mSharedPref.getBoolean(
                mContext.getString(R.string.platform_has_activating_config),
                mContext.getString(R.string.platform_has_activating_config_default).equals("true"));
        if(!mHasActivatingConfig)
            return;

        String activating = mSharedPref.getString(mContext.getString(R.string.platform_activating_config), null);
        if(activating == null)
            activating = "{}";

        mActivatingConfig = new Gson().fromJson(activating, ActivatingConfig.class);
    }

    private void saveActivatingConfig() {
        if(mActivatingConfig == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(mContext.getString(R.string.platform_has_activating_config), true);
        editor.putString(mContext.getString(R.string.platform_activating_config), new Gson().toJson(mActivatingConfig));
        editor.commit();
    }

    private void loadOrganization() {
        mHasOrganization = mSharedPref.getBoolean(
                mContext.getString(R.string.platform_has_organization),
                mContext.getString(R.string.platform_has_organization_default).equals("true"));
        if(!mHasOrganization)
            return;

        String organization = mSharedPref.getString(mContext.getString(R.string.platform_organization), null);
        if(organization == null) {
            mOrganization = MP100Application.loadSettingsFromTmpFile("org.json", Organization.class);
        } else {
            mOrganization = new Gson().fromJson(organization, Organization.class);
        }
        LogManager.i("loaded organization: \n" + new Gson().toJson(mOrganization));
    }

    private void saveOrganization() {
        if(mOrganization == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(mContext.getString(R.string.platform_has_organization), true);
        editor.putString(mContext.getString(R.string.platform_organization), new Gson().toJson(mOrganization));
        editor.commit();
    }

    private int tryConnect() {
        synchronized (mLock) {
            if (!mInited || !mAllowConnect || !mPlatformPostman.disconnected() || !mHasConnectionSettings)
                return -1;

            LogManager.i("try to connect to platform");
            if (mPlatformPostman.asyncConnect(mConnectionSettings.WebSocketURL, mConnectionSettings.Realm, this::onStateChanged) ==
                    BPError.ERROR_IMPORTANT_DELIVER_FAIL) {
                throw new RuntimeException("fail to asyncConnect platform");
            }
            return 0;
        }
    }

    private void onActivating(boolean success) {
        mActivating = success ? State.Done : State.None;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(mContext.getString(R.string.platform_had_activated), success);
        editor.commit();

        if(success && mBinding == State.Done && mHasOrganization)
            bind();
    }

    private void onStateChanged(int connected, List<Object> useless1, Map<String, Object> useless2) {
        if(connected == 0) {
            LogManager.w("has disconnected from platform");
            synchronized (mLock) {
                for(Observer ob: mObservers)
                    ob.onConnectingState(false);
            }
            if(!mAllowConnect) {
                synchronized (mLock) {
                    mLock.notifyAll();
                }
            } else if(mConnectionSettings.RetryConnectEnable && mCurrentRetryConnectTimes != 0) {
                if(mCurrentRetryConnectTimes > 0)
                    --mCurrentRetryConnectTimes;
                LogManager.w(String.format("retry connect to platform again %s",
                        (mCurrentRetryConnectTimes < 0 ? "forever" :
                                ", left " + mCurrentRetryConnectTimes + " times")));
                LogManager.w("TODO: the same thread will reentry the critical zone!!!");
                if(mPlatformPostman.asyncConnect(mConnectionSettings.WebSocketURL, mConnectionSettings.Realm, this::onStateChanged, mConnectionSettings.RetryConnectIntervalMS) ==
                        BPError.ERROR_IMPORTANT_DELIVER_FAIL) {
                    LogManager.e("!!! fail to retry connect to platform");
                }
            }
        } else {
            LogManager.i("successful to connect to the platform");
            LogManager.w("TODO: auth with the platform");
            synchronized (mLock) {
                for(Observer ob: mObservers)
                    ob.onConnectingState(true);
            }

            if(mActivating == State.None && mHasActivatingConfig) {
                activate();
            }
        }
    }

    private String getSettingsFromTmpFile(String filename) {
        try {
            String settings = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename))));
            String line = br.readLine();
            while(line != null) {
                settings += line;
                line = br.readLine();
            }
            br.close();
            return settings;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("not found " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("read from " + filename + " fail");
        }
    }

    private void startRtmpOutput(LessonInfo info, long timetable_id) throws InternalError {
        String url = mRtmpPrefix + info.program_uuid + "?s=" + info.stream_name;
        int id = RBUtil.getInstance().addOutput(url);
        if(id < 0)
            throw new InternalError(String.format("create rtmp[%s] output failed with %d", url, id));
        inClassLesson = new Tuple3<>(timetable_id, id, url);
    }
}
