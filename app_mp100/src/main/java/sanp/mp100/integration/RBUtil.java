package sanp.mp100.integration;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.SurfaceHolder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.MP100Application;
import sanp.mp100.R;
import sanp.mpx.mc.MediaController;
import sanp.mpx.mc.ScreenLayout.FillPattern;
import sanp.mpx.mc.ScreenLayout.LayoutMode;

/**
 * Created by Tuyj on 2017/10/28.
 */

public class RBUtil implements MediaController.Observer {

    public interface StateObserver {
        void onSourceAdded(int id, String url, List<Role> roles);
        void onSourceRemoved(int id, String url, List<Role> roles, int result);
        void onOutputAdded(int id, String ur);
        void onOutputRemoved(int id, String url, int result);
    }

    public interface StatisObserver {
        void onSourceResolution(int id, String url, int width, int height);
        void onSourceStatistics(int id, String url, float fps, int kbps);
        void onOutputStatistics(int id, String url, float fps, int kbps);
        void onVideoRendererStatistics(float fps, long droppedFrame);
    }

    private static RBUtil gRBUtil = null;

    public static RBUtil allocateInstance(Context context) {
        if (gRBUtil == null) {
            synchronized (RBUtil.class) {
                if (gRBUtil == null) {
                    gRBUtil = new RBUtil(context);
                }
            }
        }
        return gRBUtil;
    }

    public static RBUtil getInstance() {
        return gRBUtil;
    }

    private enum State {
        None,
        Doing,
        Done
    };

    public enum Role {
        // Teacher(0x00,"老师"),
        TeacherFullView(0x01, "老师全景"),
        TeacherFeature(0x02, "老师特写"),
        TeacherBlackboard(0x03, "板书"),

        // Student(0x10, "学生"),
        StudentFullView(0x11, "学生全景"),
        StudentFeature(0x12, "学生特写"),

        Courseware(0x20, "课件"),

        UNSPECIFIED(0xff, "未指定");

        private int value;
        private String dsp;
        private Role(int value, String dsp) {
            this.value = value;
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
        public int toValue() {
            return value;
        }
    }

    public enum Scene {
        Unspecified(0x00, "未定义"),

        Home(0x10, "主页"),
        ShowTimeTable(0x11, "展示课表"),

        InClass(0x20, "上课中"),

        Setting(0x30, "配置入口"),
        // SettingXX(0x31, "配置XX"),

        Others(0x50, "其他");

        private int value;
        private String dsp;
        private Scene(int value, String dsp) {
            this.value = value;
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
        public int toValue() {
            return value;
        }
    };

    public static class Content {
        public String name;
        public LayoutMode layout;
        public int subScreenCnt;
        public Map<Integer, List<Role>> roleList;
        public Content() {
            name = "";
            layout = LayoutMode.UNSPECIFIED;
            subScreenCnt = 0;
            roleList = new HashMap<>();
        }
        public Content(String name, LayoutMode layout, int subScreenCnt, Map<Integer, List<Role>> roleList) {
            this();
            this.name = name;
            this.layout = layout;
            this.subScreenCnt = subScreenCnt;
            this.roleList.putAll(roleList);
        }
    }

    public class Source {
        public String url;
        public FillPattern pattern;
        public List<Role> roles;

        public int id;
        public State state;

        public Source() {
            url = "";
            pattern = FillPattern.FILL_PATTERN_NONE;
            roles = new ArrayList<>();
            id = -1;
            state = State.None;
        }

        public Source(String url) {
            this();
            this.url = url;
        }

        public Source(String url, FillPattern pattern, List<Role> roles) {
            this();
            this.url = url;
            this.pattern = pattern;
            this.roles.addAll(roles);
        }

        public void reset() {
            id = -1;
            state = State.None;
        }
    }

    private Object mLock = new Object();
    private Context mContext = null;
    private SharedPreferences mSharedPref = null;
    private MediaController mMediaController = null;

    private List<StateObserver> mStateObservers = new ArrayList<>();
    private List<StatisObserver> mStatisObservers = new ArrayList<>();

    private Map<Scene, List<Content>> mScenes = null;
    private List<Source> mSources = null;

    private Scene mCurrentScene = Scene.Unspecified;

