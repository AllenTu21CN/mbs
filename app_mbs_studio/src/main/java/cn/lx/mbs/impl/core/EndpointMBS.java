package cn.lx.mbs.impl.core;

import android.content.Context;
import android.os.Handler;
import android.view.Surface;

import com.google.gson.JsonObject;
import cn.lx.mbs.impl.Const;
import cn.lx.mbs.impl.core.impl.DirectorHelper;
import cn.lx.mbs.impl.core.impl.EPHelper;
import cn.lx.mbs.impl.core.impl.ExtHelper;
import cn.lx.mbs.impl.core.impl.P2PCalling;
import cn.lx.mbs.impl.core.impl.LocalDisplayHelper;
import cn.lx.mbs.impl.core.impl.LRHelper;
import cn.lx.mbs.impl.core.impl.Meeting;
import cn.lx.mbs.impl.core.impl.PreviewHelper;
import cn.lx.mbs.impl.core.structures.CCaller;
import cn.lx.mbs.impl.core.structures.CSource;
import cn.lx.mbs.impl.core.structures.EPListener;
import cn.lx.mbs.impl.core.structures.TSEvent;
import cn.lx.mbs.impl.core.structures.TSStream;
import cn.lx.mbs.impl.core.utils.EPX200SPHelper;
import cn.lx.mbs.impl.db.Terminal;
import cn.lx.mbs.impl.structures.AudioChannelId;
import cn.lx.mbs.impl.structures.AudioCodec;
import cn.lx.mbs.impl.structures.CallingParam;
import cn.lx.mbs.impl.structures.CStreamType;
import cn.lx.mbs.impl.structures.DirectingType;
import cn.lx.mbs.impl.structures.DirectingMode;
import cn.lx.mbs.impl.structures.DisplayId;
import cn.lx.mbs.impl.structures.EPAudioCapability;
import cn.lx.mbs.impl.structures.EPStatus;
import cn.lx.mbs.impl.structures.LRState2;
import cn.lx.mbs.impl.structures.ExtParam;
import cn.lx.mbs.impl.structures.TSLayout;
import cn.lx.mbs.impl.structures.EPState;
import cn.lx.mbs.impl.structures.EPVideoCapability;
import cn.lx.mbs.impl.structures.EPBaseConfig;
import cn.lx.mbs.impl.structures.LRTitleStyle;
import cn.lx.mbs.impl.structures.LRCodec;
import cn.lx.mbs.impl.structures.LRId;
import cn.lx.mbs.impl.structures.LRSSStrategy;
import cn.lx.mbs.impl.structures.LRState;
import cn.lx.mbs.impl.structures.MeetingParam;
import cn.lx.mbs.impl.structures.PTZConfig;
import cn.lx.mbs.impl.structures.RecFile;
import cn.lx.mbs.impl.structures.RoomNameStyle;
import cn.lx.mbs.impl.structures.Source;
import cn.lx.mbs.impl.structures.SourceId;
import cn.lx.mbs.impl.structures.TSMode;
import cn.lx.mbs.impl.structures.TSRole;
import cn.lx.mbs.impl.structures.TSUnit;
import cn.lx.mbs.impl.structures.TrackingConfig;
import cn.lx.mbs.impl.structures.DisplayLayout;
import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.NetInfo;
import com.sanbu.base.Result;
import com.sanbu.base.Runnable3;
import com.sanbu.base.State;
import com.sanbu.board.EmptyBoardSupportClient;
import com.sanbu.tools.AsyncResult;
import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.sanbu.avalon.endpoint3.ext.ExtEvent;
import cn.sanbu.avalon.endpoint3.ext.ExtJni;
import cn.sanbu.avalon.endpoint3.ext.camera.CameraDirective;
import cn.sanbu.avalon.endpoint3.ext.camera.ZoomAction;
import cn.sanbu.avalon.endpoint3.ext.camera.IxNCamera;
import cn.sanbu.avalon.endpoint3.Endpoint3;
import cn.sanbu.avalon.endpoint3.structures.AudioInputDevice;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.CallingUrl;
import cn.sanbu.avalon.endpoint3.structures.Resolution;
import cn.sanbu.avalon.endpoint3.structures.SourceType;
import cn.sanbu.avalon.endpoint3.structures.TransProtocol;
import cn.sanbu.avalon.endpoint3.structures.CallingStatistics;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.jni.EPDir;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.EPObjectType;
import cn.sanbu.avalon.endpoint3.structures.jni.MediaStatistics;
import cn.sanbu.avalon.endpoint3.structures.jni.RegConfig;
import cn.sanbu.avalon.endpoint3.ext.ppt.PPTMonitorServer;
import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;
import cn.sanbu.avalon.media.MediaJni;
import cn.sanbu.avalon.media.VideoEngine;
import cn.sanbu.vt6105.api.VT6105BoardSupportClient;

/*
* EndpointMBS: 因为EP3是更灵活的通用SDK，为降低应用层使用EP的复杂度,封装了EndpointMBS
* 功能描述:
*   . 根据MBS的业务场景，支持: 视频源角色管理、直播录制、单点呼叫、多点会议
*   . 封装了内部线程、配置读写、状态维护
*   . 除了初始化类接口和查询类接口, 其他操作类接口为非阻塞异步调用,且线程安全
*   . 没有事件概念
* */
public class EndpointMBS implements Endpoint3.Callback {

    private static final String TAG = EndpointMBS.class.getSimpleName();

    private static boolean gInited = false;

    // @brief 应用层需要实现的回调
    public interface Observer {
        // @brief 注册状态发生变化
        // @param result [OUT], 错误码,请根据EPErrors中转义错误信息
        void onRegisteringStateChanged(State state, CallingProtocol protocol);

        // @brief A new sip or h.323 calling is incoming
        void onIncomingCall(int call_id, CallingParam param);

        // @brief 互动状态变化
        void onStateChanged(EPState state);

        // @brief 录制结束
        void onRecordingFinished(String path);
    }

    public static void initEnv(Context context) {
        if (gInited)
            return;

        synchronized (EndpointMBS.class) {
            if (gInited)
                return;

            // init endpoint jni environment
            Endpoint3.initEnv(context, "ep3_android");

            // set hint for NoSignal and Loading before MediaJni.init
            VideoEngine.setNoSignalText("HDMI-IN No Signal");
            VideoEngine.setLoadingText("Loading");

            // init media engine jni environment
            MediaJni.initEnv(context, new EmptyBoardSupportClient());

            // init ext-device-control jni environment
            ExtJni.initEnv();

            gInited = true;
        }
    }

    private static final EndpointMBS g_instance = new EndpointMBS();

    public static final EndpointMBS getInstance() {
        return g_instance;
    }

    ////////// inited config and handlers

    private Endpoint3 mEndpoint;
    private ExtHelper mHelper;

    private TSMode mMode = TSMode.None;
    private EPX200SPHelper mSPHelper;
    private Handler mRTWorker;
    private List<Source> mSrcCandidates;
    private Observer mObserver;
    private cn.lx.mbs.impl.core.ExtHelper mExtHelper;

    private LRHelper mLRHelper;
    private LocalDisplayHelper mLDHelper;
    private EPHelper mEPHelper;
    private DirectorHelper mDIRHelper;
    private PreviewHelper mPreviewHelper;
    private IxNCamera mCamera;
    private PPTMonitorServer mPPTMonitor;

    private P2PCalling mP2P;
    private Meeting mMeeting;

    ////////// preferences

    // video
    private Map<TSRole, SourceId> mRoleBindings;
    // audio
    private Map<AudioChannelId, Float> mDefaultAudioVolumes;
    // directing
    private PTZConfig mPTZConfig;
    private TrackingConfig mTrackingConfig; // unused
    private boolean mDefaultTracking;

    ////////// status

    // base
    private EPState mEPState = EPState.Idle;
    private String mHDName = "";

    // directing
    private boolean mTracking;

    // loaded sources
    private Map<Integer, CSource> mSources;
    private Map<TSRole, CSource> mSources2;
    private CSource mMic;

    // idle directing
    private List<String> mIdleDIRValidRoles;
    private Map<DirectingType, DirectingMode> mIdleDIRModes;
    private Map<DirectingType, Object/*content*/> mIdleDIRContents;

    private EndpointMBS() {
        mEndpoint = Endpoint3.getInstance();
        initHelper();
    }

    /////////////////////////////// 初始化和反初始化

    // @brief 初始化EPX200;必须初始化后才能调用其他接口
    // @param mode [IN], 授权模式,将影响内部功能的初始化
    // @param sp [IN], 配置项存储句柄
    // @param nrtHandler [IN], 非实时工作队列
    // @param sources [IN], 可用信号源列表
    // @param cb [IN], 应用层回调
    // @return 0 is suc, or failed.
    public int init(TSMode mode, EPX200SPHelper spHelper, Handler rtWorker, Observer ob,
                    Context context, cn.lx.mbs.impl.core.ExtHelper extHelper) {
        initEnv(context);

        if (mRTWorker != null) {
            LogUtil.w(Const.TAG, TAG, "had inited");
            return 0;
        }

        // keep the fixed config and handlers
        mMode = mode;
        mSPHelper = spHelper;
        mRTWorker = rtWorker;
        mObserver = ob;
        mExtHelper = extHelper;

        mSrcCandidates = null;
        mSources = new LinkedHashMap<>();
        mSources2 = new LinkedHashMap<>();

        // init others if license granted
        if (mMode.granted) {

            // create other handlers
            mLRHelper = new LRHelper();
            mLDHelper = new LocalDisplayHelper();
            mEPHelper = new EPHelper();
            mDIRHelper = new DirectorHelper();
            mPreviewHelper = new PreviewHelper();
            // mPPTMonitor = new PPTMonitorServer();

            // load preferences from SP
            loadPreferences();

            mTracking = mDefaultTracking;
            mEPState = EPState.Idle;
            mHDName = "";

            // init EP helper
            int ret = mEPHelper.init(mEndpoint, mSPHelper, mRTWorker, this);
            if (ret != 0) {
                release();
                return ret;
            }

            // init LR helper
            ret = mLRHelper.init(mEndpoint, mSPHelper, mHelper, mRTWorker);
            if (ret != 0) {
                release();
                return ret;
            }

            // init LD helper
            ret = mLDHelper.init(mEndpoint, mSPHelper, mHelper, mRTWorker, context, this);
            if (ret != 0) {
                release();
                return ret;
            }

            // init directing helper
            ret = mDIRHelper.init(mSPHelper, mRTWorker, this, mHelper);
            if (ret != 0) {
                release();
                return ret;
            }

            // init preview helper
            ret = mPreviewHelper.init(mEndpoint, mHelper);
            if (ret != 0) {
                release();
                return ret;
            }

            if (false) {
                // init PPT monitor
                ret = mPPTMonitor.init(Const.PPT_LISTEN_PORT, Const.PPT_LISTEN_PROTOCOL, this);
                if (ret != 0) {
                    LogUtil.w(CoreUtils.TAG, TAG, "PPTMonitorServer init failed: " + ret + ", disable ppt tracking");
                    mPPTMonitor = null;
                }
            }

            // init and connect ixn camera
            initAndConnectIxNCamera();

            // load mic as source first
            asyncCall(() -> loadMIC());
        } else {
            LogUtil.e(CoreUtils.TAG, TAG, "未授权,将不能使用互动功能");
        }

        return 0;
    }

