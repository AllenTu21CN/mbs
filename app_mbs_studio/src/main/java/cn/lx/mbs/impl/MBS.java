package cn.lx.mbs.impl;

import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.view.SurfaceHolder;

import com.google.gson.JsonObject;

import cn.lx.mbs.impl.base.Basics;
import cn.lx.mbs.impl.core.EndpointMBS;
import cn.lx.mbs.impl.core.ExtHelper;
import cn.lx.mbs.impl.core.utils.EPX200Mock;
import cn.lx.mbs.impl.core.utils.EPX200SPHelper;
import cn.lx.mbs.impl.db.CallingHistory;
import cn.lx.mbs.impl.db.CallingHistoryTable;
import cn.lx.mbs.impl.db.Template;
import cn.lx.mbs.impl.db.TemplateTable;
import cn.lx.mbs.impl.db.Terminal;
import cn.lx.mbs.impl.db.TerminalTable;
import cn.lx.mbs.impl.base.DIRTableMgr;
import cn.lx.mbs.impl.structures.BasicSummary;
import cn.lx.mbs.impl.structures.AudioChannelId;
import cn.lx.mbs.impl.structures.AudioCodec;
import cn.lx.mbs.impl.structures.EPStatus;
import cn.lx.mbs.impl.structures.LicenseSummary;
import cn.lx.mbs.impl.structures.CallingInfo;
import cn.lx.mbs.impl.structures.CallingParam;
import cn.lx.mbs.impl.structures.CStreamType;
import cn.lx.mbs.impl.structures.DirectingTableInfo;
import cn.lx.mbs.impl.structures.DirectingType;
import cn.lx.mbs.impl.structures.DirectingMode;
import cn.lx.mbs.impl.structures.DisplayId;
import cn.lx.mbs.impl.structures.DisplayLayout;
import cn.lx.mbs.impl.structures.EPAudioCapability;
import cn.lx.mbs.impl.structures.EPBaseConfig;
import cn.lx.mbs.impl.structures.LRState2;
import cn.lx.mbs.impl.structures.TSLayout;
import cn.lx.mbs.impl.structures.EPState;
import cn.lx.mbs.impl.structures.EPVideoCapability;
import cn.lx.mbs.impl.structures.LRTitleStyle;
import cn.lx.mbs.impl.structures.LRCodec;
import cn.lx.mbs.impl.structures.LRId;
import cn.lx.mbs.impl.structures.LRSSStrategy;
import cn.lx.mbs.impl.structures.LRState;
import cn.lx.mbs.impl.structures.LogConfig;
import cn.lx.mbs.impl.structures.PTZConfig;
import cn.lx.mbs.impl.structures.RecFile;
import cn.lx.mbs.impl.structures.RoomNameStyle;
import cn.lx.mbs.impl.structures.Source;
import cn.lx.mbs.impl.structures.SourceId;
import cn.lx.mbs.impl.structures.TSMode;
import cn.lx.mbs.impl.structures.TSRole;
import cn.lx.mbs.impl.structures.TemplateInfo;
import cn.lx.mbs.impl.structures.TerminalInfo;
import cn.lx.mbs.impl.structures.TrackingConfig;

import cn.lx.mbs.ui.service.agent.TSSettingsAgent;
import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.NetInfo;
import com.sanbu.base.Result;
import com.sanbu.base.Runnable3;
import com.sanbu.base.State;
import com.sanbu.base.Tuple;
import com.sanbu.tools.DBUtil;
import com.sanbu.tools.EventPub;
import com.sanbu.tools.FileSaveUtil;
import com.sanbu.tools.LocalLinuxUtil;
import com.sanbu.tools.LogCollector;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.LooperHelper;
import com.sanbu.tools.PackageUtil;
import com.sanbu.tools.SPUtil;
import com.sanbu.tools.StringUtil;
import com.sanbu.tools.TimeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sanbu.avalon.endpoint3.ext.camera.CameraDirective;
import cn.sanbu.avalon.endpoint3.ext.camera.ZoomAction;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.CallingUrl;
import cn.sanbu.avalon.endpoint3.structures.CallingStatistics;
import cn.sanbu.avalon.endpoint3.structures.jni.EPDir;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.MediaStatistics;
import cn.sanbu.avalon.endpoint3.structures.jni.RegConfig;
import cn.sanbu.ts.messenger.common.FileSyncAgent;

// MBS后台功能入口
public class MBS implements EndpointMBS.Observer {

    private static final String TAG = MBS.class.getSimpleName();

    public static void initEnv(Context context) {
        // init endpoint environment
        // EndpointX200.initEnv(context);
        // delay to init epx200 env while init endpoint instance

        // mkdirs
        new File(Const.SP_PATH).mkdirs();
        new File(Const.TEMP_PATH).mkdirs();
        new File(Const.LOG_PATH).mkdirs();
        new File(Const.DB_PATH).mkdirs();
        new File(Const.UPGRADE_PATH).mkdirs();
        new File(Const.RESOURCE_PATH).mkdirs();

        // save resources to storage
        saveResToStorage(context);
        saveAssetToStorage(context);

        // remove discarded resources
        removeResFromStorage();
    }

    private static MBS sInstance = new MBS();

    public static MBS getInstance() {
        return sInstance;
    }

    private Context mContext;
    private SPUtil mSPUtil;
    private LooperHelper mLooperHelper;
    private Handler mWorkHandler;
    private ExtHelper mHelper;

    private EPX200SPHelper mEPX200SPHelper;
    private Basics mBasics;
    private EndpointMBS mEndpointMBS;
    private EPX200Mock mEPX200Mock;

    private SurfaceHolder mMainSurfaceHolder;
    private int mMainSurfaceFormat;
    private int mMainSurfaceWidth;
    private int mMainSurfaceHeight;

    private MBS() {
        initHelper();
    }

    public int init(Context context) {
        if (mContext == null) {
            mContext = context;
            mSPUtil = new SPUtil(Const.SP_NAMESPACE);
            mLooperHelper = new LooperHelper(Const.CHILD_THREAD_NAME);
            mLooperHelper.startLoopInNewThreadUntilReady();
            mWorkHandler = mLooperHelper.getHandler();

            mEPX200SPHelper = new EPX200SPHelper(mSPUtil);
            mBasics = new Basics();
            mEPX200Mock = new EPX200Mock(mWorkHandler, mEPX200SPHelper);

            mMainSurfaceHolder = null;
            mMainSurfaceFormat = -1;
            mMainSurfaceWidth = -1;
            mMainSurfaceHeight = -1;

            // init error recorder
            Const.gMessageBox.init(mWorkHandler);

            // init basics module
            mBasics.init(mContext, mSPUtil, mWorkHandler);

            // init directingTable
            mDIRTableMgr = new DIRTableMgr();
            mDIRTableMgr.init(context, mSPUtil, mWorkHandler);

            DBUtil dbUtil = new DBUtil(Const.DB_PATH + "/" + Const.DB_NAME, Const.DB_VERSION, Const.DB_TABLE_LIST);
            mCallingHistoryTable = dbUtil.getTable(CallingHistoryTable.class);
            mTerminalTable = dbUtil.getTable(TerminalTable.class);
            mTemplateTable = dbUtil.getTable(TemplateTable.class);
            mCallingHistoryTable.setMaxCount(50, 40);

            // init TSSettingsAgent
            int ret = mSettingsAgent.init(mContext, false);
            if (ret != 0) {
                release();
                return ret;
            }

            // get all sources from settings to init epx200 mock
            mSettingsAgent.getAvailableSources(result -> {
                List<Source> sources = result.isSuccessful() ?
                        (List<Source>) result.data : new ArrayList<>();
                mEPX200Mock.setAvailableSources(sources);
            });

            // init FileSyncAgent
            ret = mFileSyncAgent.init(mContext, false);
            if (ret != 0) {
                release();
                return ret;
            }
        }
        return 0;
    }