    private RBUtil(Context context) {
        mContext = context;

        mSharedPref = mContext.getSharedPreferences(mContext.getString(R.string.my_local_video_preferences), Context.MODE_PRIVATE);
        loadPreferences();

        mMediaController = MediaController.allocateInstance(mContext);
        mMediaController.addObserver(this);
    }

    public void release() {
        LogManager.d("RBUtil release in");
        if (mMediaController != null) {
            mMediaController.release();
            mMediaController = null;
        }
        LogManager.d("RBUtil release out");
    }

    public void init(SurfaceHolder holder) {
        mMediaController.init(holder);
        addSources();
    }

    public void changeSurface(SurfaceHolder holder, int format, int width, int height) {
        mMediaController.changeSurface(holder, format, width, height);
    }

    public void addStateObserver(StateObserver ob) {
        synchronized (mLock) {
            if (!mStateObservers.contains(ob))
                mStateObservers.add(ob);
        }
    }

    public void addStatisObserver(StatisObserver ob) {
        synchronized (mLock) {
            if (!mStatisObservers.contains(ob))
                mStatisObservers.add(ob);
        }
    }

    public void removeStateObserver(StateObserver ob) {
        synchronized (mLock) {
            if (mStateObservers.contains(ob))
                mStateObservers.remove(ob);
        }
    }

    public void removeStatisObserver(StatisObserver ob) {
        synchronized (mLock) {
            if (mStatisObservers.contains(ob))
                mStatisObservers.remove(ob);
        }
    }

    public void setScene(Scene scene) {

    }

    public int addSource(String url) {
        LogManager.d(String.format("RBUtil add source: url-%s", url));
        synchronized (mLock) {
            int id = mMediaController.addSource(url, MediaController.RECOMMENDED_REOPEN_CNT);
            if(id >= 0) {
                Source source = new Source(url);
                source.id = id;
                source.state = State.Doing;
                mSources.add(source);
                saveSources();
            }
            return id;
        }
    }

    public int addSource(String url, List<Role> roles) {
        LogManager.d(String.format("RBUtil add source: url-%s, with roles-%s", url, roles));
        synchronized (mLock) {
            int id = mMediaController.addSource(url, MediaController.RECOMMENDED_REOPEN_CNT);
            if(id >= 0) {
                Source source = new Source(url);
                source.id = id;
                source.state = State.Doing;
                source.roles.addAll(roles);
                mSources.add(source);
                saveSources();
            }
            return id;
        }
    }

    public int removeSource(int id) {
        synchronized (mLock) {
            Source source = getSourceById(id);
            if(source == null) {
                LogManager.w("can't find the source-" + id);
                return -1;
            }

            LogManager.i(String.format("RBUtil remove source: id-%d url-%s roles-%s", source.id, source.url, source.roles));
            mSources.remove(source);
            if (source.state == State.Done)
                mMediaController.removeSource(source.id);
            return 0;
        }
    }

    public int removeSource(String url) {
        // just find the first one which has the same url
        synchronized (mLock) {
            Source source = getSourceByUrl(url);
            if(source == null) {
                LogManager.w("can't find the source-" + url);
                return -1;
            }

            LogManager.i(String.format("RBUtil remove source: id-%d url-%s roles-%s", source.id, source.url, source.roles));
            mSources.remove(source);
            if (source.state == State.Done)
                mMediaController.removeSource(source.id);
            return 0;
        }
    }

    public int removeSource(Role role, boolean forced) {
        // just find the first one which has the same url
        synchronized (mLock) {
            Source source = getSourceByRole(role);
            if(source == null) {
                LogManager.w("can't find the source-" + role.toString());
                return -1;
            }
            if(source.roles.size() > 1) {
                if(forced) {
                    LogManager.w("force to remove the source-" + role.toString() + " which is shared with other role-" + source.roles);
                } else {
                    LogManager.w("can't remove the source-" + role.toString() + " as this source is used by other role-" + source.roles);
                    return -2;
                }
            }

            LogManager.i(String.format("RBUtil remove source: id-%d url-%s roles-%s", source.id, source.url, source.roles));
            mSources.remove(source);
            if (source.state == State.Done)
                mMediaController.removeSource(source.id);
            return 0;
        }
    }

    private void loadPreferences() {
        loadSources();
        loadScenes();
        loadTmpFile(); // TODO:

        // reset source id and state
        for(Source source: mSources)
            source.reset();

        LogManager.i("loaded sources: \n" + new Gson().toJson(mSources));
        LogManager.i("loaded scenes: \n" + new Gson().toJson(mScenes));
    }