    // @brief 反初始化
    // @return 0 is suc, or failed.
    public int release() {

        // block to release all handlers
        if (mRTWorker != null) {
            AsyncResult asyncResult = new AsyncResult();
            mRTWorker.post(() -> {

                mEPState = EPState.Idle;

                stopPreview();

                switchLRStateImpl(LRId.LocalRecording, LRState.Stop);

                if (mP2P != null) {
                    mP2P.release();
                    mP2P = null;
                }

                if (mMeeting != null) {
                    mMeeting.release();
                    mMeeting = null;
                }

                if (mPreviewHelper != null) {
                    mPreviewHelper.release();
                }

                if (mLRHelper != null) {
                    mLRHelper.release();
                }

                if (mLDHelper != null) {
                    mLDHelper.release();
                }

                if (mDIRHelper != null) {
                    mDIRHelper.release();
                }

                if (mPPTMonitor != null) {
                    mPPTMonitor.release();
                }

                if (mCamera != null) {
                    mCamera.release();
                }

                unloadSources();

                if (mEPHelper != null) {
                    mEPHelper.release();
                }

                mLRHelper = null;
                mLDHelper = null;
                mEPHelper = null;
                mDIRHelper = null;
                mPreviewHelper = null;
                mPPTMonitor = null;
                mCamera = null;

                mMode = TSMode.None;
                mSPHelper = null;
                mRTWorker = null;
                mSrcCandidates = null;
                mObserver = null;
                mExtHelper = null;

                asyncResult.notify2(0);
            });

            int ret = (int) asyncResult.wait2(4000, -1);
            if (ret != 0)
                LogUtil.w(CoreUtils.TAG, TAG, "release timeout");
        }

        return 0;
    }

    // @brief 获取可用信号源
    public List<Source> getAvailableSources() {
        return mSrcCandidates == null ? Collections.emptyList() : mSrcCandidates;
    }

    // @brief 设置可用信号源
    public void setAvailableSources(List<Source> sources) {
        asyncCall("setAvailableSources", null, () -> {
            if (mSrcCandidates != null || mEPState != EPState.Idle)
                return genError("setAvailableSources", BaseError.ACTION_ILLEGAL,
                        "had added sources", "非法操作,已经添加过视频源");

            mSrcCandidates = sources;
            loadSources();

            // init and start idle directing
            initIdleDirecting();
            mDIRHelper.stop();
            mDIRHelper.start(mIdleDIRContents, mIdleDIRModes, mIdleDIRValidRoles);
            mDIRHelper.reFlushAllLayout();
            return Result.SUCCESS;
        });
    }

    // @brief 物理主显示创建时回调
    // @param id [IN] 物理显示ID(LocalMain/LocalSecond)
    // @param handle [IN] 物理显示surface
    // @return 0 is suc, or failed.
    public void onMainDisplayCreated(Surface handle) {
        asyncCall("onMainDisplayCreated", null,
                () -> mLDHelper.onDisplayCreated(true, handle));
    }

    // @brief 物理主显示格式改变时回调
    // @param id [IN] 物理显示ID(LocalMain/LocalSecond)
    // @return 0 is suc, or failed.
    public void onMainDisplayChanged(int format, int width, int height) {
        asyncCall("onMainDisplayChanged", null, () -> {
            Result result = mLDHelper.onDisplayChanged(true, format, width, height);
            if (result.isSuccessful())
                mDIRHelper.pushHWEvent(TSEvent.MainDisplayReady);
            return result;
        });
    }

    // @brief 物理主显示销毁时回调
    // @param id [IN] 物理显示ID(LocalMain/LocalSecond)
    // @return 0 is suc, or failed.
    public void onMainDisplayDestroyed() {
        LogUtil.d(Const.TAG, "onMainDisplayDestroyed");
        asyncCall("onMainDisplayDestroyed", null, () -> {
            mDIRHelper.pushHWEvent(TSEvent.MainDisplayUnable);
            return mLDHelper.onDisplayDestroyed(true);
        });
    }

    public boolean isInited() {
        return isReady();
    }

    /////////////////////////////// 视频设置接口

    // @brief 获取角色绑定的源ID
    public Map<TSRole, SourceId> getRoleBoundSourceIds() {
        if (isReady())
            return mRoleBindings;
        else
            return new HashMap<>();
    }

    // @brief 获取角色绑定的源
    public Map<TSRole, Source> getRoleBoundSources() {
        Map<TSRole, Source> result = new LinkedHashMap<>();
        if (mSrcCandidates == null)
            return result;

        if (isReady()) {
            for (Map.Entry<TSRole, SourceId> entry : mRoleBindings.entrySet()) {
                for (Source source: mSrcCandidates) {
                    if (source.id == entry.getValue()) {
                        result.put(entry.getKey(), source);
                        break;
                    }
                }
            }
        }

        return result;
    }

    // @brief 获取已绑定的角色
    public List<TSRole> getBoundRoles() {
        if (isReady())
            return new ArrayList<>(mRoleBindings.keySet());
        else
            return Collections.emptyList();
    }

    // @brief 设置角色绑定(重新初始化后生效)
    public void setRoleBindings(Map<TSRole, SourceId> bindings, Callback callback) {
        asyncCall("setRoleBindings", callback, () -> {
            if (bindings.equals(mRoleBindings))
                return Result.SUCCESS;

            logAction("setRoleBindings", bindings);

            mRoleBindings = bindings;
            mSPHelper.setRoleBindings(mRoleBindings);
            return Result.SUCCESS;
        });
    }

    // @brief 获取双屏异显开关
    public boolean getExtendedDisplaySwitch() {
        if (isReady())
            return mLDHelper.isEnabledExtended();
        else
            return Const.DEFAULT_EXTENDED_DISPLAY_SWITCH;
    }

    // @brief 设置双屏异显开关(重新初始化后生效)
    public void setExtendedDisplaySwitch(boolean onOff, Callback callback) {
        asyncCall("setExtendedDisplaySwitch", callback, () -> mLDHelper.setEnabledExtended(onOff));
    }

    /////////////////////////////// 音频设置接口

    // @brief 获取音频指定通道的默认音量
    public float getAudioDefaultVolume(AudioChannelId id) {
        if (isReady())
            return mDefaultAudioVolumes.get(id);
        else
            return Const.DEFAULT_AUDIO_VOLUMES.get(id);
    }

    // @brief 设置音频指定通道的默认音量(重启后/下次生效)
    public void setAudioDefaultVolume(AudioChannelId id, float volume, Callback callback) {
        asyncCall("setAudioDefaultVolume", callback, () -> {
            if (mDefaultAudioVolumes.get(id) == volume)
                return Result.SUCCESS;

            logAction("setAudioDefaultVolume", id, volume);

            mDefaultAudioVolumes.put(id, volume);
            mSPHelper.setAudioDefaultVolumes(mDefaultAudioVolumes);
            return Result.SUCCESS;
        });
    }

    /////////////////////////////// 导播设置接口

    // @brief 获取默认导播模式
    public DirectingMode getDefaultDirectingMode(DirectingType type) {
        if (isReady())
            return mDIRHelper.getDefaultMode(type);
        else
            return Const.DEFAULT_DIRECTING_MODE.get(type);
    }

    // @brief 设置默认导播模式(下次生效)
    public void setDefaultDirectingMode(DirectingType type, DirectingMode mode, Callback callback) {
        asyncCall("setDefaultDirectingMode", callback, () -> mDIRHelper.setDefaultMode(type, mode));
    }

    // @brief 获取默认跟踪开关
    public boolean getDefaultTrackingSwitch() {
        return mDefaultTracking;
    }

    // @brief 设置默认跟踪开关(空闲时立即生效/互动下次生效)
    public void setDefaultTrackingSwitch(boolean onOff, Callback callback) {
        asyncCall("setDefaultTrackingSwitch", callback, () -> {
            if (mDefaultTracking == onOff)
                return Result.SUCCESS;

            logAction("setDefaultTrackingSwitch", onOff);
            mDefaultTracking = onOff;
            mSPHelper.setDefaultTrackingSwitch(mDefaultTracking);

            if (mEPState == EPState.Idle)
                setCurrentTrackingSwitch(mDefaultTracking);

            return Result.SUCCESS;
        });
    }

    // @brief 获取默认课件监测开关
    public boolean getDefaultCoursewareCheckingSwitch() {
        return Const.DEFAULT_COURSEWARE_CHECKING_SWITCH;
    }

    // @brief 设置默认课件监测开关(下次生效)
    public void setDefaultCoursewareCheckingSwitch(boolean onOff, Callback callback) {
        asyncCall("setDefaultCoursewareCheckingSwitch", callback, () -> new Result(BaseError.ACTION_UNSUPPORTED, "暂不支持"));
    }

    // @brief 获取云台配置
    public PTZConfig getPTZConfig() {
        if (isReady())
            return mPTZConfig;
        else
            return Const.EMPTY_PTZ_CONFIG;
    }

    // @brief 设置云台配置(重新初始化后生效)
    public void setPTZConfig(PTZConfig config, Callback callback) {
        asyncCall("setPTZConfig", callback, () -> {
            if (mPTZConfig.isEqual(config))
                return Result.SUCCESS;

            logAction("setPTZConfig", config);
            mPTZConfig = config;
            mSPHelper.setPTZConfig(mPTZConfig);
            return Result.SUCCESS;
        });
    }

    // @brief 获取跟踪配置
    public TrackingConfig getTrackingConfig() {
        return Const.EMPTY_TRACKING_CONFIG;
    }

    // @brief 设置跟踪配置(重新初始化后生效)
    public void setTrackingConfig(TrackingConfig config, Callback callback) {
        CoreUtils.callbackResult(mRTWorker, callback, new Result(BaseError.ACTION_UNSUPPORTED, "暂不支持"));
    }

    /////////////////////////////// 直播录制配置接口

    // @brief 获取直播录制默认编码参数
    public LRCodec getDefaultLRCodec() {
        if (isReady())
            return mLRHelper.getDefaultLRCodec();
        else
            return Const.DEFAULT_LR_CODEC;
    }

    // @brief 设置直播录制默认编码参数(重新初始化后生效)
    public void setDefaultLRCodec(LRCodec codec, Callback callback) {
        asyncCall("setDefaultLRCodec", callback, () -> mLRHelper.setDefaultLRCodec(codec));
    }

    // @brief 获取直播录制默认标题参数
    public LRTitleStyle getDefaultLRTitleStyle() {
        if (isReady())
            return mLRHelper.getDefaultLRTitleStyle();
        else
            return Const.DEFAULT_LR_TITLE_STYLE;
    }

    // @brief 设置直播录制默认标题参数(下次生效)
    public void setDefaultLRTitleStyle(LRTitleStyle style, Callback callback) {
        asyncCall("setDefaultLRTitleStyle", callback, () -> mLRHelper.setDefaultLRTitleStyle(style));
    }

    // @brief 获取录制文件属性
    public RecFile getRecordingFile() {
        if (isReady())
            return mLRHelper.getDefaultRecFile();
        else
            return Const.DEFAULT_RECORDING_FILE;
    }

    // @brief 设置录制文件属性(下次生效)
    public void setRecordingFile(RecFile file, Callback callback) {
        asyncCall("setRecordingFile", callback, () -> mLRHelper.setDefaultRecFile(file));
    }

    // @brief 获取直播录制地址
    public String getLRUrl(LRId id) {
        if (isReady())
            return mLRHelper.getLRUrl(id);
        else
            return "";
    }

    // @brief 获取(可用的)直播地址
    public String getRunningLiveUrl() {
        if (!isReady())
            return "";
        if (getLRState(LRId.LocalLive) == LRState.Stop)
            return "";
        return mLRHelper.getLRUrl(LRId.LocalLive);
    }

    // @brief 设置录制文件名(下次生效)
    public void setRecordingFilename(String filename, Callback callback) {
        asyncCall("setRecordingFilename", callback, () -> mLRHelper.setRecordingFilename(filename));
    }

    // @brief 设置集成推流地址(立即生效)
    public void setIntegratedPushingUrl(String url, Callback callback) {
        asyncCall("setIntegratedPushingUrl", callback, () -> mLRHelper.setIntegratedPushingUrl(url));
    }

    // @brief 设置固定推流地址(下次生效)
    public void setDefaultFixedUrl(String url, Callback callback) {
        asyncCall("setDefaultFixedUrl", callback, () -> mLRHelper.setDefaultFixedUrl(url));
    }

    // @brief 获取直播录制启停策略
    public LRSSStrategy getLRSSStrategy(LRId id) {
        if (isReady())
            return mLRHelper.getLRSSStrategy(id);
        else
            return Const.DEFAULT_LR_SS_STRATEGIES.get(id);
    }

