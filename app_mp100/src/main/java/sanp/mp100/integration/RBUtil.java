package sanp.mp100.integration;

import android.content.Context;
import android.media.MediaFormat;
import android.util.Pair;
import android.view.SurfaceHolder;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.Tuple;
import sanp.mpx.mc.MediaController;
import sanp.mpx.mc.ScreeLayout;
import sanp.mpx.mc.ScreeLayout.FillPattern;
import sanp.mpx.mc.ScreeLayout.LayoutMode;

/**
 * Created by Tuyj on 2017/10/28.
 */

public class RBUtil implements MediaController.Observer {

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

    public enum Role {
        Teacher(0x00,"老师"),
        TeacherFullView(0x00, "老师全景"),
        TeacherFeature(0x01, "老师特写"),
        TeacherBlackboard(0x02, "板书"),

        Student(0x10, "学生"),
        StudentFullView(0x10, "学生全景"),
        StudentFeature(0x11, "学生特写"),

        Courseware(0x20, "课件"),

        UNSPECIFIED(0xff, "未指定");

        public static Role fromValue(final int value) {
            final Map<Integer, Role> value2role = new HashMap<Integer, Role>() {{
                put(TeacherFullView.toValue(), TeacherFullView);
                put(TeacherFeature.toValue(), TeacherFeature);
                put(TeacherBlackboard.toValue(), TeacherBlackboard);
                put(StudentFullView.toValue(), StudentFullView);
                put(StudentFeature.toValue(), StudentFeature);
                put(Courseware.toValue(), Courseware);
            }};
            if(!value2role.containsKey(value))
                return UNSPECIFIED;
            return value2role.get(value);
        }

        public static Role fromString(String dsp) {
            final Map<String, Role> dsp2role = new HashMap<String, Role>() {{
                put(Teacher.toString(), Teacher);
                put(TeacherFullView.toString(), TeacherFullView);
                put(TeacherFeature.toString(), TeacherFeature);
                put(TeacherBlackboard.toString(), TeacherBlackboard);
                put(Student.toString(), Student);
                put(StudentFullView.toString(), StudentFullView);
                put(StudentFeature.toString(), StudentFeature);
                put(Courseware.toString(), Courseware);
            }};
            if(!dsp2role.containsKey(dsp))
                return UNSPECIFIED;
            return dsp2role.get(dsp);
        }

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

    public static class Content {
        public LayoutMode layout;
        public int subScreenCnt;
        public Map<Integer, List<Role>> roleList;
        public Content() {
            layout = LayoutMode.UNSPECIFIED;
            subScreenCnt = 0;
            roleList = new HashMap<>();
        }
        public Content(LayoutMode layout, int subScreenCnt, Map<Integer, List<Role>> roleList) {
            this.layout = layout;
            this.subScreenCnt = subScreenCnt;
            this.roleList.putAll(roleList);
        }
    }

    public class Source {
        public int id;
        public String url;
        public boolean added;
        public List<Role> roles;

        Source(int id, String url) {
            this.id = id;
            this.url = url;
            added = false;
            roles = new ArrayList<>();
        }
    }

    private MediaController mMediaController = null;

    private Object mLock = new Object();
    private Map<Integer, Source> mSources = new HashMap<>();
    private Map<Role, Integer> mRole2Source = new HashMap<>();

    private RBUtil(Context context) {
        mMediaController = MediaController.allocateInstance(context);
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
        /*
        mMediaController.setDisplayName(MediaController.SCENE_IDX_LOCAL_DISPLAY,
                "本地画面",
                MediaController.HORIZONTAL_HCENTER,
                MediaController.VERTICAL_TOP, "");
        mMediaController.displayName(MediaController.SCENE_IDX_LOCAL_DISPLAY, true);
        */
        loadSources();
        loadRoles();
    }

    public void changeSurface(SurfaceHolder holder, int format, int width, int height) {
        mMediaController.changeSurface(holder, format, width, height);
    }

    public int addSource(String url) {
        LogManager.d(String.format("RBUtil add source: url-%s", url));
        synchronized (mLock) {
            int id = mMediaController.addSource(url, MediaController.RECOMMENDED_REOPEN_CNT);
            if(id >= 0) {
                mSources.put(id, new Source(id, url));
            }
            return id;
        }
    }

    public int addSource(String url, List<Role> roles) {
        LogManager.d(String.format("RBUtil add source: url-%s, with roles-%s", url, roles));
        synchronized (mLock) {
            int id = mMediaController.addSource(url, MediaController.RECOMMENDED_REOPEN_CNT);
            if(id >= 0) {
                Source source = new Source(id, url);
                for(Role role: roles) {
                    mRole2Source.put(role, id);
                    source.roles.add(role);
                }
                mSources.put(id, source);
            }
            return id;
        }
    }

    public void removeSource(int id) {
        Source source = mSources.remove(id);
        if (source != null) {
            LogManager.d(String.format("RBUtil remove source: id-%d url-%s roles-%s", source.id, source.url, source.roles));
            for (Role role: source.roles)
                mRole2Source.remove(role);
            if (source.added)
                mMediaController.removeSource(source.id);
        }
    }

    private void loadSources() {

    }

    private void saveSources() {

    }

    private void loadRoles() {

    }

    private void saveRoles() {

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

