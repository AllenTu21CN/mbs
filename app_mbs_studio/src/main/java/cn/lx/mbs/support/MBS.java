package cn.lx.mbs.support;

import android.content.Context;
import android.os.Handler;
import android.view.Surface;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sanbu.base.BaseError;
import com.sanbu.base.BaseEvents;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Tuple;
import com.sanbu.tools.EventPub;
import com.sanbu.tools.FileSaveUtil;
import com.sanbu.tools.FileUtil;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.LooperHelper;
import com.sanbu.tools.StringUtil;

import cn.lx.mbs.Events;
import cn.lx.mbs.LXConst;
import cn.lx.mbs.support.base.Basics;
import cn.lx.mbs.support.core.EndpointMBS;
import cn.lx.mbs.support.core.ExtHelper;
import cn.lx.mbs.support.structures.Layout;
import cn.lx.mbs.support.structures.SRId;
import cn.lx.mbs.support.structures.SRState;
import cn.lx.mbs.support.structures.StreamState;
import cn.lx.mbs.support.utils.SPHelper;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;

import cn.lx.mbs.support.structures.MixMode;
import cn.lx.mbs.support.structures.SurfaceId;
import cn.lx.mbs.support.structures.RecProp;
import cn.lx.mbs.support.structures.Source;
import cn.lx.mbs.support.structures.ChannelId;

// MBS后台功能入口
public class MBS implements EndpointMBS.Observer {

    private static final String TAG = MBS.class.getSimpleName();

    public static void initEnv(Context context) {
        // init endpoint environment
         EndpointMBS.initEnv(context);

        // mkdirs
        new File(LXConst.SP_PATH).mkdirs();
        FileUtil.deleteDir(LXConst.TEMP_PATH);
        new File(LXConst.TEMP_PATH).mkdirs();
        new File(LXConst.LOG_PATH).mkdirs();
        new File(LXConst.DB_PATH).mkdirs();
        new File(LXConst.UPGRADE_PATH).mkdirs();
        new File(LXConst.RESOURCE_PATH).mkdirs();
        new File(LXConst.MEDIA_PATH).mkdirs();

        // save resources to storage
        saveResToStorage(context);
        saveAssetToStorage(context);
    }

    private static MBS sInstance = new MBS();

    public static MBS getInstance() {
        return sInstance;
    }

    private Context mContext;
    private LooperHelper mLooperHelper;
    private Handler mWorkHandler;

    private SPHelper mSPHelper;
    private Basics mBasics;
    private EndpointMBS mEndpointMBS;

    private Map<SurfaceId, SurfaceCache> mSurfaceCaches;

    private MBS() {

    }

    public int init(Context context) {
        if (mContext == null) {
            mContext = context;
            mLooperHelper = new LooperHelper(LXConst.CHILD_THREAD_NAME);
            mLooperHelper.startLoopInNewThreadUntilReady();
            mWorkHandler = mLooperHelper.getHandler();

            mSPHelper = new SPHelper();
            mBasics = new Basics();
            mSurfaceCaches = new HashMap<>(6);

            // init error recorder
            LXConst.gMessageBox.init(mWorkHandler);

            // init basics module
            mBasics.init();
        }
        return 0;
    }

    public void release() {
        LogUtil.w(LXConst.TAG, "MBS release IN");

        if (mEndpointMBS != null) {
            mEndpointMBS.release();
            mEndpointMBS = null;
        }

        if (mBasics != null) {
            mBasics.release();
            mBasics = null;
        }

        if (mSPHelper != null) {
            mSPHelper.release();
            mSPHelper = null;
        }

        if (mLooperHelper != null)
            mLooperHelper.stopTheLoopThreadUntilOver(true);

        mContext = null;
        mWorkHandler = null;
        mLooperHelper = null;
        mSurfaceCaches.clear();
        mSurfaceCaches = null;

        LogUtil.w(LXConst.TAG, "MBS release OUT");
    }

