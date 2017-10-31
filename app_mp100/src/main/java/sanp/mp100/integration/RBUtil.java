package sanp.mp100.integration;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.SurfaceHolder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.media.base.AVDefines;
import sanp.mp100.MP100Application;
import sanp.mp100.R;
import sanp.mpx.mc.MediaController;
import sanp.mpx.mc.MediaEngine;
import sanp.mpx.mc.ScreenLayout;
import sanp.mpx.mc.ScreenLayout.FillPattern;
import sanp.mpx.mc.ScreenLayout.LayoutMode;

/**
 * Created by Tuyj on 2017/10/28.
 */

public class RBUtil implements MediaController.Observer {

    public interface StateObserver {
        void onSourceAdding(int id, String url, List<Role> roles, int result);
        void onSourceLost(int id, String url, List<Role> roles, int result);
        void onOutputAdding(int id, String ur, int result);
        void onOutputLost(int id, String url, int result);
    }

    public interface StatisObserver {
        void onSourceResolution(int id, String url, List<Role> roles, int width, int height);
        void onSourceStatistics(int id, String url, List<Role> roles, float fps, int kbps);
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

    public enum Quality {
        // Default(0, "默认"),
        High(1, "高"),
        Middle(2, "中"),
        Low(3, "低");

        private int value;
        private String dsp;
        private Quality(int value, String dsp) {
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

    public static class Content {
        public String name;
        public LayoutMode layout;
        public int subScreenCnt;
        public Map<Integer, List<Role>> roleCandidates;
        public Content() {
            name = "";
            layout = LayoutMode.UNSPECIFIED;
            subScreenCnt = 0;
            roleCandidates = new HashMap<>();
        }
        public Content(String name, LayoutMode layout, int subScreenCnt, Map<Integer, List<Role>> roleCandidates) {
            this();
            this.name = name;
            this.layout = layout;
            this.subScreenCnt = subScreenCnt;
            this.roleCandidates.putAll(roleCandidates);
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

    public class OutputFormat {
        public MediaEngine.VideoSinkConfig video;
        public MediaEngine.AudioSinkConfig audio;

        public OutputFormat() {
            video = null;
            audio = null;
        }

        public OutputFormat(MediaEngine.VideoSinkConfig video) {
            this.video = video;
            audio = null;
        }

        public OutputFormat(MediaEngine.AudioSinkConfig audio) {
            video = null;
            this.audio = audio;
        }

        public OutputFormat(MediaEngine.VideoSinkConfig video, MediaEngine.AudioSinkConfig audio) {
            this.video = video;
            this.audio = audio;
        }
    }

    private Object mLock = new Object();
    private Context mContext = null;
    private SharedPreferences mSharedPref = null;
    private MediaController mMediaController = null;

    private List<StateObserver> mStateObservers = new ArrayList<>();
    private List<StatisObserver> mStatisObservers = new ArrayList<>();

    private List<Source> mSources = null;
    private Map<Integer, String> mOutputs = new HashMap<>();

    private Map<Scene, List<Content>> mSupportingScenes = null;
    private Scene mCurrentScene = Scene.Unspecified;
    private Content mCurrentContent = null;
    private Map<Integer, Role> mCurrentContentRoles = new HashMap<>();

    private Quality mDefaultQuality = Quality.Middle;
    private Map<Quality, OutputFormat> mSupportingOutputFormats = null;

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

    public int setScene(Scene scene) {
        return setScene(scene, 0);
    }

    public int setScene(Scene scene, int contentIndex) {
        synchronized (mLock) {
            Content content = null;
            List<Content> contents = mSupportingScenes.get(scene);
            if(contents == null || contents.size() <= contentIndex) {
                if(contentIndex == 0) {
                    LogManager.w(String.format("Scene[%s] is not in supporting list, default to hide all source", scene.toString()));
                } else {
                    LogManager.e(String.format("can't find the scene[%s-%d]", scene.toString(), contentIndex));
                    return -1;
                }
            } else {
                content = contents.get(contentIndex);
            }
            setContent(content);
            mCurrentScene = scene;
            return 0;
        }
    }

    public int setScene(Scene scene, String contentName) {
        synchronized (mLock) {
            List<Content> contents = mSupportingScenes.get(scene);
            if(contents == null) {
                LogManager.e(String.format("can't find the scene[%s-%s]", scene.toString(), contentName));
                return -1;
            }
            for (Content content : contents) {
                if (content.name.equals(contentName)) {
                    setContent(content);
                    mCurrentScene = scene;
                    return 0;
                }
            }
        }
        LogManager.e(String.format("can't find scene[%s-%s]", scene.toString(), contentName));
        return -1;
    }

    public List<String> getSceneContentName(Scene scene) {
        synchronized (mLock) {
            List<Content> contents = mSupportingScenes.get(scene);
            if(contents == null) {
                LogManager.e(String.format("can't find the scene[%s]", scene.toString()));
                return null;
            }
            List<String> names = new ArrayList<>();
            for(Content content: contents)
                names.add(content.name);
            return names;
        }
    }

    public int addSource(String url) {
        LogManager.d(String.format("RBUtil add source: url-%s", url));
        synchronized (mLock) {
            int id = mMediaController.addSource(url, Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
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
            int id = mMediaController.addSource(url, Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
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

    public Map<Quality, OutputFormat> getSupportingOutputFormats() {
        return mSupportingOutputFormats;
    }

    public int addOutput(String url) {
        return addOutput(url, mDefaultQuality);
    }

    public int addOutput(String url, Quality quality) {
        OutputFormat format = mSupportingOutputFormats.get(quality);
        if(format == null) {
            LogManager.e("can't find the quality-" + quality.toString());
            return -1;
        }
        return addOutput(url, format);
    }

    public int addOutput(String url, OutputFormat format) {
        synchronized (mLock) {
            int ret = mMediaController.addOutput(url, format.video, format.audio, MediaController.RECOMMENDED_REOPEN_CNT);
            if (ret >= 0) {
                mOutputs.put(ret, url);
            }
            return ret;
        }
    }

    public int removeOutput(int id) {
        synchronized (mLock) {
            String url = mOutputs.remove(id);
            if(url == null) {
                LogManager.w("can't find the output-" + id);
                return -1;
            }

            LogManager.i(String.format("RBUtil remove output: id-%d url-%s", id, url));
            mMediaController.removeOutput(id);
            return 0;
        }
    }

    public int removeOutput(String url) {
        // just find the first one which has the same url
        int id = -1;
        synchronized (mLock) {
            for(Map.Entry<Integer, String> entry: mOutputs.entrySet()) {
                if(entry.getValue().equals(url)) {
                    id = entry.getKey();
                    break;
                }
            }
            if(id == -1) {
                LogManager.w("can't find the output-" + id);
                return -1;
            }
        }
        return removeOutput(id);
    }

    private void setContent(Content content) {
        if(content == null || content.subScreenCnt == 0) { // hide all source
            mMediaController.hideSceneSources();
            mMediaController.setLayout(LayoutMode.SYMMETRICAL, 1);
            mCurrentContent = content;
            mCurrentContentRoles.clear();
        } else {
            mMediaController.hideSceneSources();
            mMediaController.setLayout(content.layout, content.subScreenCnt);
            mCurrentContent = content;
            mCurrentContentRoles.clear();
            for(Map.Entry<Integer, List<Role>> entry: content.roleCandidates.entrySet()) {
                int subScreenIndex = entry.getKey();
                List<Role> candidates = entry.getValue();
                for(Role candidate: candidates) {
                    Source source = getSourceByRole(candidate);
                    if(source == null) // the candidate role is none
                        continue;
                    if(source.state != State.Done) // the candidate source has not been added or done with failure
                        continue;

                    // TODO: to check whether this source had been shown in other sub-screen(there is a hidden rule: a source just can be shown once)

                    // fill the sub-screen with this backupSource
                    String pos = ScreenLayout.getSubScreenPosition(content.layout, content.subScreenCnt, subScreenIndex);
                    mMediaController.setSourcePosition(source.id, source.pattern, pos);
                    mCurrentContentRoles.put(subScreenIndex, candidate);
                    break;
                }
            }
        }
    }

    private void addSources() {
        synchronized (mLock) {
            for (Source source : mSources) {
                LogManager.i(String.format("RBUtil initialize source: url-%s", source.url));
                int id = mMediaController.addSource(source.url, Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
                if (id >= 0) {
                    source.id = id;
                    source.state = State.Doing;
                }
            }
        }
    }

    private void loadPreferences() {
        loadSources();
        loadScenes();
        loadOutputFormats();
        loadTmpFile(); // TODO:

        // reset source id and state
        for(Source source: mSources)
            source.reset();

        LogManager.i("loaded sources: \n" + new Gson().toJson(mSources));
        LogManager.i("loaded scenes: \n" + new Gson().toJson(mSupportingScenes));
        LogManager.i("loaded output formats: \n" + new Gson().toJson(mSupportingOutputFormats));
    }

    private void loadSources() {
        // load from local video preferences
        String sourcesStr = mSharedPref.getString(mContext.getString(R.string.video_sources), null);
        if(sourcesStr != null) {
            Type type = new TypeToken<List<Source>>(){}.getType();
            mSources = new Gson().fromJson(sourcesStr, type);
        } else {
            mSources = null;
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
        String scenesStr = mSharedPref.getString(mContext.getString(R.string.video_scenes), null);
        if(scenesStr != null) {
            Type type = new TypeToken<Map<Scene, List<Content>>>(){}.getType();
            mSupportingScenes = new Gson().fromJson(scenesStr, type);
        } else {
            mSupportingScenes = null;
        }
    }

    private void saveScenes() {
        if(mSupportingScenes == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(mContext.getString(R.string.video_scenes), new Gson().toJson(mSupportingScenes));
        editor.commit();
    }

    private void loadOutputFormats() {
        String formats = mSharedPref.getString(mContext.getString(R.string.video_output_formats), null);
        if(formats != null) {
            Type type = new TypeToken<Map<Quality, OutputFormat>>(){}.getType();
            mSupportingOutputFormats = new Gson().fromJson(formats, type);
        } else {
            mSupportingOutputFormats = null;
        }

        String defaultQ = mSharedPref.getString(
                mContext.getString(R.string.video_output_default_quality),
                mContext.getString(R.string.video_output_default_quality_default));
        mDefaultQuality = new Gson().fromJson(defaultQ, Quality.class);
    }

    private void saveOutputFormats() {
        if(mSupportingOutputFormats == null)
            return;
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(mContext.getString(R.string.video_output_formats), new Gson().toJson(mSupportingOutputFormats));
        editor.commit();
    }

    private void loadTmpFile() {
        if(mSources == null) {
            Type type = new TypeToken<List<Source>>(){}.getType();
            mSources = MP100Application.loadSettingsFromTmpFile("sources.json", type);
            for(Source source: mSources) {
                if(source.url.startsWith(MP100Application.TMP_FILE_PREFIX))
                    source.url = MP100Application.EXTERNAL_STORAGE_DIRECTORY + source.url;
            }
        }

        if(mSupportingScenes == null) {
            Type type = new TypeToken<Map<Scene, List<Content>>>(){}.getType();
            mSupportingScenes = MP100Application.loadSettingsFromTmpFile("scenes.json", type);
        }

        if(mSupportingOutputFormats == null) {
            Type type = new TypeToken<Map<Quality, OutputFormat>>(){}.getType();
            mSupportingOutputFormats = MP100Application.loadSettingsFromTmpFile("output_formats.json", type);
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
        synchronized (mLock) {
            Source source = getSourceById(sourceId);
            if(source == null) {
                LogManager.w(String.format("source-%d is not in source list, it may have been removed, auto remove it from internal", sourceId));
                mMediaController.removeSource(sourceId);
                return;
            }

            source.state = (result == 0 ? State.Done : State.None);
            for(StateObserver ob: mStateObservers)
                ob.onSourceAdding(sourceId, url, source.roles, result);

            if (result == 0) {
                // TODO: audio source
                mMediaController.addSource2Scene(sourceId);

                // show this source if necessarily
                if(source.roles.size() == 0) // has not been bound any role
                    return;
                if(mCurrentContent == null) // has no content in current scene
                    return;
                for(Map.Entry<Integer, List<Role>> entry: mCurrentContent.roleCandidates.entrySet()) {
                    int subScreenIndex = entry.getKey();
                    List<Role> roleCandidates = entry.getValue();
                    Role curRoleInThisSubScreen = mCurrentContentRoles.get(subScreenIndex);
                    for(Role role: roleCandidates) {
                        if(source.roles.contains(role)) { // the role bound with this source is in the sub-screen
                            if(curRoleInThisSubScreen != null) { // the sub-screen has been filled with curRoleInThisSubScreen
                                int this_priority = roleCandidates.indexOf(role);
                                int cur_priority = roleCandidates.indexOf(curRoleInThisSubScreen);
                                if(this_priority == cur_priority) {
                                    throw new RuntimeException("logical error: this source had been add to this sub-screen or more than one source has the same role-" + role.toString());
                                } else if(this_priority > cur_priority) { // this source's role priority is lower than the current one
                                    return;
                                }
                            }

                            // fill the sub-screen with this source
                            String pos = ScreenLayout.getSubScreenPosition(mCurrentContent.layout, mCurrentContent.subScreenCnt, subScreenIndex);
                            mMediaController.setSourcePosition(sourceId, source.pattern, pos);
                            mCurrentContentRoles.put(subScreenIndex, role);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onSourceLost(int sourceId, String url, int result) {
        LogManager.i(String.format("source-%d(url=%s) lost with result-%d", sourceId, url, result));
        synchronized (mLock) {
            Source source = getSourceById(sourceId);
            if(source == null) {
                LogManager.w(String.format("source-%d is not in source list, it may have been removed, auto remove it from internal", sourceId));
                mMediaController.removeSource(sourceId);
                return;
            }

            source.state = State.None;
            mMediaController.cleanSceneSource(sourceId);
            for(StateObserver ob: mStateObservers)
                ob.onSourceLost(sourceId, url, source.roles, result);

            // replace this source with others if necessarily
            if(source.roles.size() == 0) // has not been bound any role
                return;
            if(mCurrentContent == null) // has no content in current scene
                return;

            List<Integer> subScreenIndexs = new ArrayList<>();
            subScreenIndexs.addAll(mCurrentContentRoles.keySet());
            for(int subScreenIndex: subScreenIndexs) { // search the roles which are shown on the screen
                Role role = mCurrentContentRoles.get(subScreenIndex);
                if(!source.roles.contains(role)) // the shown role is not bound with this source
                    continue;

                // find the candidate to replace this source
                boolean found = false;
                List<Role> roleCandidates = mCurrentContent.roleCandidates.get(subScreenIndex);
                for(Role candidate: roleCandidates) {
                    if(source.roles.contains(candidate))  // skip self
                        continue;
                    Source backupSource = getSourceByRole(candidate);
                    if(backupSource == null) // the candidate role is none
                        continue;
                    if(backupSource.state != State.Done) // the candidate source has not been added or done with failure
                        continue;

                    // TODO: to check whether this source had been shown in other sub-screen(there is a hidden rule: a source just can be shown once)

                    // fill the sub-screen with this backupSource
                    String pos = ScreenLayout.getSubScreenPosition(mCurrentContent.layout, mCurrentContent.subScreenCnt, subScreenIndex);
                    mMediaController.setSourcePosition(backupSource.id, backupSource.pattern, pos);
                    mCurrentContentRoles.put(subScreenIndex, candidate);
                    found = true;
                    break;
                }
                if(!found) {
                    mCurrentContentRoles.remove(subScreenIndex);
                }
            }
        }
    }

    @Override
    public void onOutputAdded(int id, String url, int result) {
        LogManager.i(String.format("output-%d(url=%s) added with result-%d", id, url, result));
        synchronized (mLock) {
            for(StateObserver ob: mStateObservers)
                ob.onOutputAdding(id, url, result);
        }
    }

    @Override
    public void onOutputLost(int id, String url, int result) {
        LogManager.i(String.format("output-%d(url=%s) lost with result-%d", id, url, result));
        synchronized (mLock) {
            for(StateObserver ob: mStateObservers)
                ob.onOutputLost(id, url, result);
        }
    }

    @Override
    public void onSourceResolutionChanged(int id, int width, int height) {
        LogManager.i(String.format("source-%d width-%d height-%d", id, width, height));
        synchronized (mLock) {
            Source source = getSourceById(id);
            if(source == null) {
                LogManager.w(String.format("source-%d is not in source list, it may have been removed", id));
                return;
            }
            for(StatisObserver ob: mStatisObservers)
                ob.onSourceResolution(id, source.url, source.roles, width, height);
        }
    }

    @Override
    public void onSourceStatistics(int id, float fps, int kbps) {
        LogManager.d(String.format("source-%d fps-%.2f kbps-%d", id, fps, kbps));
        synchronized (mLock) {
            Source source = getSourceById(id);
            if(source == null) {
                LogManager.w(String.format("source-%d is not in source list, it may have been removed", id));
                return;
            }
            for(StatisObserver ob: mStatisObservers)
                ob.onSourceStatistics(id, source.url, source.roles, fps, kbps);
        }
    }

    @Override
    public void onOutputStatistics(int id, float fps, int kbps) {
        LogManager.d(String.format("output-%d statis: fps-%.2f %dkbps", id, fps, kbps));
        synchronized (mLock) {
            String url = mOutputs.get(id);
            if(url == null) {
                LogManager.w(String.format("output-%d is not in output list, it may have been removed", id));
                return;
            }

            for(StatisObserver ob: mStatisObservers)
                ob.onOutputStatistics(id, url, fps, kbps);
        }
    }

    @Override
    public void onVideoRendererStatistics(float fps, long droppedFrame) {
        LogManager.d(String.format("render fps: %.2f dropped: %d", fps, droppedFrame));
        synchronized (mLock) {
            for(StatisObserver ob: mStatisObservers)
                ob.onVideoRendererStatistics(fps, droppedFrame);
        }
    }

}