    // @brief 设置直播录制启停策略(重新初始化后生效)
    public void setLRSSStrategy(LRId id, LRSSStrategy strategy, Callback callback) {
        asyncCall("setLRSSStrategy", callback, () -> mLRHelper.setLRSSStrategy(id, strategy));
    }

    /////////////////////////////// 呼叫设置接口

    // @brief 获取会场名默认配置
    public RoomNameStyle getDefaultRoomNameStyle() {
        if (isReady())
            return mEPHelper.getDefaultRoomNameStyle();
        else
            return Const.DEFAULT_ROOM_NAME_STYLE;
    }

    // @brief 设置会场名默认配置(下次生效)
    public void setDefaultRoomNameStyle(RoomNameStyle style, Callback callback) {
        asyncCall("setDefaultRoomNameStyle", callback, () -> mEPHelper.setDefaultRoomNameStyle(style));
    }

    // @brief 获取呼叫流类型绑定
    public Map<CStreamType, TSRole> getCStreamBindings() {
        TSRole ext = isReady() ? mEPHelper.getDefaultVideoExt() : Const.DEFAULT_VIDEO_EXT_ROLE;
        return new HashMap() {{
            put(CStreamType.VideoExt, ext);
        }};
    }

    // @brief 设置呼叫流类型绑定(下次生效)
    public void setCStreamBindings(Map<CStreamType, TSRole> bindings, Callback callback) {
        TSRole ext = bindings.get(CStreamType.VideoExt);
        asyncCall("setCStreamBindings", callback, () -> {
            if (ext == null)
                return Result.SUCCESS;
            return mEPHelper.setDefaultVideoExt(ext);
        });
    }

    // @brief 获取呼叫基础配置
    public final EPBaseConfig getEPBaseConfig() {
        if (isReady())
            return mEPHelper.getEPBaseConfig();
        else
            return Const.DEFAULT_EP_BASE_CONFIG;
    }

    // @brief 设置呼叫基础配置(下次生效)
    public void setEPBaseConfig(EPBaseConfig config, Callback callback) {
        asyncCall("setEPBaseConfig", callback, () -> mEPHelper.setEPBaseConfig(config));
    }

    // @brief 获取呼叫固定配置
    public final EPFixedConfig getEPFixedConfig() {
        if (isReady())
            return mEPHelper.getEPFixedConfig();
        else
            return Const.DEFAULT_EP_FIXED_CONFIG;
    }

    // @brief 设置呼叫固定配置(重新初始化后生效)
    public void setEPFixedConfig(EPFixedConfig config, Callback callback) {
        asyncCall("setEPFixedConfig", callback, () -> mEPHelper.setEPFixedConfig(config));
    }

    // @brief 获取呼叫视频能力
    public final EPVideoCapability getEPVideoCapability() {
        if (isReady())
            return mEPHelper.getEPVideoCapability();
        else
            return Const.DEFAULT_EP_VIDEO_CAPABILITY;
    }

    // @brief 设置呼叫视频能力(对称,上下行一致)(下次生效)
    public void setEPVideoCapability(EPVideoCapability capability, Callback callback) {
        asyncCall("setEPVideoCapability", callback, () -> mEPHelper.setEPVideoCapability(capability));
    }

    // @brief 获取呼叫音频能力
    public final EPAudioCapability getEPAudioCapability() {
        if (isReady())
            return mEPHelper.getEPAudioCapability();
        else
            return Const.DEFAULT_EP_AUDIO_CAPABILITY;
    }

    // @brief 设置呼叫音频能力(下次生效)
    public void setEPAudioCapability(EPAudioCapability capability, Callback callback) {
        asyncCall("setEPAudioCapability", callback, () -> mEPHelper.setEPAudioCapability(capability));
    }

    // @brief 获取呼叫支持的所有音频格式
    public List<AudioCodec> getSupportingCallingAudioCodecs() {
        if (isReady())
            return mEPHelper.getSupportingCallingAudioCodecs();
        else
            return Const.DEFAULT_EP_AUDIO_CAPABILITY.codecs;
    }

    /////////////////////////////// 注册接口

    // @brief 获取GK注册状态
    // @return None:未注册 Done:已注册 Doing:注册中
    public final State getGKRegState() {
        if (isReady())
            return mEPHelper.getGKRegState();
        else
            return State.None;
    }

    // @brief 获取GK注册配置
    public final RegConfig.H323 getGKRegConfig() {
        if (isReady())
            return mEPHelper.getGKRegConfig();
        else
            return Const.EMPTY_GK_REG_CONFIG;
    }

    // @brief GK注册
    public void registerGK(RegConfig.H323 config, Callback callback) {
        asyncCall("registerGK", callback, () -> mEPHelper.registerGK(config));
    }

    // @brief GK注销
    public void unregisterGK(Callback callback) {
        asyncCall("unregisterGK", callback, () -> {
            Result result = mEPHelper.unregisterGK();
            if (result.isSuccessful() && mObserver != null)
                mObserver.onRegisteringStateChanged(State.None, CallingProtocol.H323);
            return result;
        });
    }

    // @brief 获取SIP注册状态
    // @return None:未注册 Done:已注册 Doing:注册中
    public final State getSIPRegState() {
        if (isReady())
            return mEPHelper.getSIPRegState();
        else
            return State.None;
    }

    // @brief 获取SIP注册配置
    public final RegConfig.SIP getSIPRegConfig() {
        if (isReady())
            return mEPHelper.getSIPRegConfig();
        else
            return Const.EMPTY_SIP_REG_CONFIG;
    }

    // @brief SIP注册
    public void registerSIP(RegConfig.SIP config, Callback callback) {
        asyncCall("registerSIP", callback, () -> mEPHelper.registerSIP(config));
    }

    // @brief SIP注销
    public void unregisterSIP(Callback callback) {
        asyncCall("unregisterSIP", callback, () -> {
            Result result = mEPHelper.unregisterSIP();
            if (result.isSuccessful() && mObserver != null)
                mObserver.onRegisteringStateChanged(State.None, CallingProtocol.SIP);
            return result;
        });
    }

    /////////////////////////////// 互动控制通用接口

    // @brief 获取当前互动完整状态
    public EPStatus getEPStatus() {
        if (mEPState == null || mEPState == EPState.Idle)
            return EPStatus.buildIdle();

        EPStatus status = new EPStatus();
        status.epState = mEPState;
        status.epDurationMS = getEPDurationMS();
        status.epInDiscussion = inMeetingDiscussion();
        status.gkState = getGKRegState();
        status.sipState = getSIPRegState();
        status.trackingOnOff = getCurrentTrackingSwitch();
        status.lrState = new HashMap<>(2);
        status.dirModes = new HashMap<>(3);

        final LRId ids[] = {LRId.LocalRecording, LRId.LiveAndPushing};
        final DirectingType types[] = {DirectingType.Local, DirectingType.TX, DirectingType.LR};
        for (LRId id: ids)
            status.lrState.put(id, getLRState2(id));
        for (DirectingType type: types)
            status.dirModes.put(type, getCurrentDirectingMode(type));

        return status;
    }

    // @brief 获取当前互动状态
    public EPState getEPState() {
        return mEPState;
    }

    // @brief 获取当前互动持续时间
    public long getEPDurationMS() {
        if (mEPState == EPState.Idle)
            return 0;
        else if (mEPState == EPState.InMeeting)
            return mMeeting.getDurationMS();
        else
            return mP2P.getDurationMS();
    }

    // @brief 获取当前互动的可控制单元(本地/远程)
    public void getTSUnits(Callback/*List<TSUnit>*/ callback) {
        asyncCall("getTSUnits", callback, () -> {
            if (mEPState == EPState.Idle) {
                return Result.buildSuccess(Arrays.asList());
            } else if (mEPState == EPState.InMeeting) {
                return Result.buildSuccess(mMeeting.getUnits());
            } else {
                return Result.buildSuccess(mP2P.getUnits());
            }
        });
    }

    // @brief 获取当前互动的可控制单元(远程)
    public void getRemoteUnits(Callback/*List<TSUnit>*/ callback) {
        asyncCall("getRemoteUnits", callback, () -> {
            if (mEPState == EPState.Idle) {
                return Result.buildSuccess(Arrays.asList());
            } else if (mEPState == EPState.InMeeting) {
                return Result.buildSuccess(mMeeting.getRemoteUnits());
            } else {
                return Result.buildSuccess(mP2P.getRemoteUnits());
            }
        });
    }

    // @brief 获取指定显示的默认布局
    public void getDefaultLayout(DisplayId id, TSLayout layout, Callback/*List<TSUnit>*/ callback) {
        asyncCall("getDefaultLayout", callback, () -> {
            List<TSUnit> units;

            if (mEPState == EPState.Idle) {
                units = Collections.emptyList();
            } else {
                DisplayLayout detail = mDIRHelper.genExpectedLayout(id, layout);
                if (mEPState == EPState.InMeeting) {
                    units = mMeeting.transLayout2Unit(detail);
                } else {
                    units = mP2P.transLayout2Unit(detail);
                }
            }

            int count = layout.getLayoutDesc().size();
            int left = count - units.size();
            if (left > 0) {
                units = new LinkedList<>(units);
                for (int i = 0 ; i < left ; ++i)
                    units.add(TSUnit.buildEmpty());
            }

            return Result.buildSuccess(units);
        });
    }

    // @brief 获取当前导播模式
    public DirectingMode getCurrentDirectingMode(DirectingType type) {
        if (isReady())
            return mDIRHelper.getMode(type);
        else
            return DirectingMode.Manual;
    }

    // @brief 设置当前导播模式
    public void setCurrentDirectingMode(DirectingType type, DirectingMode mode, Callback callback) {
        asyncCall("setCurrentDirectingMode", callback, () -> mDIRHelper.setMode(type, mode));
    }

    // @brief 获取当前跟踪开关
    public boolean getCurrentTrackingSwitch() {
        if (isReady())
            return mTracking;
        else
            return mDefaultTracking;
    }

    // @brief 设置当前跟踪开关
    public void setCurrentTrackingSwitch(boolean onOff, Callback callback) {
        asyncCall("setCurrentDirectingMode", callback, () -> setCurrentTrackingSwitch(onOff));
    }

    private Result setCurrentTrackingSwitch(boolean onOff) {
        if (mTracking == onOff)
            return Result.SUCCESS;

        mTracking = onOff;
        if (mCamera != null) {
            mCamera.setTrackMode(mPTZConfig.viscaIdOfTeacher, mTracking);
            mCamera.setTrackMode(mPTZConfig.viscaIdOfStudent, mTracking);
        }
        return Result.SUCCESS;
    }

    // @brief 获取直播录制状态
    public LRState getLRState(LRId id) {
        if (isReady())
            return mLRHelper.getLRState(id);
        else
            return LRState.Stop;
    }

    // @brief 获取直播录制状态2(状态和持续时间)
    public LRState2 getLRState2(LRId id) {
        if (isReady())
            return mLRHelper.getLRState2(id);
        else
            return new LRState2(LRState.Stop, 0);
    }

    // @brief 切换直播录制状态
    public void switchLRState(LRId id, LRState state, Callback callback) {
        asyncCall("switchLRState", callback, () -> switchLRStateImpl(id, state));
    }

    // @brief 获取显示内容
    public DisplayLayout getDisplayLayout(DisplayId id) {
        if (!isReady())
            return DisplayLayout.buildEmpty();

        switch (id) {
            case LocalMain:
                return mLDHelper.getLDLayout(true);
            case LocalSecond:
                return mLDHelper.getLDLayout(false);
            case LR:
                return mLRHelper.getLRLayout();
            case TX:
                if (mEPState == EPState.Idle) {
                    return DisplayLayout.buildEmpty();
                } else if (mEPState == EPState.InMeeting) {
                    return mMeeting.getTXMainLayout();
                } else {
                    return mP2P.getTXMainLayout();
                }
            default:
                return DisplayLayout.buildEmpty();
        }
    }

    // @brief 设置显示内容
    public void setDisplayLayout(DisplayId id, DisplayLayout layout, Callback callback) {
        setDisplayLayout(id, layout, true, callback);
    }