    public int startEP(Context context) {
        if (mWorkHandler == null)
            return BaseError.ACTION_ILLEGAL;

        if (mEndpointMBS == null) {
            // init endpoint
            int ret = EndpointMBS.getInstance().init(mSPHelper, mWorkHandler, this, context, mExtHelper);
            if (ret != 0) {
                release();
                return ret;
            }

            mEndpointMBS = EndpointMBS.getInstance();

            for (Map.Entry<SurfaceId, SurfaceCache> entry: mSurfaceCaches.entrySet()) {
                SurfaceId surfaceId = entry.getKey();
                SurfaceCache cache = entry.getValue();
                if (cache.surface != null)
                    mEndpointMBS.onSurfaceCreated(surfaceId, cache.surface);
                if (cache.width > 0)
                    mEndpointMBS.onSurfaceChanged(surfaceId, cache.format, cache.width, cache.height);
            }
        }
        return 0;
    }

    public boolean isReady() {
        return mWorkHandler != null && mEndpointMBS != null;
    }

    /////////////////////////////// UI层显示窗口

    // @brief 本地显示子窗口创建
    public void onSurfaceCreated(SurfaceId id, Surface handle) {
        mSurfaceCaches.put(id, new SurfaceCache(handle));

        if (mEndpointMBS != null)
            mEndpointMBS.onSurfaceCreated(id, handle);
    }

    // @brief 本地显示窗口改变
    public void onSurfaceChanged(SurfaceId id, int format, int width, int height) {
        SurfaceCache cache = mSurfaceCaches.get(id);
        if (cache != null)
            cache.update(format, width, height);

        if (mEndpointMBS != null)
            mEndpointMBS.onSurfaceChanged(id, format, width, height);
    }

    // @brief 本地显示窗口销毁
    public void onSurfaceDestroyed(SurfaceId id) {
        mSurfaceCaches.remove(id);

        if (mEndpointMBS != null)
            mEndpointMBS.onSurfaceDestroyed(id);
    }

    /////////////////////////////// 基础功能

    // TODO: 基本信息/版本/授权/日志/应用数据

    /////////////////////////////// 信号源配置

    // @brief 获取可用信号源
    public List<Source> getAvailableSources() {
        return getEPX200(() -> mEndpointMBS.getAvailableSources(),
                () -> Collections.emptyList());
    }

    // @brief 添加信号源
    public void addSource(Source source, Callback/*int sourceId*/ callback) {
        callEPX200(() -> mEndpointMBS.addSource(source, callback),
                () -> illegalAction(callback));
    }

    // @brief 删除信号源(不影响已加载的信号源)
    public void removeSource(int sourceId, Callback callback) {
        callEPX200(() -> mEndpointMBS.removeSource(sourceId, callback),
                () -> illegalAction(callback));
    }