    public void release() {
        LogUtil.w(Const.TAG, "TSX200 release IN");

        if (mEndpointMBS != null) {
            mEndpointMBS.release();
            mEndpointMBS = null;
        }

        if (mBasics != null) {
            mBasics.release();
            mBasics = null;
        }

        if (mEPX200SPHelper != null) {
            mEPX200SPHelper.release();
            mEPX200SPHelper = null;
        }

        if (mSettingsAgent != null) {
            mSettingsAgent.release();
            mSettingsAgent = null;
        }

        if (mFileSyncAgent != null) {
            mFileSyncAgent.release();
            mFileSyncAgent = null;
        }

        if (mEPX200Mock != null) {
            mEPX200Mock.release();
            mEPX200Mock = null;
        }

        if (mLooperHelper != null)
            mLooperHelper.stopTheLoopThreadUntilOver(true);

        mContext = null;
        mSPUtil = null;
        mWorkHandler = null;
        mLooperHelper = null;

        mMainSurfaceHolder = null;
        mMainSurfaceFormat = -1;
        mMainSurfaceWidth = -1;
        mMainSurfaceHeight = -1;

        LogUtil.w(Const.TAG, "TSX200 release OUT");
    }

    public int startEP(Context context) {
        if (mWorkHandler == null)
            return BaseError.ACTION_ILLEGAL;

        if (mEndpointMBS == null) {
            // init endpoint
            TSMode mode = mBasics.getRealTSMode();
            int ret = EndpointMBS.getInstance().init(mode, mEPX200SPHelper, mWorkHandler, this, context, mHelper);
            if (ret != 0) {
                release();
                return ret;
            }

            mEndpointMBS = EndpointMBS.getInstance();

            // get all sources from settings to init epx200
            mSettingsAgent.getAvailableSources(result -> {
                List<Source> sources = result.isSuccessful() ?
                        (List<Source>) result.data : new ArrayList<>();
                mEndpointMBS.setAvailableSources(sources);
            });

            if (mMainSurfaceHolder != null)
                onMainSurfaceCreated(mMainSurfaceHolder);
            if (mMainSurfaceWidth > 0)
                onMainSurfaceChanged(mMainSurfaceFormat, mMainSurfaceWidth, mMainSurfaceHeight);
        }
        return 0;
    }

    public void onMainSurfaceCreated(SurfaceHolder holder) {
        mMainSurfaceHolder = holder;

        if (mEndpointMBS != null)
            mEndpointMBS.onMainDisplayCreated(holder.getSurface());
    }

    public void onMainSurfaceChanged(int format, int width, int height) {
        mMainSurfaceFormat = format;
        mMainSurfaceWidth = width;
        mMainSurfaceHeight = height;

        if (mEndpointMBS != null)
            mEndpointMBS.onMainDisplayChanged(format, width, height);
    }

    public void onMainSurfaceDestroyed() {
        mMainSurfaceHolder = null;
        mMainSurfaceFormat = -1;
        mMainSurfaceWidth = -1;
        mMainSurfaceHeight = -1;

        if (mEndpointMBS != null)
            mEndpointMBS.onMainDisplayDestroyed();
    }

    /////////////////////////////// 基础功能

    public BasicSummary getBasicSummary() {
        if (mBasics == null)
            return BasicSummary.buildEmpty(Const.DEFAULT_DEVICE_NAME);
        else
            return mBasics.getBasicSummary();
    }

    public Result setDeviceName(String name) {
        if (mBasics == null)
            return new Result(BaseError.ACTION_ILLEGAL, "非法操作,未初始化");
        else
            return mBasics.setDeviceName(name);
    }

    public Result lockDeviceName(boolean enable) {
        if (mBasics == null)
            return new Result(BaseError.ACTION_ILLEGAL, "非法操作,未初始化");
        else
            return mBasics.lockDeviceName(enable);
    }

    public LicenseSummary getLicenseSummary() {
        if (mBasics == null)
            return LicenseSummary.buildEmpty("N/A");
        else
            return mBasics.getLicenseSummary();
    }

    public Result loadLicenseByCode(String code) {
        if (getEPState() != EPState.Idle)
            return new Result(BaseError.INTERNAL_ERROR, "设备正在使用，请稍后再试", null);
        else if (mBasics == null)
            return new Result(BaseError.ACTION_ILLEGAL, "非法操作,未初始化");
        else
            return mBasics.loadLicenseByCode(code);
    }

    public boolean isGranted() {
        if (mBasics == null)
            return false;
        else
            return mBasics.isGranted();
    }

    public String getSPContent() {
        if (mBasics == null)
            return "";
        else
            return mBasics.getSPContent();
    }

    public Result setSPContent(String content) {
        if (mBasics == null)
            return new Result(BaseError.ACTION_ILLEGAL, "非法操作,未初始化");
        else
            return mBasics.setSPContent(content);
    }

    public Result resetSP() {
        if (mBasics == null)
            return new Result(BaseError.ACTION_ILLEGAL, "非法操作,未初始化");
        else
            return mBasics.resetSP();
    }

    public LogConfig getLogConfig() {
        if (mBasics != null)
            return mBasics.getLogConfig();

        LogCollector.Config config = LogCollector.Config.buildDefault(Const.LOG_PATH, PackageUtil.getPackageName(mContext));
        return new LogConfig(LogConfig.Switch.DisabledAlways, config);
    }