    private void setDisplayLayout(DisplayId id, DisplayLayout layout, boolean checkMode, Callback callback) {
        asyncCall("setDisplayLayout", callback, () -> {
            if (checkMode) {
                DirectingType type = CoreUtils.transDisplayId2DIRType(id);
                DirectingMode mode = mDIRHelper.getMode(type);
                if (mode == DirectingMode.FullAuto) {
                    return genError("setDisplayLayout", BaseError.ACTION_ILLEGAL,
                            "display#" + id.name() + " is in " + mode.name() + " mode, not allow to set display by manually",
                            id.desc + "处于" + mode.name + "模式, 不允许手动设置布局");
                } else if (mode == DirectingMode.SemiAuto) {
                    DisplayLayout current = getDisplayLayout(id);
                    if (!CoreUtils.isLayoutContained(layout, current))
                        return genError("setDisplayLayout", BaseError.ACTION_ILLEGAL,
                                "display#" + id.name() + " is in " + mode.name() + " mode, not allow to set display content",
                                id.desc + "处于" + mode.name + "模式, 只能设置分屏数, 不允许设置布局内容");
                }
            }

            switch (id) {
                case LocalMain:
                    return mLDHelper.setLDLayout(true, layout);
                case LocalSecond:
                    return mLDHelper.setLDLayout(false, layout);
                case LR:
                    return mLRHelper.setLRLayout(layout);
                case TX:
                    if (mEPState == EPState.Idle) {
                        return Result.SUCCESS;
                    } else if (mEPState == EPState.InMeeting) {
                        return mMeeting.setTXMainLayout(layout);
                    } else {
                        return mP2P.setTXMainLayout(layout);
                    }
                default:
                    return genError("setDisplayLayout", BaseError.INVALID_PARAM, "invalid display id: " + id.name(), "无效得参数: " + id.desc);
            }
        });
    }

    // @brief 获取显示的预览地址
    public String getDisplayPreviewUrl(DisplayId id) {
        if (isReady())
            return mPreviewHelper.getDisplayPreviewUrl(id);
        else
            return "";
    }

    // @brief 获取指定音频通道的静音状态
    // @param extId [IN] extra id for caller
    public boolean getAudioCurrentMute(AudioChannelId id, int extId) {
        if (!isReady())
            return true;

        switch(id) {
            case Mic:
                // TODO: 本地source的音频stream？
                return mMic == null ? true : mMic.micMuted;
            case Speaker:
                return mLDHelper.isMute();
            case LR:
                return mLRHelper.isMute();
            case CallingRx:
                if (mEPState == EPState.Idle) {
                    return true;
                } else if (mEPState == EPState.InMeeting) {
                    return mMeeting.isRemoteMicMuted(extId);
                } else {
                    return mP2P.isRemoteMicMuted(extId);
                }
            case CallingTx:
                if (mEPState == EPState.Idle) {
                    return true;
                } else if (mEPState == EPState.InMeeting) {
                    return mMeeting.isRemoteSpeakerMuted(extId);
                } else {
                    return mP2P.isRemoteSpeakerMuted(extId);
                }
            default:
                return true;
        }
    }

    // @brief 设置指定音频通道的静音状态
    // @param extId [IN] extra id for caller
    public void setAudioCurrentMute(AudioChannelId id, int extId, boolean onOff, Callback callback) {
        asyncCall("setAudioCurrentMute", callback, () -> {
            switch (id) {
                case Mic:
                    // TODO: 本地source的音频stream？
                    if (mMic == null || mMic.micMuted == onOff)
                        return Result.SUCCESS;
                    mMic.micMuted = onOff;
                    onAudioChanged(TSRole.Mic, mMic.epId);
                    return Result.SUCCESS;
                case Speaker:
                    return mLDHelper.switchMute(onOff);
                case LR:
                    return mLRHelper.switchMute(onOff);
                case CallingRx:
                    if (mEPState == EPState.Idle) {
                        return Result.SUCCESS;
                    } else if (mEPState == EPState.InMeeting) {
                        return mMeeting.muteRemoteMic(extId, onOff);
                    } else {
                        return mP2P.muteRemoteMic(extId, onOff);
                    }
                case CallingTx:
                    if (mEPState == EPState.Idle) {
                        return Result.SUCCESS;
                    } else if (mEPState == EPState.InMeeting) {
                        return mMeeting.muteRemoteSpeaker(extId, onOff);
                    } else {
                        return mP2P.muteRemoteSpeaker(extId, onOff);
                    }
                default:
                    return Result.SUCCESS;
            }
        });
    }

    // @brief 获取音频指定通道的当前音量
    // @param extId [IN] extra id for caller
    public float getAudioCurrentVolume(AudioChannelId id, int extId) {
        if (!isReady())
            return 0.0f;

        switch(id) {
            case Mic:
                // TODO: 本地source的音频stream？
                return mMic == null ? 0.0f : mMic.micVolume;
            case Speaker:
                return mLDHelper.getVolume();
            case LR:
                return mLRHelper.getVolume();
            case CallingRx:
                if (mEPState == EPState.Idle) {
                    return 0.0f;
                } else if (mEPState == EPState.InMeeting) {
                    return mMeeting.getRemoteMicVolume(extId);
                } else {
                    return mP2P.getRemoteMicVolume(extId);
                }
            case CallingTx:
                if (mEPState == EPState.Idle) {
                    return 0.0f;
                } else if (mEPState == EPState.InMeeting) {
                    return mMeeting.getRemoteSpeakerVolume(extId);
                } else {
                    return mP2P.getRemoteSpeakerVolume(extId);
                }
            default:
                return 0.0f;
        }
    }

    // @brief 设置音频指定通道的当前音量
    // @param extId [IN] extra id for caller
    public void setAudioCurrentVolume(AudioChannelId id, int extId, float volume, Callback callback) {
        asyncCall("setAudioCurrentVolume", callback, () -> {
            switch (id) {
                case Mic:
                    // TODO: 本地source的音频stream？
                    if (mMic == null || mMic.micVolume == volume)
                        return Result.SUCCESS;
                    mMic.micVolume = volume;
                    mMic.micMuted = isMute(volume);
                    onAudioChanged(TSRole.Mic, mMic.epId);
                    return Result.SUCCESS;
                case Speaker:
                    return mLDHelper.setVolume(volume);
                case LR:
                    return mLRHelper.setVolume(volume);
                case CallingRx:
                    if (mEPState == EPState.Idle) {
                        return Result.SUCCESS;
                    } else if (mEPState == EPState.InMeeting) {
                        return mMeeting.setRemoteMicVolume(extId, volume);
                    } else {
                        return mP2P.setRemoteMicVolume(extId, volume);
                    }
                case CallingTx:
                    if (mEPState == EPState.Idle) {
                        return Result.SUCCESS;
                    } else if (mEPState == EPState.InMeeting) {
                        return mMeeting.setRemoteSpeakerVolume(extId, volume);
                    } else {
                        return mP2P.setRemoteSpeakerVolume(extId, volume);
                    }
                default:
                    return Result.SUCCESS;
            }
        });
    }

    // @brief 获取远程连接状态
    public State getRemoteState(int remoteId) {
        if (mEPState == EPState.Idle)
            return State.None;
        else if (mEPState == EPState.InMeeting)
            return mMeeting.getRemoteState(remoteId);
        else
            return mP2P.getRemoteState(remoteId);
    }

    // @brief 切换远程连接(呼叫/挂断)
    public void switchRemoteState(int remoteId, boolean onOff, Callback callback) {
        asyncCall("switchRemoteState", callback, () -> {
            if (mEPState == EPState.InMeeting)
                return onOff ? mMeeting.reInvite(remoteId) : mMeeting.hangup(remoteId);
            else
                return genError("switchRemoteState", BaseError.ACTION_ILLEGAL, "not in meeting", "不在会议中");
        });
    }

    // @brief 获取双流状态
    public EPDir getVideoExtState() {
        if (!isReady() || mEPState == EPState.Idle) {
            return EPDir.None;
        } else if (mEPState == EPState.InMeeting) {
            return mMeeting.getVideoExtState();
        } else {
            return mP2P.getVideoExtState();
        }
    }

    // @brief 切换辅流发送
    public void switchVideoExtSending(boolean onOff, Callback callback) {
        asyncCall("switchVideoExtSending", callback, () -> {
            if (mEPState == EPState.Idle) {
                return Result.SUCCESS;
            } else if (mEPState == EPState.InMeeting) {
                return mMeeting.switchVideoExtSending(onOff);
            } else {
                return mP2P.switchVideoExtSending(onOff);
            }
        });
    }

    // @brief 获取信号源锁定状态
    public boolean isSourceLocked(TSRole role) {
        if (isReady())
            return mSources2.get(role).locked;
        else
            return false;
    }

    // @brief 切换信号源锁定状态
    public void switchSourceLock(TSRole role, boolean locked, Callback callback) {
        asyncCall("switchSourceLock", callback, () -> {
            if (getVideoExtState() == EPDir.Outgoing && locked)
                return genError("switchSourceLock", BaseError.ACTION_ILLEGAL,
                        "has been sending video ext", "操作失败,已经发送辅流");

            CSource source = mSources2.get(role);
            if (source.locked == locked)
                return Result.SUCCESS;

            source.locked = locked;

            TSEvent event = locked ? TSEvent.LockTarget : TSEvent.UnLockTarget;
            mDIRHelper.pushSourceEvent(event, source);
            return Result.SUCCESS;
        });
    }

    // @brief 获取直播录制当前标题参数
    public LRTitleStyle getCurrentLRTitleStyle() {
        if (isReady())
            return mLRHelper.getLRTitleStyle();
        else
            return Const.DEFAULT_LR_TITLE_STYLE;
    }

    // @brief 设置直播录制标题当前参数
    public void setCurrentLRTitleStyle(LRTitleStyle style, Callback callback) {
        asyncCall("setCurrentLRTitleStyle", callback, () -> mLRHelper.setLRTitleStyle(style));
    }

    // @brief 获取会场名当前配置
    public RoomNameStyle getCurrentRoomNameStyle() {
        if (!isReady())
            return Const.DEFAULT_ROOM_NAME_STYLE;

        if (mEPState == EPState.Idle) {
            return getDefaultRoomNameStyle();
        } else if (mEPState == EPState.InMeeting) {
            return mMeeting.getRoomNameStyle();
        } else {
            return mP2P.getRoomNameStyle();
        }
    }

    // @brief 设置会场名当前配置
    public void setCurrentRoomNameStyle(RoomNameStyle style, Callback callback) {
        asyncCall("setCurrentRoomNameStyle", callback, () -> {
            if (mEPState == EPState.Idle) {
                return Result.SUCCESS;
            } else {
                boolean changed;
                if (mEPState == EPState.InMeeting) {
                    changed = !mMeeting.getRoomNameStyle().isEqual(style);
                    Result result = mMeeting.setRoomNameStyle(style);
                    if (!result.isSuccessful())
                        return result;
                } else {
                    changed = !mP2P.getRoomNameStyle().isEqual(style);
                    Result result = mP2P.setRoomNameStyle(style);
                    if (!result.isSuccessful())
                        return result;
                }

                if (changed) {
                    if (mLDHelper.getDisplayState(true) == State.Done)
                        mLDHelper.flushLDLayout(true);
                    if (mLDHelper.getDisplayState(false) == State.Done)
                        mLDHelper.flushLDLayout(false);
                    mLRHelper.flushLRLayout();
                }
                return Result.SUCCESS;
            }
        });
    }

    // @brief 获取会场名显示状态
    public boolean isShownRoomName() {
        if (!isReady())
            return false;

        if (mEPState == EPState.Idle) {
            return false;
        } else if (mEPState == EPState.InMeeting) {
            return mMeeting.isShownRoomName();
        } else {
            return mP2P.isShownRoomName();
        }
    }