    // @brief 修改信号源(不影响已加载的信号源)
    public void updateSource(Source source, Callback callback) {
        callEPX200(() -> mEndpointMBS.updateSource(source, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 推流录制参数配置

    // @brief 获取推流录制的音频编码参数
    public AudioFormat getSRAudioFormat() {
        return getEPX200(() -> mEndpointMBS.getSRAudioFormat(),
                () -> LXConst.DEFAULT_SR_AUDIO_FORMAT);
    }

    // @brief 设置推流录制的音频编码参数(下次生效)
    public void setSRAudioFormat(AudioFormat format, Callback callback) {
        callEPX200(() -> mEndpointMBS.setSRAudioFormat(format, callback),
                () -> illegalAction(callback));
    }

    // @brief 获取推流录制的视频编码参数
    public VideoFormat getSRVideoFormat(SRId id) {
        return getEPX200(() -> mEndpointMBS.getSRVideoFormat(id),
                () -> id == SRId.Recording ?
                        LXConst.DEFAULT_SR_R_VIDEO_FORMAT : LXConst.DEFAULT_SR_S_VIDEO_FORMAT);
    }

    // @brief 设置推流录制的视频编码参数(下次生效)
    public void setSRVideoFormat(SRId id, VideoFormat format, Callback callback) {
        callEPX200(() -> mEndpointMBS.setSRVideoFormat(id, format, callback),
                () -> illegalAction(callback));
    }

    // @brief 获取推流地址
    public List<String> getStreamingUrls() {
        return getEPX200(() -> mEndpointMBS.getStreamingUrls(),
                () -> Collections.emptyList());
    }

    // @brief 设置推流地址(下次生效)
    public void setStreamingUrls(List<String> urls, Callback callback) {
        callEPX200(() -> mEndpointMBS.setStreamingUrls(urls, callback),
                () -> illegalAction(callback));
    }

    // @brief 获取录制属性
    public RecProp getRecProp() {
        return getEPX200(() -> mEndpointMBS.getRecProp(),
                () -> LXConst.DEFAULT_SR_REC_PROP);
    }

    // @brief 设置录制属性(下次生效)
    public void setRecProp(RecProp file, Callback callback) {
        callEPX200(() -> mEndpointMBS.setRecProp(file, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 输入管理

    // 备注:
    // CUT: 跳过PVW直接将输入全屏显示到输出,且无动画
    // PAUSE: 暂停输入通道的画面(采集源冻结最后一帧;解码源继续解码,但Scene冻结最好一帧)
    // CTRL: 输入源的控制操作,如：IPC的云台控制、Camera的相机控制
    // EDIT: 待定

    // @brief 加载输入通道
    public void loadInput(ChannelId id, int sourceId, Callback callback) {
        callEPX200(() -> mEndpointMBS.loadInput(id, sourceId, callback),
                () -> illegalAction(callback));
    }

    // @brief 卸载输入通道
    public void unloadInput(ChannelId id, Callback callback) {
        callEPX200(() -> mEndpointMBS.unloadInput(id, callback),
                () -> illegalAction(callback));
    }

    // @brief CUT输入通道(视频)
    public void cutInputVideo(ChannelId id, Callback callback) {
        callEPX200(() -> mEndpointMBS.cutInputVideo(id, callback),
                () -> illegalAction(callback));
    }

    // @brief 暂停输入通道(视频)
    public void pauseInputVideo(ChannelId id, Callback callback) {
        callEPX200(() -> mEndpointMBS.pauseInputVideo(id, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 音频设置

    // 备注: 音频存在N个(1~5)输入通道,1个输出通道,1个监听通道;
    // OFF/ON/AFV开关仅针对输出通道; SOLO开关仅针对监听通道;
    // 输入通道的音量调节是针对输入本身,实际会改变输出和监听通道中的音量

    // @brief 获取音频指定通道的音量
    public float getChannelVolume(ChannelId id) {
        return getEPX200(() -> mEndpointMBS.getChannelVolume(id),
                () -> 0.0f);
    }

    // @brief 设置音频指定通道的音量
    public void setChannelVolume(ChannelId id, float volume, Callback callback) {
        callEPX200(() -> mEndpointMBS.setChannelVolume(id, volume, callback),
                () -> illegalAction(callback));
    }

    // @brief 获取输出的音频通道的混音模式
    public Map<ChannelId, MixMode> getOutputAudioMixModes() {
        return getEPX200(() -> mEndpointMBS.getOutputAudioMixModes(),
                () -> new HashMap<>());
    }

    // @brief 设置输出的音频通道的混音模式
    public void setOutputAudioMixModes(ChannelId id, MixMode mode, Callback callback) {
        callEPX200(() -> mEndpointMBS.setOutputAudioMixModes(id, mode, callback),
                () -> illegalAction(callback));
    }

    // @brief 获取音频监听通道的状态
    public Map<ChannelId, Boolean> getSoloAudioOnOff() {
        return getEPX200(() -> mEndpointMBS.getSoloAudioOnOff(),
                () -> new HashMap<>());
    }

    // @brief 切换指定音频通道是否被监听
    public void switchSoloAudio(ChannelId id, boolean onOff, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchSoloAudio(id, onOff, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 推流录制控制

    // @brief 开关推流录制
    public void switchSRState(SRId id, SRState state, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchSRState(id, state, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// 显示切换

    // @brief 设置预览画面
    public void setPVWLayout(Layout layout, Callback callback) {
        callEPX200(() -> mEndpointMBS.setPVWLayout(layout, callback),
                () -> illegalAction(callback));
    }

    // @brief 切换预览画面到输出画面
    public void switchPVW2PGM(TransitionDesc transition, Callback callback) {
        callEPX200(() -> mEndpointMBS.switchPVW2PGM(transition, callback),
                () -> illegalAction(callback));
    }

    /////////////////////////////// callbacks

    public void onNetworkChanged() {
        if (mBasics != null)
            mBasics.onNetworkChanged();
    }

    @Override
    public void onRecordingFinished(String path) {
        if (StringUtil.isEmpty(path))
            return;
        if (path.contains("file://"))
            path = path.substring("file://".length());

        String name = new File(path).getName();
        EventPub.getDefaultPub().post(BaseEvents.buildEvt4UserHint("录制完成: " + name, false));
    }

    @Override
    public void onStreamStateChanged(ChannelId channelId, DataType streamType, boolean ready) {
        StreamState state = new StreamState(channelId, streamType, ready);
        EventPub.getDefaultPub().post(new EventPub.Event(Events.STREAM_STATE_CHANGED, -1, -1, state));
    }

    private ExtHelper mExtHelper = new ExtHelper() {
        @Override
        public String getUserName() {
            // TODO
            return "MBS";
        }

        @Override
        public String getTitle() {
            // TODO
            return "Demo";
        }
    };

    /////////////////////////////// private functions

    /////////////////////////////// private static utils

    private interface EPFunc<T> {
        T run();
    }

    private <T> T getEPX200(EPFunc<T> realFunc, EPFunc<T> nullFunc) {
        if (mEndpointMBS != null)
            return realFunc.run();
        else
            return nullFunc.run();
    }

    private void callEPX200(Runnable realFunc, Runnable nullFunc) {
        if (mEndpointMBS != null)
            realFunc.run();
        else
            nullFunc.run();
    }

    private void illegalAction(Callback callback) {
        String hint = "非法操作,请先初始化";
        LogUtil.w(LXConst.TAG, TAG, "init first");
        LXConst.gMessageBox.add("ERROR", TAG, hint);
        LXConst.callbackResult(callback, new Result(BaseError.ACTION_ILLEGAL, hint));
    }

    private void nonEPAction(Callback callback) {
        String hint = "不支持的操作: 未启动互动教学主程序";
        LogUtil.w(LXConst.TAG, TAG, "start ep first");
        LXConst.gMessageBox.add("ERROR", TAG, hint);
        LXConst.callbackResult(callback, new Result(BaseError.ACTION_ILLEGAL, hint));
    }

    private static void saveResToStorage(Context context) {
        for (Map.Entry<String, Tuple<Integer, FileSaveUtil.Action>> resource : LXConst.FIXED_RESOURCE.entrySet()) {
            String toFilename = resource.getKey();
            int resId = resource.getValue().first;
            String resName = context.getResources().getResourceEntryName(resId);
            FileSaveUtil.Action action = resource.getValue().second;

            int ret = FileSaveUtil.saveResToStorage(context, resId, toFilename, action);
            if (ret == FileSaveUtil.CODE_ERROR)
                LogUtil.w(LXConst.TAG, TAG, String.format("save resources(%s) to storage(%s) failed", resName, toFilename));
            else if (ret == FileSaveUtil.CODE_EXISTED)
                LogUtil.i(LXConst.TAG, TAG, String.format("skip to save resources(%s) to storage(%s), it existed", resName, toFilename));
            else
                LogUtil.i(LXConst.TAG, TAG, String.format("save resources(%s) to storage(%s)", resName, toFilename));
        }
    }

    private static void saveAssetToStorage(Context context) {
        for (Map.Entry<String, Tuple<String, FileSaveUtil.Action>> asset : LXConst.BUILD_IN_ASSETS.entrySet()) {
            String toFilename = asset.getKey();
            String assetFile = asset.getValue().first;
            FileSaveUtil.Action action = asset.getValue().second;

            int ret = FileSaveUtil.saveAssetToStorage(context, assetFile, toFilename, action);
            if (ret == FileSaveUtil.CODE_ERROR)
                LogUtil.w(LXConst.TAG, TAG, String.format("save asset(%s) to storage(%s) failed", assetFile, toFilename));
            else if (ret == FileSaveUtil.CODE_EXISTED)
                LogUtil.i(LXConst.TAG, TAG, String.format("skip to save asset(%s) to storage(%s), it existed", assetFile, toFilename));
            else
                LogUtil.i(LXConst.TAG, TAG, String.format("save asset(%s) to storage(%s)", assetFile, toFilename));
        }
    }

    private class SurfaceCache {
        public Surface surface;
        public int format;
        public int width;
        public int height;

        public SurfaceCache(Surface surface) {
            this.surface = surface;
        }

        public void update(int format, int width, int height) {
            this.format = format;
            this.width = width;
            this.height = height;
        }
    }
}