    public void setLogConfig(LogConfig config, Callback callback) {
        if (mBasics == null)
            Const.callbackError(callback, "setLogConfig", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mBasics.setLogConfig(config, callback);
    }

    public void cleanLogs(boolean today, Callback callback) {
        if (mBasics == null)
            Const.callbackError(callback, "cleanLogs", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mBasics.cleanLogs(today, callback);
    }

    public void zipLogs(boolean today, Callback callback) {
        if (mBasics == null)
            Const.callbackError(callback, "zipLogs", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mBasics.zipLogs(today, callback);
    }

    public NetInfo getNetworkInfo() {
        if (mBasics == null)
            return NetInfo.buildEmpty();
        else
            return mBasics.getNetworkInfo();
    }

    /////////////////////////////// 视频设置接口

    public List<Source> getAvailableSources() {
        return getEPX200(() -> mEndpointMBS.getAvailableSources(),
                () -> mEPX200Mock.getAvailableSources(),
                () -> Collections.emptyList());
    }

    // 获取角色绑定的源ID
    public Map<TSRole, SourceId> getRoleBoundSourceIds() {
        return getEPX200(() -> mEndpointMBS.getRoleBoundSourceIds(),
                () -> mEPX200Mock.getRoleBoundSourceIds(),
                () -> Collections.emptyMap());
    }

    // 获取角色绑定的源
    public Map<TSRole, Source> getRoleBoundSources() {
        return getEPX200(() -> mEndpointMBS.getRoleBoundSources(),
                () -> mEPX200Mock.getRoleBoundSources(),
                () -> Collections.emptyMap());
    }

    // 获取已绑定的角色
    public List<TSRole> getBoundRoles() {
        return getEPX200(() -> mEndpointMBS.getBoundRoles(),
                () -> mEPX200Mock.getBoundRoles(),
                () -> Collections.emptyList());
    }

    // 设置角色绑定
    public void setRoleBindings(Map<TSRole, SourceId> bindings, Callback callback) {
        callEPX200(() -> mEndpointMBS.setRoleBindings(bindings, callback),
                () -> mEPX200Mock.setRoleBindings(bindings, callback),
                () -> illegalAction(callback));
    }

    // 获取双屏异显开关
    public boolean getExtendedDisplaySwitch() {
        return getEPX200(() -> mEndpointMBS.getExtendedDisplaySwitch(),
                () -> mEPX200Mock.getExtendedDisplaySwitch(),
                () -> false);
    }

    // 设置双屏异显开关
    public void setExtendedDisplaySwitch(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.setExtendedDisplaySwitch(onOff, callback),
                () -> mEPX200Mock.setExtendedDisplaySwitch(onOff, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 音频设置接口

    // 获取音频指定通道的默认音量
    public float getAudioDefaultVolume(AudioChannelId id) {
        return getEPX200(() -> mEndpointMBS.getAudioDefaultVolume(id),
                () -> mEPX200Mock.getAudioDefaultVolume(id),
                () -> 0.0f);
    }

    // 设置音频指定通道的默认音量
    public void setAudioDefaultVolume(AudioChannelId id, float volume, Callback callback) {
        callEPX200(() -> mEndpointMBS.setAudioDefaultVolume(id, volume, callback),
                () -> mEPX200Mock.setAudioDefaultVolume(id, volume, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 导播设置接口

    // 获取默认导播模式
    public DirectingMode getDefaultDirectingMode(DirectingType type) {
        return getEPX200(() -> mEndpointMBS.getDefaultDirectingMode(type),
                () -> mEPX200Mock.getDefaultDirectingMode(type),
                () -> DirectingMode.Manual);
    }

    // 设置默认导播模式
    public void setDefaultDirectingMode(DirectingType type, DirectingMode mode, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultDirectingMode(type, mode, callback),
                () -> mEPX200Mock.setDefaultDirectingMode(type, mode, callback),
                () -> illegalAction(callback));
    }

    // 获取默认跟踪开关
    public boolean getDefaultTrackingSwitch() {
        return getEPX200(() -> mEndpointMBS.getDefaultTrackingSwitch(),
                () -> mEPX200Mock.getDefaultTrackingSwitch(),
                () -> false);
    }

    // 设置默认跟踪开关
    public void setDefaultTrackingSwitch(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultTrackingSwitch(onOff, callback),
                () -> mEPX200Mock.setDefaultTrackingSwitch(onOff, callback),
                () -> illegalAction(callback));
    }

    // 获取默认课件监测开关
    public boolean getDefaultCoursewareCheckingSwitch() {
        return getEPX200(() -> mEndpointMBS.getDefaultCoursewareCheckingSwitch(),
                () -> mEPX200Mock.getDefaultCoursewareCheckingSwitch(),
                () -> false);
    }

    // 设置默认课件监测开关
    public void setDefaultCoursewareCheckingSwitch(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultCoursewareCheckingSwitch(onOff, callback),
                () -> mEPX200Mock.setDefaultCoursewareCheckingSwitch(onOff, callback),
                () -> illegalAction(callback));
    }

    // 获取云台配置
    public PTZConfig getPTZConfig() {
        return getEPX200(() -> mEndpointMBS.getPTZConfig(),
                () -> mEPX200Mock.getPTZConfig(),
                () -> Const.EMPTY_PTZ_CONFIG);
    }

    // 设置云台配置
    public void setPTZConfig(PTZConfig config, Callback callback) {
        callEPX200(() -> mEndpointMBS.setPTZConfig(config, callback),
                () -> mEPX200Mock.setPTZConfig(config, callback),
                () -> illegalAction(callback));
    }

    // 获取跟踪配置
    public TrackingConfig getTrackingConfig() {
        return getEPX200(() -> mEndpointMBS.getTrackingConfig(),
                () -> mEPX200Mock.getTrackingConfig(),
                () -> Const.EMPTY_TRACKING_CONFIG);
    }

    // 设置跟踪配置
    public void setTrackingConfig(TrackingConfig config, Callback callback) {
        callEPX200(() -> mEndpointMBS.setTrackingConfig(config, callback),
                () -> mEPX200Mock.setTrackingConfig(config, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 导播表管理接口

    // 获取导播表路径
    public String getDefaultDirectingTablePath() {
        return Const.DIRECTOR_PATH;
    }

    // 获取默认导播表(空闲)
    public DirectingTableInfo getDefaultIdleDirectingTable(DirectingType type) {
        if (mDIRTableMgr == null)
            return null;
        else
            return mDIRTableMgr.getDefaultIdleTable(type);
    }

    // 设置默认导播表(空闲)(下次生效)
    public void setDefaultIdleDirectingTable(DirectingType type, DirectingTableInfo table, Callback callback) {
        if (mDIRTableMgr == null)
            Const.callbackError(callback, "setDefaultIdleDirectingTable", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mDIRTableMgr.setDefaultIdleTable(type, table, callback);
    }

    // 获取默认导播表(单点互动)
    public DirectingTableInfo getDefaultP2PDirectingTable(DirectingType type) {
        if (mDIRTableMgr == null)
            return null;
        else
            return mDIRTableMgr.getDefaultP2PTable(type);
    }

    // 设置默认导播表(单点互动)(下次生效)
    public void setDefaultP2PDirectingTable(DirectingType type, DirectingTableInfo table, Callback callback) {
        if (mDIRTableMgr == null)
            Const.callbackError(callback, "setDefaultP2PDirectingTable", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mDIRTableMgr.setDefaultP2PTable(type, table, callback);
    }

    // 获取默认导播表(多点互动)
    public DirectingTableInfo getDefaultMeetingDirectingTable(DirectingType type) {
        if (mDIRTableMgr == null)
            return null;
        else
            return mDIRTableMgr.getDefaultMeetingTable(type);
    }

    // 设置默认导播表(多点互动)(下次生效)
    public void setDefaultMeetingDirectingTable(DirectingType type, DirectingTableInfo table, Callback callback) {
        if (mDIRTableMgr == null)
            Const.callbackError(callback, "setDefaultMeetingDirectingTable", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mDIRTableMgr.setDefaultMeetingTable(type, table, callback);
    }

    // 获取指定类型支持的所有导播表信息
    public List<DirectingTableInfo> getAllDirectingTables(DirectingType type) {
        if (mDIRTableMgr == null)
            return Collections.emptyList();
        else
            return mDIRTableMgr.getAllTables(type);
    }

    // 获取指定类型支持的所有导播表信息(带过滤)
    public List<DirectingTableInfo> getAllDirectingTables(DirectingType type, String filter) {
        if (mDIRTableMgr == null)
            return Collections.emptyList();
        else
            return mDIRTableMgr.getAllTables(type, filter);
    }

    // 获取支持的所有导播表信息
    public List<DirectingTableInfo> getAllDirectingTables() {
        if (mDIRTableMgr == null)
            return Collections.emptyList();
        else
            return mDIRTableMgr.getAllTables();
    }

    // 添加导播表
    public void addDirectingTable(String fullPath, Callback callback) {
        if (mDIRTableMgr == null)
            Const.callbackError(callback, "addDirectingTable", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mDIRTableMgr.addTable(fullPath, callback);
    }

    // 删除导播表
    public void deleteDirectingTable(String fullPath, Callback callback) {
        if (mDIRTableMgr == null)
            Const.callbackError(callback, "deleteDirectingTable", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,未初始化");
        else
            mDIRTableMgr.deleteTable(fullPath, callback);
    }

    /////////////////////////////// 直播录制配置接口

    // 获取直播录制默认编码参数
    public LRCodec getDefaultLRCodec() {
        return getEPX200(() -> mEndpointMBS.getDefaultLRCodec(),
                () -> mEPX200Mock.getDefaultLRCodec(),
                () -> Const.DEFAULT_LR_CODEC);
    }

    // 设置直播录制默认编码参数
    public void setDefaultLRCodec(LRCodec codec, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultLRCodec(codec, callback),
                () -> mEPX200Mock.setDefaultLRCodec(codec, callback),
                () -> illegalAction(callback));
    }

    // 获取直播录制默认标题参数
    public LRTitleStyle getDefaultLRTitleStyle() {
        return getEPX200(() -> mEndpointMBS.getDefaultLRTitleStyle(),
                () -> mEPX200Mock.getDefaultLRTitleStyle(),
                () -> Const.DEFAULT_LR_TITLE_STYLE);
    }

    // 设置直播录制默认标题参数
    public void setDefaultLRTitleStyle(LRTitleStyle style, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultLRTitleStyle(style, callback),
                () -> mEPX200Mock.setDefaultLRTitleStyle(style, callback),
                () -> illegalAction(callback));
    }

    // 获取录制文件属性
    public RecFile getRecordingFile() {
        return getEPX200(() -> mEndpointMBS.getRecordingFile(),
                () -> mEPX200Mock.getRecordingFile(),
                () -> Const.DEFAULT_RECORDING_FILE);
    }

    // 设置录制文件属性
    public void setRecordingFile(RecFile file, Callback callback) {
        callEPX200(() -> mEndpointMBS.setRecordingFile(file, callback),
                () -> mEPX200Mock.setRecordingFile(file, callback),
                () -> illegalAction(callback));
    }

    // 获取直播录制地址
    public String getLRUrl(LRId id) {
        return getEPX200(() -> mEndpointMBS.getLRUrl(id),
                () -> mEPX200Mock.getLRUrl(id),
                () -> "");
    }

    // 获取(可用的)直播地址
    public String getRunningLiveUrl() {
        return getEPX200(() -> mEndpointMBS.getRunningLiveUrl(),
                () -> mEPX200Mock.getRunningLiveUrl(),
                () -> "");
    }

    // 设置录制文件名
    public void setRecordingFilename(String filename, Callback callback) {
        callEPX200(() -> mEndpointMBS.setRecordingFilename(filename, callback),
                () -> mEPX200Mock.setRecordingFilename(filename, callback),
                () -> illegalAction(callback));
    }

    // 设置集成推流地址
    public void setIntegratedPushingUrl(String url, Callback callback) {
        callEPX200(() -> mEndpointMBS.setIntegratedPushingUrl(url, callback),
                () -> mEPX200Mock.setIntegratedPushingUrl(url, callback),
                () -> illegalAction(callback));
    }

    // 设置固定推流地址
    public void setDefaultFixedUrl(String url, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultFixedUrl(url, callback),
                () -> mEPX200Mock.setDefaultFixedUrl(url, callback),
                () -> illegalAction(callback));
    }

    // 获取直播录制启停策略
    public LRSSStrategy getLRSSStrategy(LRId id) {
        return getEPX200(() -> mEndpointMBS.getLRSSStrategy(id),
                () -> mEPX200Mock.getLRSSStrategy(id),
                () -> LRSSStrategy.Forbidden);
    }

    // 设置直播录制启停策略
    public void setLRSSStrategy(LRId id, LRSSStrategy strategy, Callback callback) {
        callEPX200(() -> mEndpointMBS.setLRSSStrategy(id, strategy, callback),
                () -> mEPX200Mock.setLRSSStrategy(id, strategy, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 呼叫设置接口

    // 获取会场名默认配置
    public RoomNameStyle getDefaultRoomNameStyle() {
        return getEPX200(() -> mEndpointMBS.getDefaultRoomNameStyle(),
                () -> mEPX200Mock.getDefaultRoomNameStyle(),
                () -> Const.DEFAULT_ROOM_NAME_STYLE);
    }

    // 设置会场名默认配置
    public void setDefaultRoomNameStyle(RoomNameStyle style, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDefaultRoomNameStyle(style, callback),
                () -> mEPX200Mock.setDefaultRoomNameStyle(style, callback),
                () -> illegalAction(callback));
    }

    // 获取呼叫流类型绑定
    public Map<CStreamType, TSRole> getCStreamBindings() {
        return getEPX200(() -> mEndpointMBS.getCStreamBindings(),
                () -> mEPX200Mock.getCStreamBindings(),
                () -> new HashMap() {{
                    put(CStreamType.VideoExt, TSRole.None);
                }});
    }

    // 设置呼叫流类型绑定
    public void setCStreamBindings(Map<CStreamType, TSRole> bindings, Callback callback) {
        callEPX200(() -> mEndpointMBS.setCStreamBindings(bindings, callback),
                () -> mEPX200Mock.setCStreamBindings(bindings, callback),
                () -> illegalAction(callback));
    }

    // 获取呼叫基础配置
    public final EPBaseConfig getEPBaseConfig() {
        return getEPX200(() -> mEndpointMBS.getEPBaseConfig(),
                () -> mEPX200Mock.getEPBaseConfig(),
                () -> Const.DEFAULT_EP_BASE_CONFIG);
    }

    // 设置呼叫基础配置
    public void setEPBaseConfig(EPBaseConfig config, Callback callback) {
        callEPX200(() -> mEndpointMBS.setEPBaseConfig(config, callback),
                () -> mEPX200Mock.setEPBaseConfig(config, callback),
                () -> illegalAction(callback));
    }

    // 获取呼叫固定配置
    public final EPFixedConfig getEPFixedConfig() {
        return getEPX200(() -> mEndpointMBS.getEPFixedConfig(),
                () -> mEPX200Mock.getEPFixedConfig(),
                () -> Const.DEFAULT_EP_FIXED_CONFIG);
    }

    // 设置呼叫固定配置
    public void setEPFixedConfig(EPFixedConfig config, Callback callback) {
        callEPX200(() -> mEndpointMBS.setEPFixedConfig(config, callback),
                () -> mEPX200Mock.setEPFixedConfig(config, callback),
                () -> illegalAction(callback));
    }

    // 获取呼叫视频能力
    public final EPVideoCapability getEPVideoCapability() {
        return getEPX200(() -> mEndpointMBS.getEPVideoCapability(),
                () -> mEPX200Mock.getEPVideoCapability(),
                () -> Const.DEFAULT_EP_VIDEO_CAPABILITY);
    }

    // 设置呼叫视频能力
    public void setEPVideoCapability(EPVideoCapability capability, Callback callback) {
        callEPX200(() -> mEndpointMBS.setEPVideoCapability(capability, callback),
                () -> mEPX200Mock.setEPVideoCapability(capability, callback),
                () -> illegalAction(callback));
    }

    // 获取呼叫音频能力
    public final EPAudioCapability getEPAudioCapability() {
        return getEPX200(() -> mEndpointMBS.getEPAudioCapability(),
                () -> mEPX200Mock.getEPAudioCapability(),
                () -> Const.DEFAULT_EP_AUDIO_CAPABILITY);
    }

    // 设置呼叫音频能力
    public void setEPAudioCapability(EPAudioCapability capability, Callback callback) {
        callEPX200(() -> mEndpointMBS.setEPAudioCapability(capability, callback),
                () -> mEPX200Mock.setEPAudioCapability(capability, callback),
                () -> illegalAction(callback));
    }

    // 获取呼叫支持的所有音频格式
    public List<AudioCodec> getSupportingCallingAudioCodecs() {
        return getEPX200(() -> mEndpointMBS.getSupportingCallingAudioCodecs(),
                () -> mEPX200Mock.getSupportingCallingAudioCodecs(),
                () -> Const.DEFAULT_EP_AUDIO_CAPABILITY.codecs);
    }

    /////////////////////////////// 注册接口

    // 获取GK注册状态
    public final State getGKRegState() {
        return getEPX200(() -> mEndpointMBS.getGKRegState(),
                () -> mEPX200Mock.getGKRegState(),
                () -> State.None);
    }

    // 获取GK注册配置
    public final RegConfig.H323 getGKRegConfig() {
        return getEPX200(() -> mEndpointMBS.getGKRegConfig(),
                () -> mEPX200Mock.getGKRegConfig(),
                () -> Const.EMPTY_GK_REG_CONFIG);
    }

    // GK注册
    public void registerGK(RegConfig.H323 config, Callback callback) {
        callEPX200(() -> mEndpointMBS.registerGK(config, callback),
                () -> mEPX200Mock.registerGK(config, callback),
                () -> illegalAction(callback));
    }

    // GK注销
    public void unregisterGK(Callback callback) {
        callEPX200(() -> mEndpointMBS.unregisterGK(callback),
                () -> mEPX200Mock.unregisterGK(callback),
                () -> illegalAction(callback));
    }

    // 获取SIP注册状态
    public final State getSIPRegState() {
        return getEPX200(() -> mEndpointMBS.getSIPRegState(),
                () -> mEPX200Mock.getSIPRegState(),
                () -> State.None);
    }

    // 获取SIP注册配置
    public final RegConfig.SIP getSIPRegConfig() {
        return getEPX200(() -> mEndpointMBS.getSIPRegConfig(),
                () -> mEPX200Mock.getSIPRegConfig(),
                () -> Const.EMPTY_SIP_REG_CONFIG);
    }

    // SIP注册
    public void registerSIP(RegConfig.SIP config, Callback callback) {
        callEPX200(() -> mEndpointMBS.registerSIP(config, callback),
                () -> mEPX200Mock.registerSIP(config, callback),
                () -> illegalAction(callback));
    }

    // SIP注销
    public void unregisterSIP(Callback callback) {
        callEPX200(() -> mEndpointMBS.unregisterSIP(callback),
                () -> mEPX200Mock.unregisterSIP(callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 互动控制通用接口

    // 获取互动可用状态
    public boolean isEPReady() {
        return mEndpointMBS != null;
    }

    // 获取当前互动完整状态
    public EPStatus getEPStatus() {
        return getEPX200(() -> mEndpointMBS.getEPStatus(),
                () -> EPStatus.buildIdle());
    }

    // 获取当前互动状态
    public EPState getEPState() {
        return getEPX200(() -> mEndpointMBS.getEPState(),
                () -> EPState.Idle);
    }

    // 获取当前互动持续时间(秒)
    public long getEPDurationMS() {
        return getEPX200(() -> mEndpointMBS.getEPDurationMS(),
                () -> 0l);
    }

    // 获取当前互动的可控制单元(本地/远程)
    public void getTSUnits(Callback/*List<TSUnit>*/ callback) {
        callEPX200(() -> mEndpointMBS.getTSUnits(callback),
                () -> nonEPAction(callback));
    }

    // 获取当前互动的可控制单元(远程)
    public void getRemoteUnits(Callback/*List<TSUnit>*/ callback) {
        callEPX200(() -> mEndpointMBS.getRemoteUnits(callback),
                () -> nonEPAction(callback));
    }

    // 获取指定显示的默认布局
    public void getDefaultLayout(DisplayId id, TSLayout layout, Callback/*List<TSUnit>*/ callback) {
        callEPX200(() -> mEndpointMBS.getDefaultLayout(id, layout, callback),
                () -> nonEPAction(callback));
    }

    // 获取当前导播模式
    public DirectingMode getCurrentDirectingMode(DirectingType type) {
        return getEPX200(() -> mEndpointMBS.getCurrentDirectingMode(type),
                () -> DirectingMode.Manual);
    }

    // 设置当前导播模式
    public void setCurrentDirectingMode(DirectingType type, DirectingMode mode, Callback callback) {
        callEPX200(() -> mEndpointMBS.setCurrentDirectingMode(type, mode, callback),
                () -> nonEPAction(callback));
    }

    // 获取当前跟踪开关
    public boolean getCurrentTrackingSwitch() {
        return getEPX200(() -> mEndpointMBS.getCurrentTrackingSwitch(),
                () -> false);
    }

    // 设置当前跟踪开关
    public void setCurrentTrackingSwitch(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.setCurrentTrackingSwitch(onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取直播录制状态
    public LRState getLRState(LRId id) {
        return getEPX200(() -> mEndpointMBS.getLRState(id),
                () -> LRState.Stop);
    }

    // 获取直播录制状态2(状态和持续时间)
    public LRState2 getLRState2(LRId id) {
        return getEPX200(() -> mEndpointMBS.getLRState2(id),
                () -> new LRState2(LRState.Stop, 0l));
    }

    // 切换直播录制状态
    public void switchLRState(LRId id, LRState state, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchLRState(id, state, callback),
                () -> nonEPAction(callback));
    }

    // 获取显示内容
    public DisplayLayout getDisplayLayout(DisplayId id) {
        return getEPX200(() -> mEndpointMBS.getDisplayLayout(id),
                () -> DisplayLayout.buildEmpty());
    }

    // 设置显示内容
    public void setDisplayLayout(DisplayId id, DisplayLayout layout, Callback callback) {
        callEPX200(() -> mEndpointMBS.setDisplayLayout(id, layout, callback),
                () -> nonEPAction(callback));
    }

    // 获取显示的预览地址
    public String getDisplayPreviewUrl(DisplayId id) {
        return getEPX200(() -> mEndpointMBS.getDisplayPreviewUrl(id),
                () -> "");
    }

    // 获取指定音频通道的静音状态
    // @param extId [IN] extra id for caller
    public boolean getAudioCurrentMute(AudioChannelId id, int extId) {
        return getEPX200(() -> mEndpointMBS.getAudioCurrentMute(id, extId),
                () -> true);
    }

    // 设置指定音频通道的静音状态
    public void setAudioCurrentMute(AudioChannelId id, int extId, boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.setAudioCurrentMute(id, extId, onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取音频指定通道的当前音量
    public float getAudioCurrentVolume(AudioChannelId id, int extId) {
        return getEPX200(() -> mEndpointMBS.getAudioCurrentVolume(id, extId),
                () -> 0.0f);
    }

    // 设置音频指定通道的当前音量
    public void setAudioCurrentVolume(AudioChannelId id, int extId, float volume, Callback callback) {
        callEPX200(() -> mEndpointMBS.setAudioCurrentVolume(id, extId, volume, callback),
                () -> nonEPAction(callback));
    }

    // 获取远程连接状态
    public State getRemoteState(int remoteId) {
        return getEPX200(() -> mEndpointMBS.getRemoteState(remoteId),
                () -> State.None);
    }

    // 切换远程连接(呼叫/挂断)
    public void switchRemoteState(int remoteId, boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchRemoteState(remoteId, onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取双流状态
    public EPDir getVideoExtState() {
        return getEPX200(() -> mEndpointMBS.getVideoExtState(),
                () -> EPDir.None);
    }

    // 切换辅流发送
    public void switchVideoExtSending(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchVideoExtSending(onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取信号源锁定状态
    public boolean isSourceLocked(TSRole role) {
        return getEPX200(() -> mEndpointMBS.isSourceLocked(role),
                () -> false);
    }

    // 切换信号源锁定状态
    public void switchSourceLock(TSRole role, boolean locked, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchSourceLock(role, locked, callback),
                () -> nonEPAction(callback));
    }

    // 获取直播录制当前标题参数
    public LRTitleStyle getCurrentLRTitleStyle() {
        return getEPX200(() -> mEndpointMBS.getCurrentLRTitleStyle(),
                () -> getDefaultLRTitleStyle());
    }

    // 设置直播录制标题当前参数
    public void setCurrentLRTitleStyle(LRTitleStyle style, Callback callback) {
        callEPX200(() -> mEndpointMBS.setCurrentLRTitleStyle(style, callback),
                () -> nonEPAction(callback));
    }

    // 获取会场名当前配置
    public RoomNameStyle getCurrentRoomNameStyle() {
        return getEPX200(() -> mEndpointMBS.getCurrentRoomNameStyle(),
                () -> getDefaultRoomNameStyle());
    }

    // 设置会场名当前配置
    public void setCurrentRoomNameStyle(RoomNameStyle style, Callback callback) {
        callEPX200(() -> mEndpointMBS.setCurrentRoomNameStyle(style, callback),
                () -> nonEPAction(callback));
    }

    // 获取会场名显示状态
    public boolean isShownRoomName() {
        return getEPX200(() -> mEndpointMBS.isShownRoomName(),
                () -> false);
    }

    // 切换会场名显示状态
    public void switchShownRoomName(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchShownRoomName(onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取媒体统计
    public void getMediaStatisticsStr(Callback/**@see MediaStatistics*/callback) {
        callEPX200(() -> mEndpointMBS.getMediaStatisticsStr(callback),
                () -> nonEPAction(callback));
    }

    // 获取消息数量
    public int getMessageRecordsCount() {
        return Const.gMessageBox.getRecordsCount();
    }

    // 获取消息
    public void getMessageRecords(int max, Callback callback) {
        Const.gMessageBox.getRecords(max, callback);
    }

    // 清除消息
    public void clearMessageRecords() {
        Const.gMessageBox.clear();
    }

    /////////////////////////////// 单点呼叫控制接口

    // 获取点对点当前呼叫参数
    public CallingParam getP2PCallingParam() {
        return getEPX200(() -> mEndpointMBS.getP2PCallingParam(),
                () -> mEPX200Mock.getP2PCallingParam(),
                () -> new CallingParam());
    }

    // 创建点对点互动
    public void createP2P(CallingParam param, Callback callback) {
        CallingInfo callingInfo = new CallingInfo(param.name, param.url, param.protocol, param.bandwidth,
                param.resolution, param.framerate, param.vCodecs, param.aCodecs, EPDir.Outgoing,
                TimeUtil.stampToDate(System.currentTimeMillis()));
        callTable(mCallingHistoryTable, null, () -> mCallingHistoryTable.insert(callingInfo));

        callEPX200(() -> mEndpointMBS.createP2P(param, callback),
                () -> nonEPAction(callback));
    }

    // 结束点对点互动
    public void releaseP2P(Callback callback) {
        callEPX200(() -> mEndpointMBS.releaseP2P(callback),
                () -> nonEPAction(callback));
    }

    // 接受点对点被叫
    public void p2pAccept(Callback callback) {
        callEPX200(() -> mEndpointMBS.p2pAccept(callback),
                () -> nonEPAction(callback));
    }

    // 拒绝点对点被叫
    public void p2pReject(Callback callback) {
        callEPX200(() -> mEndpointMBS.p2pReject(callback),
                () -> nonEPAction(callback));
    }

    // 点对点呼叫发送DTMF
    public void p2pSendDTMF(char key, Callback callback) {
        callEPX200(() -> mEndpointMBS.p2pSendDTMF(key, callback),
                () -> nonEPAction(callback));
    }

    // 获取点对点呼叫统计
    public void p2pGetStatistics(Callback/**@see CallingStatistics */callback) {
        callEPX200(() -> mEndpointMBS.p2pGetStatistics(callback),
                () -> nonEPAction(callback));
    }

    /////////////////////////////// 多点呼叫控制接口

    // 创建多点互动
    public void createMeeting(int templateId, Callback callback) {
        queryTemplate(templateId, result -> {
            if (!result.isSuccessful()) {
                Const.callbackResult(callback, result);
                return;
            }

            Template template = (Template) result.data;
            if (template == null) {
                Const.callbackError(callback, "createMeeting", BaseError.INVALID_PARAM,
                        "can not find the template by id", "无效的参数,无法找到指定课程模板");
                return;
            }

            createMeeting(template, callback);
        });
    }

    public void createMeeting(TemplateInfo info, Callback callback) {
        callEPX200(() -> {
                    info.loadDirectingContent();
                    mEndpointMBS.createMeeting(info, callback);
                },
                () -> nonEPAction(callback));
    }

    // 结束多点互动
    public void releaseMeeting(Callback callback) {
        callEPX200(() -> mEndpointMBS.releaseMeeting(callback),
                () -> nonEPAction(callback));
    }

    // 会议讨论中
    public boolean inMeetingDiscussion() {
        return getEPX200(() -> mEndpointMBS.inMeetingDiscussion(),
                () -> false);
    }

    // 切换会议讨论
    public void switchMeetingDiscussion(boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchMeetingDiscussion(onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取远程点名状态
    public boolean inMeetingSpeaking(int remoteId) {
        return getEPX200(() -> mEndpointMBS.inMeetingSpeaking(remoteId),
                () -> false);
    }

    // 切换远程点名状态
    public void switchMeetingSpeaking(int remoteId, boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchMeetingSpeaking(remoteId, onOff, callback),
                () -> nonEPAction(callback));
    }

    // 获取指定远程的呼叫统计
    public void getMeetingCallingStatistics(int remoteId, Callback/**@see CallingStatistics*/callback) {
        callEPX200(() -> mEndpointMBS.getMeetingCallingStatistics(remoteId, callback),
                () -> nonEPAction(callback));
    }

    // 获取所有远程的呼叫统计
    public void getMeetingAllCallingStatistics(Callback/**@see Map<Integer,  CallingStatistics >*/callback) {
        callEPX200(() -> mEndpointMBS.getMeetingAllCallingStatistics(callback),
                () -> nonEPAction(callback));
    }

    /////////////////////////////// 云台控制接口

    // 设置云台控制目标
    public void setPTZTarget(TSRole role, Callback callback) {
        callEPX200(() -> mEndpointMBS.setPTZTarget(role, callback),
                () -> nonEPAction(callback));
    }

    // 获取云台速度
    public int getPTZSpeed() {
        return getEPX200(() -> mEndpointMBS.getPTZSpeed(),
                () -> 0);
    }

    // 设置云台速度
    public void setPTZSpeed(int speed, Callback callback) {
        callEPX200(() -> mEndpointMBS.setPTZSpeed(speed, callback),
                () -> nonEPAction(callback));
    }

    // 移动云台
    public void movePTZ(CameraDirective dir, Callback callback) {
        callEPX200(() -> mEndpointMBS.movePTZ(dir, callback),
                () -> nonEPAction(callback));
    }

    // 对焦云台
    public void setPTZZoom(ZoomAction action, Callback callback) {
        callEPX200(() -> mEndpointMBS.setPTZZoom(action, callback),
                () -> nonEPAction(callback));
    }

    // 设置云台预置位
    public void setPTZPreset(int number, Callback callback) {
        callEPX200(() -> mEndpointMBS.setPTZPreset(number, callback),
                () -> nonEPAction(callback));
    }

    // 调用云台预置位
    public void loadPTZPreset(int number, Callback callback) {
        callEPX200(() -> mEndpointMBS.loadPTZPreset(number, callback),
                () -> nonEPAction(callback));
    }

    /////////////////////////////// 内部方法

    public void onNetworkChanged() {
        if (mBasics != null)
            mBasics.onNetworkChanged();
    }

    /////////////////////////////// 呼叫历史管理

    public void clearCallHistory(Callback callback) {
        callTable(mCallingHistoryTable, callback,
                () -> mCallingHistoryTable.clearAll());
    }

    public void queryCallHistory(String id, Callback/**@see CallingHistory*/ callback) {
        callTable(mCallingHistoryTable, callback,
                () -> Result.buildSuccess(mCallingHistoryTable.queryById(id)));
    }

    public void queryAllCallHistory(int maxCount, Callback/**@see List<CallingHistory>*/ callback) {
        callTable(mCallingHistoryTable, callback,
                () -> Result.buildSuccess(mCallingHistoryTable.enumItems(maxCount)));
    }

    /////////////////////////////// 终端信息管理

    public void addTerminal(TerminalInfo info, Callback callback) {
        callTable(mTerminalTable, callback,
                () -> mTerminalTable.insert(info));
    }

    public void updateTerminal(Terminal terminal, Callback callback) {
        callTable(mTerminalTable, callback,
                () -> mTerminalTable.update(terminal));
    }

    public void deleteTerminal(Callback callback, String... ids) {
        callTable(mTerminalTable, callback,
                () -> mTerminalTable.delete(ids));
    }

    public void queryTerminal(String id, Callback/**@see Terminal*/ callback) {
        callTable(mTerminalTable, callback,
                () -> Result.buildSuccess(mTerminalTable.queryById(id)));
    }

    public void queryAllTerminals(int maxCount, Callback/**@see List<Terminal>*/ callback) {
        callTable(mTerminalTable, callback,
                () -> Result.buildSuccess(mTerminalTable.enumItems(maxCount)));
    }

    ////////////////////////////////// 模板管理

    public void addTemplate(TemplateInfo info, Callback callback) {
        callTable(mTemplateTable, callback,
                () -> mTemplateTable.insert(info));
    }

    public void updateTemplate(Template terminal, Callback callback) {
        callTable(mTemplateTable, callback,
                () -> mTemplateTable.update(terminal));
    }

    public void deleteTemplate(Callback callback, String... ids) {
        callTable(mTemplateTable, callback,
                () -> mTemplateTable.delete(ids));
    }

    public void queryTemplate(int id, Callback/**@see Template*/ callback) {
        callTable(mTemplateTable, callback,
                () -> Result.buildSuccess(mTemplateTable.queryById(id)));
    }

    public void queryTemplates(String keyword, int maxCount, Callback/**@see List<Template>*/ callback) {
        callTable(mTemplateTable, callback,
                () -> Result.buildSuccess(mTemplateTable.queryByKeyword(keyword, maxCount)));
    }

    public void queryAllTemplates(int maxCount, Callback/**@see List<Template>*/ callback) {
        callTable(mTemplateTable, callback,
                () -> Result.buildSuccess(mTemplateTable.enumItems(maxCount)));
    }

    /////////////////////////////// implementation of EndpointX200.Observer

    @Override
    public void onRegisteringStateChanged(State state, CallingProtocol protocol) {
        if (state == State.Doing)
            state = State.None;

        EventPub.getDefaultPub().post(Events.EP_REGISTERING_STATE_CHANGED, state.value, -1, protocol);
    }

    @Override
    public void onIncomingCall(int call_id, CallingParam param) {
        CallingInfo callingInfo = new CallingInfo(param.name, param.url, param.protocol, param.bandwidth, param.resolution, param.framerate, param.vCodecs, param.aCodecs, EPDir.Incoming, TimeUtil.stampToDate(System.currentTimeMillis()));
        mCallingHistoryTable.insert(callingInfo);
    }

    @Override
    public void onStateChanged(EPState state) {
        EventPub.getDefaultPub().post(Events.EP_STATE_CHANGED, -1, -1, state);
    }

    @Override
    public void onRecordingFinished(String path) {
        if (StringUtil.isEmpty(path))
            return;
        if (path.contains("file://"))
            path = path.substring("file://".length());

        LogUtil.d(Const.TAG, TAG, "onRecordingFinished: " + path);

        final String []spChars = {";", "&", "`", "!", "@", "$", "*", "\\", ":", "?", "\"", "<", ">", "|"};
        for (String ch: spChars) {
            if (path.contains(ch)) {
                LogUtil.w(Const.TAG, TAG, "invalid recording name: " + path);
                return;
            }
        }

        path = path.replace("(", "\\(");
        path = path.replace(")", "\\)");
        path = path.replace(" ", "\\ ");

        int index = path.lastIndexOf(".");
        if (index < 0) {
            LogUtil.w(Const.TAG, TAG, "invalid recording name2: " + path);
            return;
        }

        final String prefix = path.substring(0, index);
        final String suffix = path.substring(index + 1);

        new Thread(() -> {
            if (mFileSyncAgent == null) {
                LogUtil.w(Const.TAG, TAG, "tsx200 had stopped, give up to notify file_sync_service: " + prefix);
                return;
            }

            String cmd = String.format("ls -l %s*.%s | busybox awk '{print $8}'", prefix, suffix);
            LocalLinuxUtil.Result ret = LocalLinuxUtil.doShellWithResult(cmd);
            if (ret.code != 0) {
                LogUtil.w(Const.TAG, TAG, String.format("can not find valid file(%s), give up to notify file_sync_service: %s", prefix, ret.AllToString()));
                return;
            }

            for (String file: ret.stdOut) {
                mFileSyncAgent.addFileTask("app_tsx200_hd", file, result -> {
                    if (result.isSuccessful())
                        LogUtil.i(Const.TAG, TAG, "notify file_sync_service success: " + file);
                    else
                        LogUtil.w(Const.TAG, TAG, "failed to notify file_sync_service: " + result.getMessage());
                });
            }
        }).run();
    }

    /////////////////////////////// private functions

    private void initHelper() {
        mHelper = new ExtHelper() {
            @Override
            public NetInfo getNetworkInfo() {
                return MBS.this.getNetworkInfo();
            }

            @Override
            public String getCustomName() {
                return getBasicSummary().customName;
            }

            @Override
            public Terminal getContact(CallingUrl url) {
                String str = new CallingUrl(url.protocol, null, url.address, 0, null).toString();
                return mTerminalTable.queryByUrl(str);
            }

            @Override
            public String getPublicAddress() {
                String address = SystemProperties.get("ro.sanbu.product.public.address");
                return StringUtil.isEmpty(address) ? getNetworkInfo().ip : address;
            }

            @Override
            public int getPublicPreviewPort() {
                int port = SystemProperties.getInt("ro.sanbu.product.preview.port", Const.LOCAL_RTMP_PORT);
                return port > 0 ? port : Const.LOCAL_RTMP_PORT;
            }

            @Override
            public JsonObject getIdleDIRTableContent(DirectingType type) {
                if (mDIRTableMgr == null)
                    return null;

                DirectingTableInfo table = mDIRTableMgr.getDefaultIdleTable(type);
                if (table == null)
                    return null;

                return DIRTableMgr.getDirectingContent(table.fullPath);
            }

            @Override
            public JsonObject getP2PDIRTableContent(DirectingType type) {
                if (mDIRTableMgr == null)
                    return null;

                DirectingTableInfo table = mDIRTableMgr.getDefaultP2PTable(type);
                if (table == null)
                    return null;

                return DIRTableMgr.getDirectingContent(table.fullPath);
            }
        };
    }

    private <T> T getEPX200(EPFunc<T> realFunc, EPFunc<T> mockFunc, EPFunc<T> nullFunc) {
        if (mEndpointMBS != null)
            return realFunc.run();
        else if (mEPX200Mock != null)
            return mockFunc.run();
        else
            return nullFunc.run();
    }

    private <T> T getEPX200(EPFunc<T> realFunc, EPFunc<T> nullFunc) {
        if (mEndpointMBS != null)
            return realFunc.run();
        else
            return nullFunc.run();
    }

    private void callEPX200(Runnable realFunc, Runnable mockFunc, Runnable nullFunc) {
        if (mEndpointMBS != null)
            realFunc.run();
        else if (mEPX200Mock != null)
            mockFunc.run();
        else
            nullFunc.run();
    }

    private void callEPX200(Runnable realFunc, Runnable nullFunc) {
        if (mEndpointMBS != null)
            realFunc.run();
        else
            nullFunc.run();
    }

    private interface EPFunc<T> {
        T run();
    }

    private void illegalAction(Callback callback) {
        String hint = "非法操作,请先初始化";
        LogUtil.w(Const.TAG, TAG, "init first");
        Const.gMessageBox.add("ERROR", TAG, hint);
        Const.callbackResult(callback, new Result(BaseError.ACTION_ILLEGAL, hint));
    }

    private void nonEPAction(Callback callback) {
        String hint = "不支持的操作: 未启动互动教学主程序";
        LogUtil.w(Const.TAG, TAG, "start ep first");
        Const.gMessageBox.add("ERROR", TAG, hint);
        Const.callbackResult(callback, new Result(BaseError.ACTION_ILLEGAL, hint));
    }

    private void callTable(Object table, Callback callback, Runnable3 runnable) {
        if (mWorkHandler == null) {
            illegalAction(callback);
            return;
        }

        mWorkHandler.post(() -> {
            if (table == null)
                illegalAction(callback);
            else
                Const.callbackResult(callback, runnable.run());
        });
    }

    /////////////////////////////// private static utils

    private static void saveResToStorage(Context context) {
        for (Map.Entry<String, Tuple<Integer, FileSaveUtil.Action>> resource : Const.FIXED_RESOURCE.entrySet()) {
            String toFilename = resource.getKey();
            int resId = resource.getValue().first;
            String resName = context.getResources().getResourceEntryName(resId);
            FileSaveUtil.Action action = resource.getValue().second;

            int ret = FileSaveUtil.saveResToStorage(context, resId, toFilename, action);
            if (ret == FileSaveUtil.CODE_ERROR)
                LogUtil.w(Const.TAG, TAG, String.format("save resources(%s) to storage(%s) failed", resName, toFilename));
            else if (ret == FileSaveUtil.CODE_EXISTED)
                LogUtil.i(Const.TAG, TAG, String.format("skip to save resources(%s) to storage(%s), it existed", resName, toFilename));
            else
                LogUtil.i(Const.TAG, TAG, String.format("save resources(%s) to storage(%s)", resName, toFilename));
        }
    }

    private static void saveAssetToStorage(Context context) {
        for (Map.Entry<String, Tuple<String, FileSaveUtil.Action>> asset : Const.BUILD_IN_ASSETS.entrySet()) {
            String toFilename = asset.getKey();
            String assetFile = asset.getValue().first;
            FileSaveUtil.Action action = asset.getValue().second;

            int ret = FileSaveUtil.saveAssetToStorage(context, assetFile, toFilename, action);
            if (ret == FileSaveUtil.CODE_ERROR)
                LogUtil.w(Const.TAG, TAG, String.format("save asset(%s) to storage(%s) failed", assetFile, toFilename));
            else if (ret == FileSaveUtil.CODE_EXISTED)
                LogUtil.i(Const.TAG, TAG, String.format("skip to save asset(%s) to storage(%s), it existed", assetFile, toFilename));
            else
                LogUtil.i(Const.TAG, TAG, String.format("save asset(%s) to storage(%s)", assetFile, toFilename));
        }
    }

    private static void removeResFromStorage() {
        for (String path: Const.DISCARDED_RESOURCES) {
            File file = new File(path);
            if (file.exists())
                file.delete();
        }
    }

}