    // @brief 切换会场名显示状态
    public void switchShownRoomName(boolean onOff, Callback callback) {
        asyncCall("switchShownRoomName", callback, () -> {
            if (mEPState == EPState.Idle) {
                return Result.SUCCESS;
            } else {
                boolean changed;
                if (mEPState == EPState.InMeeting) {
                    changed = mMeeting.isShownRoomName() != onOff;
                    Result result = mMeeting.switchShownRoomName(onOff);
                    if (!result.isSuccessful())
                        return result;
                } else {
                    changed = mP2P.isShownRoomName() != onOff;
                    Result result = mP2P.switchShownRoomName(onOff);
                    if (!result.isSuccessful())
                        return result;
                }

                if (changed) {
                    if (mLDHelper.getDisplayState(true) == State.Done)
                        mLDHelper.flushLDLayout(true);
                    if (mLDHelper.getDisplayState(false) == State.Done)
                        mLDHelper.flushLDLayout(false);
                    mLRHelper.flushLRLayout();
                }
                return Result.SUCCESS;
            }
        });
    }

    // @brief 获取媒体统计
    public void getMediaStatistics(Callback/**@see MediaStatistics*/ callback) {
        asyncCall("getMediaStatistics", callback, () -> {
            MediaStatistics statistics = mEndpoint.getMediaStatistics();
            return Result.buildSuccess(statistics);
        });
    }

    public void getMediaStatisticsStr(Callback/**@see MediaStatistics*/ callback) {
        asyncCall("getMediaStatisticsStr", callback, () -> {
            String statistics = mEndpoint.getMediaStatisticsStr();
            // logAction("getMediaStatistics", statistics);
            return Result.buildSuccess(statistics);
        });
    }

    /////////////////////////////// 单点呼叫控制接口

    // @brief 获取点对点当前呼叫参数
    public CallingParam getP2PCallingParam() {
        if (!isReady())
            return new CallingParam();

        if (mEPState == EPState.Idle || mEPState == EPState.InMeeting) {
            EPBaseConfig config = mEPHelper.getEPBaseConfig();
            EPVideoCapability video = mEPHelper.getEPVideoCapability();
            EPAudioCapability audio = mEPHelper.getEPAudioCapability();
            return new CallingParam("", "", config.defaultProtocol, video.maxBandwidth,
                    video.maxResolution, video.maxFramerate, video.codecs, audio.codecs);
        } else {
            return mP2P.getCallingParam();
        }
    }

    // @brief 创建点对点互动
    public void createP2P(CallingParam param, Callback callback) {
        createP2P(param, -1, callback);
    }

    public void createP2P(CallingParam param, int incomingId, Callback callback) {
        asyncCall("createP2P", callback, () -> {
            if (mEPState != EPState.Idle)
                return genError("createP2P", BaseError.ACTION_ILLEGAL, "not idle",
                        "设备正忙,无法发起呼叫");
            if (!param.checkName())
                return genError("createP2P", BaseError.INVALID_PARAM, "invalid calling url: " + param.url,
                        "无效的呼叫地址: " + param.url);

            // clear old error records
            Const.gMessageBox.clear();

            // double check for starting video stream of source
            for (CSource source: mSources.values()) {
                for (TSStream stream: source.vStreams) {
                    if (!stream.isProcessed()) {
                        int decId = mEndpoint.epStartSrcStreamDecoding(source.epId, stream.id);
                        stream.onDecoding(decId);
                    }
                }
            }

            // gen calling extend param2
            ExtParam param2 = new ExtParam(mEPHelper.getDefaultVideoExt(),
                    mEPHelper.getDefaultRoomNameStyle(), mEPHelper.getEPBaseConfig(),
                    mEPHelper.getEPVideoCapability(), mEPHelper.getEPAudioCapability());

            // create and init p2p calling
            P2PCalling p2p = new P2PCalling(mEndpoint, mHelper, this, mRTWorker);
            Result result = p2p.init(param, param2, incomingId);
            if (!result.isSuccessful())
                return genError("createP2P", BaseError.INTERNAL_ERROR,
                        "init p2p calling failed: " + result.code,
                        "初始化呼叫失败: " + result.getMessage());

            // flush content of LR Title
            mHDName = CoreUtils.getHDName4P2P(param.name);
            LRTitleStyle title = mLRHelper.getDefaultLRTitleStyle();
            mLRHelper.setLRTitleStyle(title, true);

            // start LR by strategy
            switchLRStateImpl(LRId.LocalRecording, LRState.Stop);
            for (LRId id: LRId.values()) {
                if (id == LRId.LiveAndPushing)
                    continue;
                LRSSStrategy strategy = mLRHelper.getLRSSStrategy(id);
                if (strategy == LRSSStrategy.InCalling)
                    switchLRStateImpl(id, LRState.Start);
            }

            // switch tracking by default
            setCurrentTrackingSwitch(mDefaultTracking);

            // update state
            mEPState = incomingId < 0 ? EPState.Outgoing : EPState.Incoming;
            mP2P = p2p;

            // notify outside observer
            if (mObserver != null)
                mObserver.onStateChanged(mEPState);

            // start to preview
            startPreview();

            // gen valid roles for directing
            List<String> validRoles = new LinkedList<>();
            for (TSRole role: mSources2.keySet())
                validRoles.add(role.name());
            validRoles.add(TSRole.None.name());
            validRoles.add(TSRole.Caller.name());
            validRoles.add(TSRole.CallerPlus.name());
            if (isLocalExtValid())
                validRoles.add(TSRole.LocalExt.name());

            // gen p2p directing content and modes
            Map<DirectingType, Object> contents = getP2PDirectingContents();
            Map<DirectingType, DirectingMode> modes = new HashMap<>(3);
            modes.put(DirectingType.Local, mDIRHelper.getDefaultMode(DirectingType.Local));
            modes.put(DirectingType.LR, mDIRHelper.getDefaultMode(DirectingType.LR));
            modes.put(DirectingType.TX, mDIRHelper.getDefaultMode(DirectingType.TX));

            // start directing
            mDIRHelper.stop();
            mDIRHelper.start(contents, modes, validRoles);
            mDIRHelper.reFlushAllLayout();

            // make or accept the call
            result = mP2P.start(incomingId);
            if (!result.isSuccessful()) {
                releaseP2PImpl();
                return result;
            }

            return Result.SUCCESS;
        });
    }

    // @brief 结束点对点互动
    // 结束呼叫/停止直播录制/停止预览
    public void releaseP2P(Callback callback) {
        asyncCall("releaseP2P", callback, this::releaseP2PImpl);
    }

    private Result releaseP2PImpl() {
        if (mEPState == EPState.InMeeting || mEPState == EPState.Idle)
            return Result.SUCCESS;

        // stop directing
        mDIRHelper.stop();

        // stop preview
        stopPreview();

        // release the p2p calling
        mP2P.release();
        mP2P = null;

        // flush content of LR Title
        mHDName = "";
        LRTitleStyle title = mLRHelper.getDefaultLRTitleStyle();
        mLRHelper.setLRTitleStyle(title, true);

        // stop LR by default strategy
        for (LRId id: LRId.values()) {
            if (id == LRId.LiveAndPushing)
                continue;
            LRSSStrategy strategy = mLRHelper.getLRSSStrategy(id);
            if (strategy == LRSSStrategy.InCalling ||
                    strategy == LRSSStrategy.Forbidden ||
                    (id == LRId.LocalRecording && strategy == LRSSStrategy.Manual))
                switchLRStateImpl(id, LRState.Stop);
        }

        // update state
        mEPState = EPState.Idle;

        // notify outside observer
        if (mObserver != null)
            mObserver.onStateChanged(mEPState);

        // start idle directing
        if (mIdleDIRValidRoles != null) {
            mDIRHelper.start(mIdleDIRContents, mIdleDIRModes, mIdleDIRValidRoles);
            mDIRHelper.reFlushAllLayout();
        }

        return Result.SUCCESS;
    }

    // @brief 接受点对点被叫
    public void p2pAccept(Callback callback) {
        asyncCall("p2pAccept", callback, () -> {
            if (mEPState != EPState.Idle && mEPState != EPState.InMeeting)
                return mP2P.answer(true);
            else
                return genError("p2pAccept", BaseError.ACTION_ILLEGAL,
                        "not in p2p calling", "不在点对点呼叫中");
        });
    }

    // @brief 拒绝点对点被叫
    public void p2pReject(Callback callback) {
        asyncCall("p2pReject", callback, () -> {
            if (mEPState != EPState.Idle && mEPState != EPState.InMeeting)
                return mP2P.answer(false);

            genError2("p2pReject", BaseError.ACTION_ILLEGAL,
                    "not in p2p calling", "不在点对点呼叫中");
            return Result.SUCCESS;
        });
    }

    // @brief 点对点呼叫发送DTMF
    public void p2pSendDTMF(char key, Callback callback) {
        asyncCall("p2pSendDTMF", callback, () -> {
            if (mEPState != EPState.Idle && mEPState != EPState.InMeeting)
                return mP2P.sendDTMF(key);
            else
                return genError("p2pSendDTMF", BaseError.ACTION_ILLEGAL,
                        "not in p2p calling", "不在点对点呼叫中");
        });
    }

    // @brief 获取点对点呼叫统计
    public void p2pGetStatistics(Callback/**@see CallingStatistics */ callback) {
        asyncCall("p2pGetStatistics", callback, () -> {
            if (mEPState == EPState.Idle || mEPState == EPState.InMeeting)
                return genError("p2pGetStatistics", BaseError.ACTION_ILLEGAL,
                        "not in p2p calling", "不在点对点呼叫中");

            CallingStatistics statistics = mP2P.getCallingStatistics();
            return Result.buildSuccess(statistics);
        });
    }

    /////////////////////////////// 多点呼叫控制接口

    // @brief 创建多点互动
    public void createMeeting(MeetingParam param, Callback callback) {
        asyncCall("createMeeting", callback, () -> {
            if (mMode.maxHDCount < 3)
                return genError("createMeeting", BaseError.ACTION_ILLEGAL, "not granted for meeting",
                        "未授权使用多点呼叫");
            if (mEPState != EPState.Idle)
                return genError("createMeeting", BaseError.ACTION_ILLEGAL, "not idle",
                        "设备正忙,无法发起会议");

            // gen meeting param2
            ExtParam param2 = new ExtParam(mEPHelper.getDefaultVideoExt(),
                    mEPHelper.getDefaultRoomNameStyle(), mEPHelper.getEPBaseConfig(),
                    mEPHelper.getEPVideoCapability(), mEPHelper.getEPAudioCapability());

            // create and init meeting
            Meeting meeting = new Meeting(mEndpoint, mHelper, this, mRTWorker);
            Result result = meeting.init(param, param2);
            if (!result.isSuccessful()) {
                meeting.release();
                return genError("createMeeting", BaseError.INTERNAL_ERROR,
                        "init meeting failed: " + result.code,
                        "初始化会议失败: " + result.getMessage());
            }

            // clear old error records
            Const.gMessageBox.clear();

            // disable unused video stream of source
            for (CSource source: mSources.values()) {
                if (CoreUtils.isCrossed(source.roles, param.sources))
                    continue;
                for (TSStream stream: source.vStreams) {
                    if (stream.isProcessed()) {
                        mEndpoint.epStopSrcStreamDecoding(source.epId, stream.id);
                        stream.onStopped();
                    }
                }
            }

            // flush content of LR Title
            mHDName = param.name;
            LRTitleStyle title = mLRHelper.getDefaultLRTitleStyle();
            mLRHelper.setLRTitleStyle(title, true);

            // start local living and recording by default
            switchLRStateImpl(LRId.LocalRecording, LRState.Stop);
            if (param.defaultLocalLive)
                switchLRStateImpl(LRId.LocalLive, LRState.Start);
            if (param.defaultLocalRecording)
                switchLRStateImpl(LRId.LocalRecording, LRState.Start);

            // start pushing by default strategy
            LRSSStrategy strategy = mLRHelper.getLRSSStrategy(LRId.FixedPushing);
            if (strategy == LRSSStrategy.InCalling)
                switchLRStateImpl(LRId.FixedPushing, LRState.Start);
            strategy = mLRHelper.getLRSSStrategy(LRId.IntegratedPushing);
            if (strategy == LRSSStrategy.InCalling)
                switchLRStateImpl(LRId.IntegratedPushing, LRState.Start);

            // switch tracking by default
            setCurrentTrackingSwitch(param.defaultTracking);

            // update state
            mEPState = EPState.InMeeting;
            mMeeting = meeting;

            // notify outside observer
            if (mObserver != null)
                mObserver.onStateChanged(mEPState);

            // start to preview
            startPreview();

            // gen valid roles for directing
            List<String> validRoles = new LinkedList<>();
            for (TSRole role: param.sources)
                validRoles.add(role.name());
            validRoles.add(TSRole.None.name());
            validRoles.add(TSRole.Caller.name());
            validRoles.add(TSRole.CallerPlus.name());
            if (isLocalExtValid())
                validRoles.add(TSRole.LocalExt.name());

            // start directing
            mDIRHelper.stop();
            mDIRHelper.start(param.directingContents, param.defaultDirectingMode, validRoles);
            mDIRHelper.reFlushAllLayout();

            return mMeeting.start();
        });
    }