    private void loadSources() {
        // load from local video preferences
        mSources = new ArrayList<>();
        String sourcesStr = mSharedPref.getString(mContext.getString(R.string.video_sources), null);
        if(sourcesStr != null) {
            Type type = new TypeToken<List<Source>>(){}.getType();
            mSources = new Gson().fromJson(sourcesStr, type);
        }
    }

    private void saveSources() {
        if(mSources == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(mContext.getString(R.string.video_sources), new Gson().toJson(mSources));
        editor.commit();
    }

    private void loadScenes() {
        mScenes = new HashMap<>();
        String scenesStr = mSharedPref.getString(mContext.getString(R.string.video_scenes), null);
        if(scenesStr != null) {
            Type type = new TypeToken<Map<Scene, List<Content>>>(){}.getType();
            mScenes = new Gson().fromJson(scenesStr, type);
        }
    }

    private void saveScenes() {
        if(mScenes == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(mContext.getString(R.string.video_scenes), new Gson().toJson(mScenes));
        editor.commit();
    }

    private void loadTmpFile() {
        if(mSources.size() == 0) {
            Type type = new TypeToken<List<Source>>(){}.getType();
            mSources = MP100Application.loadSettingsFromTmpFile("sources.json", type);
        }

        if(mScenes.size() == 0) {
            Type type = new TypeToken<Map<Scene, List<Content>>>(){}.getType();
            mScenes = MP100Application.loadSettingsFromTmpFile("scenes.json", type);
        }
    }

    private void addSources() {
        synchronized (mLock) {
            for (Source source : mSources) {
                LogManager.i(String.format("RBUtil initialize source: url-%s", source.url));
                int id = mMediaController.addSource(source.url, MediaController.RECOMMENDED_REOPEN_CNT);
                if (id >= 0) {
                    source.id = id;
                    source.state = State.Doing;
                }
            }
        }
    }

    private Source getSourceById(int id) {
        if(id < 0)
            return null;

        for(Source source: mSources) {
            if(source.id == id)
                return source;
        }
        return null;
    }

    private Source getSourceByRole(Role role) {
        if(role == Role.UNSPECIFIED)
            return null;

        for(Source source: mSources) {
            if(source.roles.contains(role))
                return source;
        }
        return null;
    }

    private Source getSourceByUrl(String url) {
        // just find the first one which has the same url
        if(url == null || url.isEmpty())
            return null;
        for(Source source: mSources) {
            if(source.url.equals(url))
                return source;
        }
        return null;
    }

    ///////////////////////// implementation of MediaController.Observer
    @Override
    public void onSourceAdded(int sourceId, String url, int result) {
        LogManager.i(String.format("source-%d(url=%s) added with result-%d", sourceId, url, result));
//        synchronized (mLock) {
//            if (result == 0) {
//                if (isRemoteSource(url)) {
//                    Integer trackId = mRemote.rxSources.get(sourceId);
//                    if (trackId == null) {
//                        LogManager.e("logical error: can't find the remote source id-" + sourceId + " url-" + url);
//                        return;
//                    }
//                    Tuple3<DataType, Integer/*sourceId*/, Boolean/*added*/> track = mRemote.rxTracks.get(trackId);
//                    if (track.third) {
//                        LogManager.e("logical error: the remote source had added " + sourceId + " url-" + url);
//                        return;
//                    }
//                    mRemote.rxTracks.put(trackId, new Tuple3<>(track.first, sourceId, true));
//                    if (track.first == DataType.VIDEO && mLocalShownRemote) {
//                        mMediaController.addSource2Scene(MediaController.SCENE_IDX_LOCAL_DISPLAY, sourceId);
//                        mMediaController.autoShow(MediaController.SCENE_IDX_LOCAL_DISPLAY, LayoutMode.SYMMETRICAL, FillPattern.FILL_PATTERN_ADAPTING);
//                    }
//                } else {
//                    // TODO: audio source
//                    Integer role = mLocalRoles.get(sourceId);
//                    if (role == null) {
//                        LogManager.e("logical error: can't find the local source id-" + sourceId + " url-" + url);
//                        return;
//                    }
//                    Tuple<Local, Boolean/*added*/> local = mLocals.get(role);
//                    if (local.second) {
//                        LogManager.e("logical error: the local source had added " + sourceId + " url-" + url);
//                        return;
//                    }
//                    mLocals.put(role, new Tuple<>(local.first, true));
//                    if (mLocalShownRemote) {
//                        mMediaController.addSource2Scene(MediaController.SCENE_IDX_SEND_TO_REMOTE, sourceId);
//                        mMediaController.autoShow(MediaController.SCENE_IDX_SEND_TO_REMOTE, LayoutMode.SYMMETRICAL, FillPattern.FILL_PATTERN_ADAPTING);
//                    } else {
//                        mMediaController.addSource2Scene(MediaController.SCENE_IDX_LOCAL_DISPLAY, sourceId);
//                        mMediaController.autoShow(MediaController.SCENE_IDX_LOCAL_DISPLAY, LayoutMode.SYMMETRICAL, FillPattern.FILL_PATTERN_ADAPTING);
//                    }
//                }
//            }
//        }
    }

    @Override
    public void onSourceLost(int sourceId, String url, int result) {
        LogManager.i(String.format("source-%d(url=%s) lost with result-%d", sourceId, url, result));
//        synchronized (mLock) {
//            if (isRemoteSource(url)) {
//                Integer trackId = mRemote.rxSources.get(sourceId);
//                if (trackId == null) {
//                    LogManager.e("logical error: can't find the remote source id-" + sourceId + " url-" + url);
//                    return;
//                }
//                Tuple3<DataType, Integer/*sourceId*/, Boolean/*added*/> track = mRemote.rxTracks.get(trackId);
//                if (track.third) {
//                    mRemote.rxTracks.put(trackId, new Tuple3<>(track.first, sourceId, false));
//                    if (track.first == DataType.VIDEO && mLocalShownRemote) {
//                        mMediaController.cleanSceneSource(MediaController.SCENE_IDX_LOCAL_DISPLAY, sourceId);
//                        mMediaController.autoShow(MediaController.SCENE_IDX_LOCAL_DISPLAY, LayoutMode.SYMMETRICAL, FillPattern.FILL_PATTERN_ADAPTING);
//                    }
//                }
//            } else {
//                Integer role = mLocalRoles.get(sourceId);
//                if (role == null) {
//                    LogManager.e("logical error: can't find the local source id-" + sourceId + " url-" + url);
//                    return;
//                }
//                Tuple<Local, Boolean/*added*/> local = mLocals.get(role);
//                if (local.second) {
//                    mLocals.put(role, new Tuple<>(local.first, false));
//                    if (mLocalShownRemote) {
//                        mMediaController.cleanSceneSource(MediaController.SCENE_IDX_SEND_TO_REMOTE, sourceId);
//                        mMediaController.autoShow(MediaController.SCENE_IDX_SEND_TO_REMOTE, LayoutMode.SYMMETRICAL, FillPattern.FILL_PATTERN_ADAPTING);
//                    } else {
//                        mMediaController.cleanSceneSource(MediaController.SCENE_IDX_LOCAL_DISPLAY, sourceId);
//                        mMediaController.autoShow(MediaController.SCENE_IDX_LOCAL_DISPLAY, LayoutMode.SYMMETRICAL, FillPattern.FILL_PATTERN_ADAPTING);
//                    }
//                }
//            }
//        }
    }

    @Override
    public void onSourceResolutionChanged(int id, int width, int height) {
        LogManager.i(String.format("source-%d width-%d height-%d", id, width, height));
    }

    @Override
    public void onSourceStatistics(int id, float fps, int kbps) {
        LogManager.i(String.format("source-%d fps-%.2f kbps-%d", id, fps, kbps));
    }

    @Override
    public void onOutputAdded(int id, String url, int result) {
        LogManager.i(String.format("output-%d(url=%s) added with result-%d", id, url, result));
    }

    @Override
    public void onOutputLost(int id, String url, int result) {
        LogManager.i(String.format("output-%d(url=%s) lost with result-%d", id, url, result));
    }

    @Override
    public void onOutputStatistics(int id, float fps, int kbps) {
        LogManager.i(String.format("output-%d statis: fps-%.2f %dkbps", id, fps, kbps));
    }

    @Override
    public void onVideoRendererStatistics(float fps, long droppedFrame) {
        LogManager.i(String.format("render fps: %.2f dropped: %d", fps, droppedFrame));
    }

}