    // @brief 结束多点互动
    public void releaseMeeting(Callback callback) {
        asyncCall("releaseMeeting", callback, () -> {
            if (mEPState != EPState.InMeeting)
                return Result.SUCCESS;

            // stop directing
            mDIRHelper.stop();

            // stop preview
            stopPreview();

            // release the meeting
            mMeeting.release();
            mMeeting = null;

            // flush content of LR Title
            mHDName = "";
            LRTitleStyle title = mLRHelper.getDefaultLRTitleStyle();
            mLRHelper.setLRTitleStyle(title, true);

            // stop LR by default strategy
            for (LRId id: LRId.values()) {
                if (id == LRId.LiveAndPushing)
                    continue;
                LRSSStrategy strategy = mLRHelper.getLRSSStrategy(id);
                if (strategy == LRSSStrategy.InCalling ||
                        strategy == LRSSStrategy.Forbidden ||
                        (id == LRId.LocalRecording && strategy == LRSSStrategy.Manual))
                    switchLRStateImpl(id, LRState.Stop);
            }

            // recover unused video stream of source
            for (CSource source: mSources.values()) {
                for (TSStream stream: source.vStreams) {
                    if (!stream.isProcessed()) {
                        int decId = mEndpoint.epStartSrcStreamDecoding(source.epId, stream.id);
                        if (decId < 0) {
                            LogUtil.w(CoreUtils.TAG, TAG, "releaseMeeting, epStartSrcStreamDecoding failed: " + decId);
                            continue;
                        }
                        stream.onDecoding(decId);
                    }
                }
            }

            // update state
            mEPState = EPState.Idle;

            // notify outside observer
            if (mObserver != null)
                mObserver.onStateChanged(mEPState);

            // start idle directing
            if (mIdleDIRValidRoles != null) {
                mDIRHelper.start(mIdleDIRContents, mIdleDIRModes, mIdleDIRValidRoles);
                mDIRHelper.reFlushAllLayout();
            }

            return Result.SUCCESS;
        });
    }

    // @brief 会议讨论中
    public boolean inMeetingDiscussion() {
        if (isReady() && mEPState == EPState.InMeeting)
            return mMeeting.inDiscussion();
        else
            return false;
    }

    // @brief 切换会议讨论
    public void switchMeetingDiscussion(boolean onOff, Callback callback) {
        asyncCall("switchMeetingDiscussion", callback, () -> {
            if (mEPState == EPState.InMeeting)
                return mMeeting.switchDiscussion(onOff);
            else
                return genError("switchMeetingDiscussion", BaseError.ACTION_ILLEGAL, "not in meeting", "不在会议中");
        });
    }

    // @brief 获取远程点名状态
    public boolean inMeetingSpeaking(int remoteId) {
        if (isReady() && mEPState == EPState.InMeeting)
            return mMeeting.inSpeaking(remoteId);
        else
            return false;
    }

    // @brief 切换远程点名状态
    public void switchMeetingSpeaking(int remoteId, boolean onOff, Callback callback) {
        asyncCall("switchMeetingSpeaking", callback, () -> {
            if (mEPState == EPState.InMeeting)
                return mMeeting.switchSpeaking(remoteId, onOff);
            else
                return genError("switchMeetingSpeaking", BaseError.ACTION_ILLEGAL, "not in meeting", "不在会议中");
        });
    }

    // @brief 获取指定远程的呼叫统计
    public void getMeetingCallingStatistics(int remoteId, Callback/**@see CallingStatistics*/ callback) {
        asyncCall("getMeetingCallingStatistics", callback, () -> {
            if (mEPState != EPState.InMeeting)
                return genError("getMeetingCallingStatistics", BaseError.ACTION_ILLEGAL, "not in meeting", "不在会议中");

            CallingStatistics statistics = mMeeting.getCallingStatistics(remoteId);
            return Result.buildSuccess(statistics);
        });
    }

    // @brief 获取所有远程的呼叫统计
    public void getMeetingAllCallingStatistics(Callback/**@see Map<Integer, CallingStatistics>*/ callback) {
        asyncCall("getMeetingAllCallingStatistics", callback, () -> {
            if (mEPState != EPState.InMeeting)
                return genError("getMeetingAllCallingStatistics", BaseError.ACTION_ILLEGAL, "not in meeting", "不在会议中");

            Map<Integer, CallingStatistics> statistics = mMeeting.getAllCallingStatistics();
            return Result.buildSuccess(statistics);
        });
    }

    /////////////////////////////// 云台控制接口

    // @brief 设置云台控制目标
    public void setPTZTarget(TSRole role, Callback callback) {
        asyncCall("setPTZTarget", callback, () -> {
            if (mCamera == null)
                return genError2("setPTZTarget", BaseError.ACTION_UNSUPPORTED, "has no valid ptz config", "未配置有效的云台地址");

            int id = mPTZConfig.getTargetId(role);
            if (id < 1)
                return genError2("setPTZTarget", BaseError.ACTION_UNSUPPORTED,
                        "invalid target id for " + role.name(), "无法控制当前目标: " + role.desc);

            int ret = mCamera.setPTZTarget(id);
            return new Result(ret, ret == 0 ? null : "内部错误");
        });
    }

    // @brief 获取云台速度
    public int getPTZSpeed() {
        if (mCamera == null)
            return 0x0a;
        else
            return mCamera.getPTZSpeed();
    }

    // @brief 设置云台速度
    public void setPTZSpeed(int speed, Callback callback) {
        asyncCall("setPTZSpeed", callback, () -> {
            if (mCamera == null)
                return genError("setPTZSpeed", BaseError.ACTION_UNSUPPORTED, "has no valid ptz config", "未配置有效的云台地址");

            int ret = mCamera.setPTZSpeed(speed);
            return new Result(ret, ret == 0 ? null : "内部错误");
        });
    }

    // @brief 移动云台
    public void movePTZ(CameraDirective dir, Callback callback) {
        asyncCall("movePTZ", callback, () -> {
            if (mCamera == null)
                return genError("movePTZ", BaseError.ACTION_UNSUPPORTED, "has no valid ptz config", "未配置有效的云台地址");

            int ret = mCamera.movePTZ(dir);
            return new Result(ret, ret == 0 ? null : "内部错误");
        });
    }

    // @brief 对焦云台
    public void setPTZZoom(ZoomAction action, Callback callback) {
        asyncCall("setPTZZoom", callback, () -> {
            if (mCamera == null)
                return genError("setPTZZoom", BaseError.ACTION_UNSUPPORTED, "has no valid ptz config", "未配置有效的云台地址");

            int ret = mCamera.setPTZZoom(action);
            return new Result(ret, ret == 0 ? null : "内部错误");
        });
    }

    // @brief 设置云台预置位
    public void setPTZPreset(int number, Callback callback) {
        asyncCall("setPTZPreset", callback, () -> {
            if (mCamera == null)
                return genError("setPTZPreset", BaseError.ACTION_UNSUPPORTED, "has no valid ptz config", "未配置有效的云台地址");

            int ret = mCamera.setPTZPreset(number);
            return new Result(ret, ret == 0 ? null : "内部错误");
        });
    }

    // @brief 调用云台预置位
    public void loadPTZPreset(int number, Callback callback) {
        asyncCall("loadPTZPreset", callback, () -> {
            if (mCamera == null)
                return genError("loadPTZPreset", BaseError.ACTION_UNSUPPORTED, "has no valid ptz config", "未配置有效的云台地址");

            int ret = mCamera.loadPTZPreset(number);
            return new Result(ret, ret == 0 ? null : "内部错误");
        });
    }

    /////////////////////////////// 测试接口

    public void echoLocalAudio(boolean onOff, Callback callback) {
        asyncCall("echoLocalAudio", callback, () -> mLDHelper.echoLocalAudio(onOff));
    }

    /////////////////////////////// implementation of Endpoint3.Callback

    @Override
    public void onError(int errcode, String error) {
        LogUtil.w(CoreUtils.TAG, TAG, "onError(EP): " + errcode + ", " + error);
    }

    @Override
    public void onStreamOpen(EPObjectType parent_type, int parent_id, int stream_id, StreamDesc desc, Object format) {
        asyncCall(() -> {
            if (parent_type == EPObjectType.Source) {
                CSource source = mSources.get(parent_id);
                if (source == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamOpen, from unknown source#" + parent_id);
                    return;
                }

                List<TSStream> streams = desc.type == DataType.AUDIO ? source.aStreams : source.vStreams;
                TSStream stream = new TSStream(stream_id, desc, format);
                streams.add(stream);

                boolean decoding;
                if (source.config.id == SourceId.MIC) {
                    decoding = true;
                } else if (desc.type == DataType.AUDIO) {
                    // TODO: skip all audio stream for local source?
                    decoding = false;
                } else if (mEPState == EPState.InMeeting) {
                    List<TSRole> roles = mMeeting.getMeetingParam().sources;
                    decoding = CoreUtils.isCrossed(roles, source.roles);
                } else {
                    // decoding all video source for p2p calling
                    decoding = true;
                }

                if (decoding) {

                    // start decoding rx stream
                    int decId = mEndpoint.epStartSrcStreamDecoding(source.epId, stream_id);
                    if (decId < 0) {
                        LogUtil.w(CoreUtils.TAG, TAG, "onStreamOpen, epStartSrcStreamDecoding failed: " + decId);
                        return;
                    }
                    stream.onDecoding(decId);

                    // apply stream change
                    if (desc.type == DataType.AUDIO) {
                        for (TSRole role: source.roles)
                            onAudioChanged(role, source.epId);
                    } else {
                        mDIRHelper.pushSourceEvent(TSEvent.SourceVideoReady, source);
                    }

                    // apply preview status
                    if ((desc.type == DataType.VIDEO || desc.type == DataType.VIDEO_EXT) &&
                            mEPState.flag == State.Done) {
                        if (mPreviewHelper.startSourcePreview(source).isSuccessful())
                            source.previewUrl = mPreviewHelper.getSourcePreviewUrl(source.epId);
                    }
                }

            } else if (parent_type == EPObjectType.Caller) {
                if (mEPState == EPState.Idle) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamOpen from caller#" + parent_id);
                } else if (mEPState == EPState.InMeeting) {
                    mMeeting.onStreamOpen(parent_type, parent_id, stream_id, desc, format);
                } else {
                    mP2P.onStreamOpen(parent_type, parent_id, stream_id, desc, format);
                }
            } else if (parent_type == EPObjectType.Output) {
                LogUtil.i(CoreUtils.TAG, TAG, "onStreamOpen from output#" + parent_id);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "onStreamOpen, unknown from: " + parent_type.name());
            }
        });
    }

    @Override
    public void onStreamClose(EPObjectType parent_type, int parent_id, int stream_id, StreamDesc desc) {
        asyncCall(() -> {
            if (parent_type == EPObjectType.Source) {
                CSource source = mSources.get(parent_id);
                if (source == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamClose, from unknown source#" + parent_id +
                            ", it may have been remove previously, force to release it");
                    mEndpoint.epStopSrcStreamDecoding(parent_id, stream_id);
                    return;
                }

                TSStream stream = source.removeStream(desc.type, stream_id);
                if (stream.isProcessed()) {

                    // start decoding rx stream
                    mEndpoint.epStopSrcStreamDecoding(source.epId, stream_id);
                    stream.onStopped();

                    // apply stream change
                    if (desc.type == DataType.AUDIO) {
                        for (TSRole role: source.roles)
                            onAudioChanged(role, source.epId);
                    } else {
                        mDIRHelper.pushSourceEvent(TSEvent.SourceVideoOver, source);
                    }

                    // apply preview status
                    if (desc.type == DataType.VIDEO || desc.type == DataType.VIDEO_EXT) {
                        mPreviewHelper.stopSourcePreview(source.epId);
                        source.previewUrl = "";
                    }
                }

            } else if (parent_type == EPObjectType.Caller) {
                if (mEPState == EPState.Idle) {
                    LogUtil.i(CoreUtils.TAG, TAG, "onStreamClose from caller#" + parent_id +
                            ", it may have been remove previously");
                } else if (mEPState == EPState.InMeeting) {
                    mMeeting.onStreamClose(parent_type, parent_id, stream_id, desc);
                } else {
                    mP2P.onStreamClose(parent_type, parent_id, stream_id, desc);
                }
            } else if (parent_type == EPObjectType.Output) {
                LogUtil.i(CoreUtils.TAG, TAG, "onStreamClose from output#" + parent_id);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "onStreamClose, unknown from: " + parent_type.name());
            }
        });
    }

    @Override
    public void onRegistering(int result, CallingProtocol protocol) {
        asyncCall(() -> {
            if (result != 0)
                LogUtil.w(Const.TAG, "onRegistering, " + protocol.name + " registering failed: " + result);

            State state = result == 0 ? State.Done : State.Doing;
            boolean changed = mEPHelper.onRegistering(state, protocol);

            // notify outside observer
            if (changed && mObserver != null)
                mObserver.onRegisteringStateChanged(state, protocol);
        });
    }

    @Override
    public void onUnRegistering(int result, CallingProtocol protocol) {
        asyncCall(() -> mEPHelper.onUnRegistering(result, protocol));
    }

    @Override
    public void onNotifyRegisterStatus(int result, CallingProtocol protocol) {
        asyncCall(() -> {
            State state = result == 0 ? State.Done : State.Doing;
            boolean changed = mEPHelper.onNotifyRegisterStatus(state, protocol);

            // notify outside observer
            if (changed && mObserver != null)
                mObserver.onRegisteringStateChanged(state, protocol);
        });
    }

    @Override
    public void onIncomingCall(int call_id, String number, String call_url, CallingProtocol protocol) {
        asyncCall(() -> {
            // gen calling param
            EPVideoCapability vCap = mEPHelper.getEPVideoCapability();
            EPAudioCapability aCap = mEPHelper.getEPAudioCapability();
            CallingParam param = new CallingParam(null, call_url, protocol, vCap.maxBandwidth,
                    vCap.maxResolution, vCap.maxFramerate, vCap.codecs, aCap.codecs);
            param.checkName();

            if (mEPState == EPState.Idle) {
                // create p2p calling
                createP2P(param, call_id, null);
            } else if (mEPState == EPState.InMeeting) {
                mMeeting.onIncomingCall(call_id, number, call_url, protocol);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "in busy now");
                mEndpoint.epReject(call_id);
            }

            // notify outside observer
            if (mObserver != null)
                mObserver.onIncomingCall(call_id, param);
        });
    }

    @Override
    public void onRemoteRinging(int call_id) {
        asyncCall(() -> {
            LogUtil.d(CoreUtils.TAG, TAG, "onRemoteRinging#" + call_id);
        });
    }

    @Override
    public void onEstablished(int call_id, String vendor, String name) {
        asyncCall(() -> {
            if (mEPState == EPState.Idle) {
                LogUtil.w(CoreUtils.TAG, TAG, "invalid callback: onEstablished");
            } else if (mEPState == EPState.InMeeting) {
                mMeeting.onEstablished(call_id, vendor, name);
            } else {
                mEPState = EPState.Established;
                mP2P.onEstablished(call_id, vendor, name);

                // notify outside observer
                if (mObserver != null)
                    mObserver.onStateChanged(mEPState);
            }
        });
    }

    @Override
    public void onFinished(int call_id, int errcode, String reason) {
        asyncCall(() -> {
            if (mEPState == EPState.Idle) {
                LogUtil.i(CoreUtils.TAG, TAG, "onFinished: " + call_id);
                mEndpoint.epReleaseCaller(call_id);
            } else if (mEPState == EPState.InMeeting) {
                mMeeting.onFinished(call_id, errcode, reason);
            } else if (call_id == mP2P.getCallerId()) {
                mP2P.onFinished(call_id, errcode, reason);
                releaseP2P(null);
            } else {
                mEndpoint.epReleaseCaller(call_id);
            }
        });
    }

    @Override
    public void onCallerError(int call_id, int errcode, String error) {
        asyncCall(() -> {
            if (mEPState == EPState.Idle) {
                LogUtil.w(CoreUtils.TAG, TAG, "onCallerError: " + call_id + ", " + errcode + ", " + error);
            } else if (mEPState == EPState.InMeeting) {
                mMeeting.onCallerError(call_id, errcode, error);
            } else {
                mP2P.onFinished(call_id, errcode, error);
                releaseP2P(null);
            }
        });
    }

    /////////////////////////////// implementation of EPListener

    @Override
    public void onCallerEvent(TSEvent event, CCaller caller) {
        asyncCall(() -> {
            int callerId = caller == null ? -1 : caller.epId;

            if (event.isAudio)
                onAudioChanged(TSRole.Caller, callerId);
            else
                mDIRHelper.pushCallerEvent(event, caller);

            if (event == TSEvent.RemoteReady) {
                mPreviewHelper.startRemotePreview(caller);
            } else if (event == TSEvent.RemoteDisconnected) {
                mPreviewHelper.stopRemotePreview(callerId);
            } else if (event == TSEvent.RemoteVideoExtOpened) {
                mPreviewHelper.startRemoteExtPreview(caller);
            } else if (event == TSEvent.RemoteVideoExtClosed) {
                mPreviewHelper.stopRemoteExtPreview();
            }
        });
    }

    @Override
    public void onHWEvent(TSEvent event) {
        asyncCall(() -> {
            mDIRHelper.pushHWEvent(event);

            if (event == TSEvent.MainDisplayReady) {
                if (mEPState.flag == State.Done)
                    mPreviewHelper.startDisplayPreview(DisplayId.LocalMain);
            } else if (event == TSEvent.MainDisplayUnable) {
                mPreviewHelper.stopDisplayPreview(DisplayId.LocalMain);
            } else if (event == TSEvent.SecondDisplayReady) {
                if (mEPState.flag == State.Done)
                    mPreviewHelper.startDisplayPreview(DisplayId.LocalSecond);
            } else if (event == TSEvent.SecondDisplayUnable) {
                mPreviewHelper.stopDisplayPreview(DisplayId.LocalSecond);
            }
        });
    }

    @Override
    public void onTXMainDisplayChanged(boolean ready) {
        asyncCall(() -> {
            if (ready) {
                if (mEPState.flag == State.Done)
                    mPreviewHelper.startDisplayPreview(DisplayId.TX);
            } else {
                mPreviewHelper.stopDisplayPreview(DisplayId.TX);
            }
        });
    }

    /////////////////////////////// implementation of DirectorHelper.Callback

    @Override
    public void onLayout(DisplayId id, DisplayLayout layout) {
        setDisplayLayout(id, layout, false, null);
    }

    /////////////////////////////// implementation of IxNCamera.EventListener

    @Override
    public void onIxNEvent(ExtEvent event) {
        asyncCall(() -> {
            mDIRHelper.pushExtEvent(event);
        });
    }

    /////////////////////////////// implementation of PPTMonitorServer.EventListener

    @Override
    public void onPPTEvent(ExtEvent event) {
        asyncCall(() -> {
            mDIRHelper.pushExtEvent(event);
        });
    }

    /////////////////////////////// private functions

    private boolean isReady() {
        return mEndpoint != null && mRTWorker != null && mLRHelper != null;
    }

    private void loadPreferences() {
        mRoleBindings = mSPHelper.getRoleBindings();
        mDefaultAudioVolumes = mSPHelper.getAudioDefaultVolumes();
        mDefaultTracking = mSPHelper.getDefaultTrackingSwitch();
        mPTZConfig = mSPHelper.getPTZConfig();
        mTrackingConfig = mSPHelper.getTrackingConfig();
    }

    private void loadMIC() {
        // add local mic
        AudioInputDevice device = mEndpoint.epGetDefaultAudioInputDevice();
        if (device != null) {
            int id = mEndpoint.epAddAudioCapture(device.url);
            if (id >= 0) {
                Source base = Source.buildMICSource(SourceId.MIC, device.url);
                mMic = new CSource(id, Arrays.asList(TSRole.Mic), base);
                mMic.connState = State.Done;
                mMic.micVolume = getAudioDefaultVolume(AudioChannelId.Mic);
                mMic.micMuted = isMute(mMic.micVolume);

                mSources.put(id, mMic);
                mSources2.put(TSRole.Mic, mMic);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "epAddAudioCapture(" + device.url + ") failed: " + id);
            }
        }
    }

    private void loadSources() {
        // add candidates
        for (Source candidate: mSrcCandidates) {
            // list all roles for this candidate
            List<TSRole> roles = new LinkedList<>();
            for (Map.Entry<TSRole, SourceId> entry: mRoleBindings.entrySet()) {
                TSRole role = entry.getKey();
                SourceId id = entry.getValue();
                if (id == candidate.id)
                    roles.add(role);
            }

            // skip unbound candidate
            if (roles.size() == 0)
                continue;

            int id = -1;
            if (candidate.type == SourceType.Capture) {
                id = mEndpoint.epAddVideoCapture(candidate.url, candidate.hdmiRes);
            } else if (candidate.type == SourceType.RTSP) {
                TransProtocol protocol = candidate.rtspOverTCP ? TransProtocol.TCP : TransProtocol.RTP;
                id = mEndpoint.epAddRTSPSource(candidate.url, protocol, Const.DEFAULT_SOURCE_RECONNECTING);
            }

            if (id >= 0) {
                CSource source = new CSource(id, roles, candidate);
                source.connState = State.Done;
                source.micVolume = getAudioDefaultVolume(AudioChannelId.Mic);
                source.micMuted = isMute(source.micVolume);

                mSources.put(id, source);
                for (TSRole role: roles)
                    mSources2.put(role, source);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "epAddXXSource(" + candidate.url + ") failed: " + id);
            }
        }
    }

    private void unloadSources() {
        if (mSources != null) {
            for (Map.Entry<Integer, CSource> entry : mSources.entrySet()) {
                mEndpoint.epRemoveSource(entry.getKey());
                entry.getValue().connState = State.None;
            }
            mSources.clear();
            mSources2.clear();
        }
        mMic = null;
    }

    private void initAndConnectIxNCamera() {
        if (mPTZConfig.isValid()) {
            mCamera = new IxNCamera();
            int ret = mCamera.init(mPTZConfig.viscaIdOfTeacher, this);
            if (ret != 0) {
                LogUtil.w(CoreUtils.TAG, TAG, "IxNCamera init failed: " + ret + ", disable PTZ and tracking");
                mCamera = null;
            } else {
                mRTWorker.postDelayed(() -> {
                    if (mCamera != null && mPTZConfig.isValid())
                        mCamera.connect(mPTZConfig.address, mPTZConfig.port, result -> {
                            LogUtil.i(CoreUtils.TAG, TAG, "connect to IxNCamera: " +
                                    (result.isSuccessful() ? "successful" : result.getMessage()));
                            if (result.isSuccessful()) {
                                mCamera.setTrackMode(mPTZConfig.viscaIdOfTeacher, mTracking);
                                mCamera.setTrackMode(mPTZConfig.viscaIdOfStudent, mTracking);
                            }
                        });
                }, 3000);
            }
        } else {
            LogUtil.w(CoreUtils.TAG, TAG, "invalid ptz config, give up connecting to the ptz");
        }
    }

    private void onAudioChanged(TSRole role, int parentId) {
        if (CoreUtils.containsAudio(mLDHelper.getLDLayout(true), role, parentId, true) ||
                CoreUtils.containsAudio(mLDHelper.getLDLayout(false), role, parentId, true))
            mLDHelper.flushLDMixer();

        if (CoreUtils.containsAudio(mLRHelper.getLRLayout(), role, parentId, false))
            mLRHelper.flushLRMixer();

        if (mMeeting != null && (
                CoreUtils.containsAudio(mMeeting.getTXMainLayout(), role, parentId, false) ||
                CoreUtils.containsAudio(mMeeting.getTXExtLayout(), role, parentId, false)))
            mMeeting.flushTXMixers();

        if (mP2P != null && (
                CoreUtils.containsAudio(mP2P.getTXMainLayout(), role, parentId, false) ||
                CoreUtils.containsAudio(mP2P.getTXExtLayout(), role, parentId, false)))
            mP2P.flushTXMixers();
    }

    private void initHelper() {
        mHelper = new ExtHelper() {
            private String customName = null;

            @Override
            public EPState getEPState() {
                return mEPState;
            }

            @Override
            public String getCustomName() {
                if (customName == null)
                    customName = mExtHelper.getCustomName();
                return customName;
            }

            @Override
            public String getCurrentHDName() {
                return mHDName;
            }

            @Override
            public boolean isShownRoomName() {
                if (mEPState == EPState.Idle)
                    return false;
                else if (mEPState == EPState.InMeeting)
                    return mMeeting.isShownRoomName();
                else
                    return mP2P.isShownRoomName();
            }

            @Override
            public RoomNameStyle getRoomNameStyle() {
                if (mEPState == EPState.Idle)
                    return mEPHelper.getDefaultRoomNameStyle();
                else if (mEPState == EPState.InMeeting)
                    return mMeeting.getRoomNameStyle();
                else
                    return mP2P.getRoomNameStyle();
            }

            @Override
            public CSource getSource(TSRole role) {
                if (isReady())
                    return mSources2.get(role);
                else
                    return null;
            }

            @Override
            public CSource getSource(int sourceId) {
                if (isReady())
                    return mSources.get(sourceId);
                else
                    return null;
            }

            @Override
            public CCaller getCaller(int callerId) {
                if (mEPState == EPState.Idle)
                    return null;
                else if (mEPState == EPState.InMeeting)
                    return mMeeting.getRemote(callerId);
                else
                    return mP2P.getRemote(callerId);
            }

            @Override
            public NetInfo getNetworkInfo() {
                if (isReady())
                    return mExtHelper.getNetworkInfo();
                else
                    return NetInfo.buildEmpty();
            }

            @Override
            public String getDisplayBackgroundColor() {
                if (isReady())
                    return mEPHelper.getEPFixedConfig().background_color;
                else
                    return "black";
            }

            @Override
            public DisplayLayout getDisplayLayout(DisplayId id) {
                return EndpointMBS.this.getDisplayLayout(id);
            }

            @Override
            public Terminal getContact(CallingUrl url) {
                return mExtHelper.getContact(url);
            }

            @Override
            public String getRemotePreviewUrl(int id) {
                if (isReady())
                    return mPreviewHelper.getRemotePreviewUrl(id);
                else
                    return "";
            }

            @Override
            public String getRemoteExtPreviewUrl() {
                if (isReady())
                    return mPreviewHelper.getRemoteExtPreviewUrl();
                else
                    return "";
            }

            @Override
            public LRCodec getLRCodec() {
                if (isReady())
                    return mLRHelper.getDefaultLRCodec();
                else
                    return Const.DEFAULT_LR_CODEC;
            }

            @Override
            public VideoFormat getTXMainVideoFormat() {
                if (!isReady() || mEPState == EPState.Idle) {
                    return null;
                } else if (mEPState == EPState.InMeeting) {
                    return mMeeting.getTXMainVideoFormat();
                } else {
                    return mP2P.getTXMainVideoFormat();
                }
            }

            @Override
            public VideoFormat getPreviewVideoFormat() {
                int framerate = Const.LOCAL_PREVIEW_VIDEO_FPS;
                Resolution resolution = Const.LOCAL_PREVIEW_VIDEO_RESOLUTION;
                Bandwidth bandwidth = Const.LOCAL_PREVIEW_VIDEO_BANDWIDTH;

                LRCodec codec = mHelper.getLRCodec();
                int keyInvInFrames = CoreUtils.trans2IFrameIntervalInFrame(codec.vIFrameInterval, framerate);

                return new VideoFormat(codec.vCodec, codec.vProfile, resolution,
                        framerate, bandwidth, keyInvInFrames);
            }

            @Override
            public int getDisplayEPId(DisplayId id) {
                if (!isReady())
                    return -1;
                else if (id == DisplayId.LocalMain)
                    return mLDHelper.getDisplayId(true);
                else if (id == DisplayId.LocalSecond)
                    return mLDHelper.getDisplayId(false);
                else if (id == DisplayId.LR)
                    return mLRHelper.getDisplayId();

                if (id != DisplayId.TX)
                    return -1;

                if (mEPState == EPState.Idle)
                    return -1;
                else if (mEPState == EPState.InMeeting)
                    return mMeeting.getDisplayId(true);
                else
                    return mP2P.getDisplayId(true);
            }

            @Override
            public int getMixerEPId(DisplayId id) {
                if (!isReady())
                    return -1;
                else if (id == DisplayId.LocalMain || id == DisplayId.LocalSecond)
                    return mLDHelper.getMixerId();
                else if (id == DisplayId.LR)
                    return mLRHelper.getMixerId();

                if (id != DisplayId.TX)
                    return -1;

                if (mEPState == EPState.Idle)
                    return -1;
                else if (mEPState == EPState.InMeeting)
                    return -1; // not support get the mixer for meeting
                else
                    return mP2P.getMixerId();
            }

            @Override
            public String getPublicAddress() {
                return mExtHelper.getPublicAddress();
            }

            @Override
            public int getPublicPreviewPort() {
                return mExtHelper.getPublicPreviewPort();
            }

            @Override
            public TSRole getVideoExt() {
                if (isReady())
                    return mEPHelper.getDefaultVideoExt();
                else
                    return TSRole.None;
            }

            @Override
            public List<TSRole> getAvailableLocalRoles() {
                if (isReady())
                    return new ArrayList<>(mSources2.keySet());
                else
                    return Collections.emptyList();
            }

            @Override
            public float getDefaultVolume(AudioChannelId id) {
                return getAudioDefaultVolume(id);
            }

            @Override
            public boolean isSupport2PreviewHDMIIn() {
                if (false) {
                    return Const.SUPPORTED_FOR_CAPTURE_SOURCE && isReady() &&
                            mLDHelper.getDisplayState(false) == State.None;
                } else {
                    return Const.SUPPORTED_FOR_CAPTURE_SOURCE;
                }
            }

            @Override
            public boolean isSupport2PreviewHDMIOut() {
                if (false) {
                    return Const.SUPPORTED_FOR_HDMI_OUT && isReady() &&
                            mLDHelper.getDisplayState(false) == State.None;
                } else {
                    return Const.SUPPORTED_FOR_HDMI_OUT;
                }
            }
        };
    }

    private void startPreview() {
        // start to preview for display
        mPreviewHelper.startDisplayPreview(DisplayId.LocalMain);
        mPreviewHelper.startDisplayPreview(DisplayId.LR);
        if (mHelper.getDisplayEPId(DisplayId.LocalSecond) >= 0)
            mPreviewHelper.startDisplayPreview(DisplayId.LocalSecond);
        if (mHelper.getDisplayEPId(DisplayId.TX) >= 0)
            mPreviewHelper.startDisplayPreview(DisplayId.TX);

        // start to preview those used sources
        for (CSource source: mSources.values()) {
            boolean used = false;
            for (TSStream stream: source.vStreams) {
                if (stream.isProcessed()) {
                    used = true;
                    break;
                }
            }
            if (used) {
                // start to preview the source and update url
                if (mPreviewHelper.startSourcePreview(source).isSuccessful())
                    source.previewUrl = mPreviewHelper.getSourcePreviewUrl(source.epId);
            }
        }
    }

    private void stopPreview() {
        mPreviewHelper.stop();
        for (CSource source: mSources.values())
            source.previewUrl = "";
    }

    private Result switchLRStateImpl(LRId id, LRState state) {
        String path = (mObserver != null && id == LRId.LocalRecording &&
                state == LRState.Stop && !CompareHelper.isEqual(mLRHelper.getLRState(id), LRState.Stop)) ?
                mLRHelper.getLRUrl(LRId.LocalRecording) : null;

        Result result = mLRHelper.switchLRState(id, state);
        if (result.isSuccessful() && path != null)
            mObserver.onRecordingFinished(path);

        return result;
    }

    private void initIdleDirecting() {
        // gen valid roles for idle directing
        mIdleDIRValidRoles = new LinkedList<>();
        for (TSRole role: mSources2.keySet())
            mIdleDIRValidRoles.add(role.name());
        mIdleDIRValidRoles.add(TSRole.None.name());
        if (isLocalExtValid())
            mIdleDIRValidRoles.add(TSRole.LocalExt.name());

        // gen idle directing modes
        mIdleDIRModes = new HashMap(3) {{
            put(DirectingType.Local, DirectingMode.FullAuto);
            put(DirectingType.LR, DirectingMode.FullAuto);
            put(DirectingType.TX, DirectingMode.Manual);
        }};

        // gen idle directing content
        mIdleDIRContents = getIdleDirectingContents();
    }

    private Map<DirectingType, Object> getIdleDirectingContents() {
        Map<DirectingType, Object> contents = new HashMap<>(Const.DEFAULT_DIRECTING_IDLE_TABLES.size());
        for (DirectingType type: Const.DEFAULT_DIRECTING_IDLE_TABLES.keySet()) {
            JsonObject content = mExtHelper.getIdleDIRTableContent(type);
            if (content != null)
                contents.put(type, content);
        }
        return contents;
    }

    private Map<DirectingType, Object> getP2PDirectingContents() {
        Map<DirectingType, Object> contents = new HashMap<>(Const.DEFAULT_DIRECTING_P2P_TABLES.size());
        for (DirectingType type: Const.DEFAULT_DIRECTING_P2P_TABLES.keySet()) {
            JsonObject content = mExtHelper.getP2PDIRTableContent(type);
            if (content != null)
                contents.put(type, content);
        }
        return contents;
    }

    private boolean isLocalExtValid() {
        TSRole role = mEPHelper.getDefaultVideoExt();
        if (role == null || role == TSRole.None)
            return false;
        return mSources2.containsKey(role);
    }

    private void asyncCall(String action, Callback callback, Runnable3 runnable) {
        if (isReady()) {
            mRTWorker.post(() -> {
                if (isReady()) {
                    Result result = runnable.run();
                    CoreUtils.callbackResult(callback, result);
                }
            });
        } else {
            Result result = genError(action, BaseError.ACTION_ILLEGAL, "init first", "非法操作,请先初始化");
            CoreUtils.callbackResult(mRTWorker, callback, result);
        }
    }

    private void asyncCall(Runnable runnable) {
        if (isReady()) {
            mRTWorker.post(() -> {
                if (isReady())
                    runnable.run();
            });
        }
    }

    ///////////////// static utils

    private static boolean isMute(float volume) {
        return volume <= 0.01f;
    }

    private static void logAction(String action, Object... params) {
        Const.logAction(CoreUtils.TAG, TAG, action, params);
    }

    private static Result genError(String action, int error, String message, String hint) {
        LogUtil.w(CoreUtils.TAG, TAG, action + ":" + message);
        Const.gMessageBox.add("ERROR", TAG + "::" + action, hint);
        return new Result(error, hint);
    }

    private static Result genError2(String action, int error, String message, String hint) {
        LogUtil.i(CoreUtils.TAG, TAG, action + ":" + message);
        return new Result(error, hint);
    }
}
