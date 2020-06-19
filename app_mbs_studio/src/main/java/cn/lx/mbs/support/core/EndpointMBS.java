package cn.lx.mbs.support.core;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Runnable3;
import com.sanbu.base.State;
import com.sanbu.base.Tuple;
import com.sanbu.board.BoardSupportClient;
import com.sanbu.board.EmptyBoardSupportClient;
import com.sanbu.tools.AsyncResult;
import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.lx.mbs.LXConst;
import cn.lx.mbs.support.structures.AVStream;
import cn.lx.mbs.support.structures.AudioCodec;
import cn.lx.mbs.support.structures.MixMode;
import cn.lx.mbs.support.structures.ChannelId;
import cn.lx.mbs.support.structures.Input;
import cn.lx.mbs.support.structures.Layout;
import cn.lx.mbs.support.structures.Overlay;
import cn.lx.mbs.support.structures.RecProp;
import cn.lx.mbs.support.structures.SRId;
import cn.lx.mbs.support.structures.SRState;
import cn.lx.mbs.support.structures.Source;
import cn.lx.mbs.support.structures.SurfaceId;
import cn.lx.mbs.support.utils.SPHelper;
import cn.sanbu.avalon.endpoint3.Endpoint3;
import cn.sanbu.avalon.endpoint3.structures.AudioInputDevice;
import cn.sanbu.avalon.endpoint3.structures.AudioOutputDevice;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.EPObjectType;
import cn.sanbu.avalon.endpoint3.structures.Region;
import cn.sanbu.avalon.endpoint3.structures.SourceType;
import cn.sanbu.avalon.endpoint3.structures.TransProtocol;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioCapabilities;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioCapability;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.jni.DisplayConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.DisplayOverlay;
import cn.sanbu.avalon.endpoint3.structures.jni.EPDir;
import cn.sanbu.avalon.endpoint3.structures.jni.EPEvent;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.MixerTracks;
import cn.sanbu.avalon.endpoint3.structures.jni.RecSplitStrategy;
import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionMode;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapabilities;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapability;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;
import cn.sanbu.avalon.media.MediaJni;
import cn.sanbu.avalon.media.VideoEngine;

/*
* EndpointMBS: 因为EP3是更灵活的通用SDK，为降低应用层使用EP的复杂度,封装了EndpointMBS
* 功能描述:
*   . 根据MBS的业务场景，支持: 信号管理、输入管理、声音管理、推流录制、显示和切换
*   . 封装了内部线程、配置读写、状态维护
*   . 除了初始化类接口和查询类接口, 其他操作类接口为非阻塞异步调用,且线程安全
*   . 没有事件概念
* */
public class EndpointMBS implements Endpoint3.Callback {

    private static final String TAG = EndpointMBS.class.getSimpleName();

    // @brief 应用层需要实现的回调
    public interface Observer {
        // @brief 录制结束
        void onRecordingFinished(String path);

        // @brief 输入流状态变化
        void onStreamStateChanged(ChannelId channelId, DataType streamType, boolean ready);
    }

    private static boolean gInited = false;

    public static void initEnv(Context context) {
        if (gInited)
            return;

        synchronized (EndpointMBS.class) {
            if (gInited)
                return;

            // init endpoint jni environment
            Endpoint3.initEnv(context, "ep3_android");

            // set hint for NoSignal and Loading before MediaJni.init
            if (LXConst.USING_INTERNAL_NO_SIGNAL_IMG) {
                VideoEngine.setNoSignalText("Camera无信号");
                VideoEngine.setLoadingText("加载中");
            }

            // init media engine jni environment
            BoardSupportClient client = new EmptyBoardSupportClient();
            MediaJni.initEnv(context, client, LXConst.USING_INTERNAL_NO_SIGNAL_IMG);

            gInited = true;
        }
    }

    private static final EndpointMBS gInstance = new EndpointMBS();

    public static final EndpointMBS getInstance() {
        return gInstance;
    }

    ////////// inited config and handlers

    private Endpoint3 mEndpoint;

    private SPHelper mSPHelper;
    private Handler mRTWorker;
    private Observer mObserver;
    private ExtHelper mExtHelper;

    ////////// preferences

    // base
    private EPFixedConfig mEPFixedConfig;

    // source
    private List<Source> mAvailableSources;

    // SR(streaming and recording)
    private AudioFormat mSRAudioFormat;
    private Map<SRId, VideoFormat> mSRVideoFormats;
    private List<String> mStreamingUrls;
    private RecProp mRecProp;

    ////////// status

    // display
    private Map<SurfaceId, Display> mDisplays;

    // mixer
    private int mSpeaker;
    private Mixer mOutputMixer;
    private Mixer mEchoMixer;

    // inputs
    private Map<ChannelId, Input> mInputs;

    // streaming and recording
    private List<SR> mStreaming;
    private SR mRecording;

    private EndpointMBS() {
        mEndpoint = Endpoint3.getInstance();
    }

    /////////////////////////////// 初始化和反初始化

    // @brief 初始化EP-MBS;必须初始化后才能调用其他接口
    // @param sp [IN], 配置项存储句柄
    // @param rtWorker [IN], 实时工作队列
    // @param ob [IN], 应用层回调
    // @return 0 is suc, or failed.
    public int init(SPHelper sp, Handler rtWorker, Observer ob, Context context,
                    ExtHelper extHelper) {
        initEnv(context);

        if (mRTWorker != null) {
            LogUtil.w(LXConst.TAG, TAG, "had inited");
            return 0;
        }

        // keep the handlers
        mSPHelper = sp;
        mRTWorker = rtWorker;
        mObserver = ob;
        mExtHelper = extHelper;

        // init status
        mInputs = new HashMap<>(5);
        mStreaming = new LinkedList<>();
        mRecording = new SR(-1, SRState.Stop, null);

        // load preferences from SP
        loadPreferences();

        // init displays
        int ret = initEP();
        if (ret != 0) {
            release();
            return ret;
        }

        // init displays
        ret = initDisplays();
        if (ret != 0) {
            release();
            return ret;
        }

        // init mixers
        ret = initMixers();
        if (ret != 0) {
            release();
            return ret;
        }

        // load mic as source first
        asyncCall(() -> loadMic());

        return 0;
    }

    // @brief 反初始化
    // @return 0 is suc, or failed.
    public int release() {
        // block to release all handlers
        if (mRTWorker != null) {

            AsyncResult asyncResult = new AsyncResult();
            mRTWorker.post(() -> {
                switchRecordingState(SRState.Stop);
                switchStreamingState(SRState.Stop);

                releaseMixers();
                releaseDisplays();
                unloadInputs();
                releaseEP();

                mSPHelper = null;
                mRTWorker = null;
                mObserver = null;
                mExtHelper = null;

                mInputs = null;
                mStreaming = null;
                mRecording = null;

                asyncResult.notify2(0);
            });

            int ret = (int) asyncResult.wait2(4000, -1);
            if (ret != 0)
                LogUtil.w(CoreUtils.TAG, TAG, "release timeout");
        }
        return 0;
    }

    /////////////////////////////// UI层显示窗口

    // @brief 本地显示窗口创建
    // @param id [IN] 窗口ID
    // @param handle [IN] 显示surface
    public void onSurfaceCreated(SurfaceId id, Surface handle) {
        logAction("onSurfaceCreated", id);

        asyncCall("onSurfaceCreated", null, () -> {
            Display display = mDisplays.get(id);
            if (display == null)
                return genError("onSurfaceCreated", BaseError.LOGICAL_ERROR,
                        id.name() + " display not support", "不支持的显示窗口: " + id.desc);
            if (display.epId >= 0)
                return genError("onSurfaceCreated", BaseError.LOGICAL_ERROR,
                        id.name() + " display had created", "重复创建显示窗口: " + id.desc);

            // create display
            display.epId = mEndpoint.epAddVisibleDisplay(handle);
            if (display.epId < 0)
                return genError("onSurfaceCreated", BaseError.INTERNAL_ERROR,
                        "epAddVisibleDisplay#" + id.name() + " failed: " + display.epId, "创建显示窗口失败: " + id.desc);

            // update state
            display.state = State.Doing;
            return Result.SUCCESS;
        });
    }

    // @brief 本地显示窗口改变
    // @param id [IN] 窗口ID
    public void onSurfaceChanged(SurfaceId id, int format, int width, int height) {
        logAction("onSurfaceChanged", id, width, height);

        asyncCall("onSurfaceChanged", null, () -> {
            Display display = mDisplays.get(id);
            if (display == null)
                return genError("onSurfaceChanged", BaseError.LOGICAL_ERROR,
                        id.name() + " display not support", "不支持的显示窗口: " + id.desc);
            if (display.epId < 0)
                return genError("onSurfaceChanged", BaseError.LOGICAL_ERROR,
                        "had not created display#" + id.name(), "逻辑错误,未创建显示窗口: " + id.desc);

            // re-config display size
            int ret = mEndpoint.epChangeVisibleDisplay(display.epId, format, width, height);
            if (ret != 0)
                return genError("onSurfaceChanged", BaseError.INTERNAL_ERROR,
                        "epChangeVisibleDisplay#" + id.name() + " failed: " + ret, "配置显示窗口失败: " + id.desc);

            // init display background color
            String color = mEPFixedConfig.background_color;
            mEndpoint.epConfigureDisplay(display.epId, DisplayConfig.buildBackgroundColor(color));

            display.size = new Size(width, height);
            display.state = State.Done;

            onSurfaceReady(id);
            return Result.SUCCESS;
        });
    }

    // @brief 本地显示窗口销毁
    // @param id [IN] 窗口ID
    public void onSurfaceDestroyed(SurfaceId id) {
        logAction("onSurfaceDestroyed", id);

        asyncCall("onSurfaceDestroyed", null, () -> {
            Display display = mDisplays.get(id);
            if (display == null)
                return genError("onSurfaceDestroyed", BaseError.LOGICAL_ERROR,
                        id.name() + " display not support", "不支持的显示窗口: " + id.desc);

            if (display.epId >= 0) {
                mEndpoint.epRemoveDisplay(display.epId);
                display.reset();
            }

            return Result.SUCCESS;
        });
    }

    /////////////////////////////// 信号源配置

    // @brief 获取可用信号源
    public List<Source> getAvailableSources() {
        if (isReady())
            return mAvailableSources;
        else
            return Collections.emptyList();
    }

    // @brief 添加信号源
    public void addSource(Source source, Callback/*int sourceId*/ callback) {
        asyncCall("addSource", callback, () -> {
            int maxId = -1;
            for (Source src: mAvailableSources) {
                if (src.id == source.id ||
                        (src.type == source.type && src.url.equals(source.url)))
                    return genError("addSource", BaseError.ACTION_ILLEGAL,
                            "had added a same source: " + src.url, "信号源已经存在,无须重复操作");
                maxId = Math.max(maxId, src.id);
            }

            logAction("addSource", source);

            int sourceId = source.id >= 0 ? source.id : maxId + 1;
            mAvailableSources.add(Source.copy(sourceId, source));

            mSPHelper.setAvailableSources(mAvailableSources);
            return Result.buildSuccess(sourceId);
        });
    }

    // @brief 删除信号源(不影响已加载的信号源)
    public void removeSource(int sourceId, Callback callback) {
        asyncCall("removeSource", callback, () -> {
            for (int i = 0 ; i < mAvailableSources.size() ; ++i) {
                if (mAvailableSources.get(i).id == sourceId) {
                    logAction("removeSource", sourceId);
                    mAvailableSources.remove(i);

                    mSPHelper.setAvailableSources(mAvailableSources);
                    return Result.SUCCESS;
                }
            }

            return genError("removeSource", BaseError.TARGET_NOT_FOUND,
                    "not found the target source#" + sourceId, "未找到指定信号源");
        });
    }

    // @brief 修改信号源(不影响已加载的信号源)
    public void updateSource(Source source, Callback callback) {
        asyncCall("updateSource", callback, () -> {
            for (int i = 0 ; i < mAvailableSources.size() ; ++i) {
                if (mAvailableSources.get(i).id == source.id) {
                    logAction("updateSource", source);
                    mAvailableSources.remove(i);
                    mAvailableSources.add(source);

                    mSPHelper.setAvailableSources(mAvailableSources);
                    return Result.SUCCESS;
                }
            }

            return genError("updateSource", BaseError.TARGET_NOT_FOUND,
                    "not found the target source#" + source.id, "未找到指定信号源");
        });
    }

    /////////////////////////////// 推流录制参数配置

    // @brief 获取推流录制的音频编码参数
    public AudioFormat getSRAudioFormat() {
        if (isReady())
            return mSRAudioFormat;
        else
            return LXConst.DEFAULT_SR_AUDIO_FORMAT;
    }

    // @brief 设置推流录制的音频编码参数(下次生效)
    public void setSRAudioFormat(AudioFormat format, Callback callback) {
        asyncCall("setSRAudioFormat", callback, () -> {
            if (mSRAudioFormat.isEqual(format))
                return Result.SUCCESS;

            // ignore the case that the streaming or recording has been started

            logAction("setSRAudioFormat", format);

            mSRAudioFormat = new AudioFormat(format);
            mSPHelper.setSRAudioFormat(mSRAudioFormat);
            return Result.SUCCESS;
        });
    }

    // @brief 获取推流录制的视频编码参数
    public VideoFormat getSRVideoFormat(SRId id) {
        if (isReady())
            return mSRVideoFormats.get(id);
        else if (id == SRId.Streaming)
            return LXConst.DEFAULT_SR_S_VIDEO_FORMAT;
        else if (id == SRId.Recording)
            return LXConst.DEFAULT_SR_R_VIDEO_FORMAT;
        else
            return null;
    }

    // @brief 设置推流录制的视频编码参数(下次生效)
    public void setSRVideoFormat(SRId id, VideoFormat format, Callback callback) {
        asyncCall("setSRVideoFormat", callback, () -> {
            if (mSRVideoFormats.get(id).isEqual(format))
                return Result.SUCCESS;

            // ignore the case that the streaming or recording has been started

            logAction("setSRVideoFormat", id, format);

            mSRVideoFormats.put(id, new VideoFormat(format));
            mSPHelper.setSRVideoFormat(id, format);
            return Result.SUCCESS;
        });
    }

    // @brief 获取推流地址
    public List<String> getStreamingUrls() {
        if (isReady())
            return mStreamingUrls;
        else
            return Collections.emptyList();
    }

    // @brief 设置推流地址(下次生效)
    public void setStreamingUrls(List<String> urls, Callback callback) {
        asyncCall("setStreamingUrls", callback, () -> {

            // ignore the case that the streaming has been started

            logAction("setStreamingUrls", urls);

            mStreamingUrls = new ArrayList<>(urls);
            mSPHelper.setStreamingUrls(mStreamingUrls);
            return Result.SUCCESS;
        });
    }

    // @brief 获取录制属性
    public RecProp getRecProp() {
        if (isReady())
            return mRecProp;
        else
            return LXConst.DEFAULT_SR_REC_PROP;
    }

    // @brief 设置录制属性(下次生效)
    public void setRecProp(RecProp file, Callback callback) {
        asyncCall("setRecProp", callback, () -> {
            if (mRecProp.isEqual(file))
                return Result.SUCCESS;

            // ignore the case that the recording has been started

            logAction("setRecProp", file);

            mRecProp = new RecProp(file);
            mSPHelper.setRecProp(mRecProp);
            return Result.SUCCESS;
        });
    }

    /////////////////////////////// 输入管理

    // @brief 加载输入通道
    public void loadInput(ChannelId id, int sourceId, Callback callback) {
        asyncCall("loadInput", callback, () -> {
            if (mInputs.containsKey(id))
                return genError("loadInput", BaseError.ACTION_ILLEGAL,
                        "the input channel had loaded: " + id.name(),
                        id.name() + "已经添加, 请先卸载原先输入再重新添加");

            Source source = mAvailableSources.get(sourceId);
            if (source == null)
                return genError("loadInput", BaseError.TARGET_NOT_FOUND,
                        "not find the source#" + sourceId,
                        "没有找到指定源: " + sourceId);

            logAction("loadInput", id, sourceId);

            int epId;
            Caller caller = null;
            if (source.type == SourceType.VideoCapture) {
                epId = mEndpoint.epAddVideoCapture(source.url, source.resolution);
            } else if (source.type == SourceType.RTSP) {
                TransProtocol protocol = source.overTCP ? TransProtocol.TCP : TransProtocol.RTP;
                epId = mEndpoint.epAddRTSPSource(source.url, protocol, LXConst.SOURCE_RECONNECTING);
            } else if (source.type == SourceType.RTMP) {
                epId = mEndpoint.epAddNetSource(source.url, LXConst.SOURCE_RECONNECTING);
            } else if (source.type == SourceType.RMSP) {
                epId = mEndpoint.epAddRMSPSource(source.url, source.videoFormat, source.audioFormat);
            } else if (source.type == SourceType.Caller) {
                Result result = makeCall(source, id);
                if (!result.isSuccessful())
                    return result;
                caller = (Caller) result.data;
                epId = caller.epId;
            } else {
                return genError("loadInput", BaseError.ACTION_UNSUPPORTED,
                        "not support this type of source: " + source.type.name(),
                        "暂时不支持加载此类型的源: " + source.type.name());
            }

            if (epId < 0)
                return genError("loadInput", BaseError.INTERNAL_ERROR,
                        "epAddXXSource failed: " + epId,
                        "内部错误,加载源失败: " + epId);

            if (caller == null) {
                float volume = getDefaultVolume();
                Input input = new Input(source, id, epId, State.Done, volume, isMute(volume));
                mInputs.put(id, input);
            } else {
                mInputs.put(id, caller);
            }

            return Result.SUCCESS;
        });
    }

    // @brief 卸载输入通道
    public void unloadInput(ChannelId id, Callback callback) {
        asyncCall("unloadInput", callback, () -> unloadInputImpl(id));
    }

    // @brief CUT输入通道(视频)
    public void cutInputVideo(ChannelId id, Callback callback) {
        asyncCall("cutInputVideo", callback, () -> {
            Layout layout = new Layout().setOverlays(Arrays.asList(new Overlay.Stream(id, Region.buildFull())));
            return switchLayout(SurfaceId.PGM, layout, TransitionDesc.buildEmpty());
        });
    }

    // @brief 暂停输入通道(视频)
    public void pauseInputVideo(ChannelId id, Callback callback) {
        asyncCall("pauseInputVideo", callback, () ->
                genError("pauseInputVideo", BaseError.ACTION_UNSUPPORTED,
                    "not implement", "暂不支持")
        );
    }

    /////////////////////////////// 音频设置

    // @brief 获取音频指定通道的音量
    public float getChannelVolume(ChannelId id) {
        if (!isReady())
            return 0.0f;
        else if (id == ChannelId.Output)
            return mOutputMixer.volume;
        else if (mInputs.containsKey(id))
            return mInputs.get(id).volume;
        else
            return 0.0f;
    }

    // @brief 设置音频指定通道的音量
    public void setChannelVolume(ChannelId id, float volume, Callback callback) {
        asyncCall("setChannelVolume", callback, () -> {

            logAction("setChannelVolume", id, volume);

            if (id == ChannelId.Output) {
                if (volume == mOutputMixer.volume && !mOutputMixer.muted)
                    return Result.SUCCESS;

                Result result = setVolumeImpl(mOutputMixer.epId, volume, mOutputMixer.tracks);
                if (result.isSuccessful()) {
                    mOutputMixer.volume = volume;
                    mOutputMixer.muted = isMute(volume);
                    flushCallerVolume();
                }
            } else {
                Input input = mInputs.get(id);
                if (input == null)
                    return genError("setChannelVolume", BaseError.TARGET_NOT_FOUND,
                            "the input channel is not loaded: " + id.name(),
                            "指定的输入通道尚未加载: " + id.name());

                if (volume == input.volume && !input.muted)
                    return Result.SUCCESS;

                // update volume
                input.volume = volume;
                input.muted = isMute(volume);

                // get the first decoded stream
                AVStream stream = input.getDecodedAudioStream();

                // flush mixer by required
                if (mOutputMixer.tracks.contains(stream.getDecId()))
                    flushOutputMixer();
                if (mEchoMixer.tracks.contains(stream.getDecId()))
                    flushEchoMixer();
            }

            return Result.SUCCESS;
        });
    }

    // @brief 获取输出的音频通道的混音模式
    public Map<ChannelId, MixMode> getOutputAudioMixModes() {
        if (isReady())
            return mOutputMixer.mixModes;
        else
            return Collections.emptyMap();
    }

    // @brief 设置输出的音频通道的混音模式
    public void setOutputAudioMixModes(ChannelId id, MixMode mode, Callback callback) {
        asyncCall("setOutputAudioMixModes", callback, () -> {
            MixMode current = mOutputMixer.mixModes.get(id);
            if (current != null && current == mode)
                return Result.SUCCESS;

            if (id == ChannelId.MIC0 && mode == MixMode.AFV)
                return genError("setOutputAudioMixModes", BaseError.ACTION_UNSUPPORTED,
                        "mic channel not supports AFV mode",
                        "MIC通道不支持AFV模式");

            Input input = mInputs.get(id);
            if (input == null)
                return genError("setOutputAudioMixModes", BaseError.TARGET_NOT_FOUND,
                        "the input channel is not loaded: " + id.name(),
                        "指定的输入通道尚未加载: " + id.name());

            logAction("setOutputAudioMixModes", id, mode);

            mOutputMixer.mixModes.put(id, mode);

            AVStream stream = input.getDecodedAudioStream();
            if (stream != null) {
                flushOutputMixer();
            } else {
                LogUtil.d(CoreUtils.TAG, TAG, "setOutputAudioMixModes, audio stream is not ready for " + id.name());
            }

            return Result.SUCCESS;
        });
    }

    // @brief 获取音频监听通道的状态
    public Map<ChannelId, Boolean> getSoloAudioOnOff() {
        if (!isReady())
            return Collections.emptyMap();

        Map<ChannelId, Boolean> result = new HashMap<>(mEchoMixer.mixModes.size());
        for (Map.Entry<ChannelId, MixMode> entry: mEchoMixer.mixModes.entrySet())
            result.put(entry.getKey(), entry.getValue() == MixMode.On ? true : false);
        return result;
    }

    // @brief 切换指定音频通道是否被监听
    public void switchSoloAudio(ChannelId id, boolean onOff, Callback callback) {
        asyncCall("switchSoloAudio", callback, () -> {
            MixMode mode = onOff ? MixMode.On : MixMode.Off;

            MixMode current = mEchoMixer.mixModes.get(id);
            if (current != null && current == mode)
                return Result.SUCCESS;

            boolean flush = false;
            if (id == ChannelId.Output) {
                flush = true;
            } else {
                Input input = mInputs.get(id);
                if (input == null)
                    return genError("switchSoloAudio", BaseError.TARGET_NOT_FOUND,
                            "the input channel is not loaded: " + id.name(),
                            "指定的输入通道尚未加载: " + id.name());

                AVStream stream = input.getDecodedAudioStream();
                if (stream != null) {
                    flush = true;
                } else {
                    LogUtil.d(CoreUtils.TAG, TAG, "switchSoloAudio, audio stream is not ready for " + id.name());
                }
            }

            logAction("switchSoloAudio", id, onOff);

            mEchoMixer.mixModes.put(id, mode);

            if (flush)
                flushEchoMixer();

            return Result.SUCCESS;
        });
    }

    /////////////////////////////// 推流录制控制

    // @brief 开关推流录制
    public void switchSRState(SRId id, SRState state, Callback callback) {
        asyncCall("switchSRState", callback, () ->
            id == SRId.Streaming ? switchStreamingState(state) : switchRecordingState(state)
        );
    }

    /////////////////////////////// 显示切换

    // @brief 设置预览画面
    public void setPVWLayout(Layout layout, Callback callback) {
        asyncCall("setPVWLayout", callback, () ->
                switchLayout(SurfaceId.PVW, layout, TransitionDesc.buildEmpty()));
    }

    // @brief 切换预览画面到输出画面
    public void switchPVW2PGM(TransitionDesc transition, Callback callback) {
        asyncCall("switchPVW2PGM", callback, () -> {
            Display display = mDisplays.get(SurfaceId.PVW);
            return switchLayout(SurfaceId.PGM, display.layout, transition);
        });
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
                Input input = getSourceByEPId(parent_id);
                if (input == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamOpen, from unknown source#" + parent_id);
                    return;
                }

                onRxStreamOpen(input, stream_id, desc, format);
            } else if (parent_type == EPObjectType.Caller) {
                Caller caller = getCallerByEPId(parent_id);
                if (caller == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamOpen, from unknown caller#" + parent_id);
                    return;
                }

                if (desc.direction == EPDir.Incoming)
                    onRxStreamOpen(caller, stream_id, desc, format);
                else
                    onTxStreamOpen(caller, stream_id, desc, format);
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
                Input input = getSourceByEPId(parent_id);
                if (input == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamClose, from unknown source#" + parent_id +
                            ", it may have been remove previously, force to release it");
                    mEndpoint.epStopSrcStreamDecoding(parent_id, stream_id);
                    return;
                }

                onRxStreamClose(input, stream_id, desc);
            } else if (parent_type == EPObjectType.Caller) {
                Caller caller = getCallerByEPId(parent_id);
                if (caller == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "onStreamClose, got unknown caller#" + parent_id +
                            ", it may have been remove previously, force to release it");
                    if (desc.direction == EPDir.Outgoing)
                        mEndpoint.epStopTxStream(parent_id, stream_id);
                    else if (desc.direction == EPDir.Incoming)
                        mEndpoint.epStopRxStreamDecoding(parent_id, stream_id);
                    return;
                }

                if (desc.direction == EPDir.Incoming)
                    onRxStreamClose(caller, stream_id, desc);
                else
                    onTxStreamClose(caller, stream_id, desc);
            } else if (parent_type == EPObjectType.Output) {
                LogUtil.i(CoreUtils.TAG, TAG, "onStreamClose from output#" + parent_id);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "onStreamClose, unknown from: " + parent_type.name());
            }
        });
    }

    @Override
    public void onRegistering(int result, CallingProtocol protocol) {
        LogUtil.w(CoreUtils.TAG, TAG, "onRegistering");
    }

    @Override
    public void onUnRegistering(int result, CallingProtocol protocol) {
        LogUtil.w(CoreUtils.TAG, TAG, "onUnRegistering");
    }

    @Override
    public void onNotifyRegisterStatus(int result, CallingProtocol protocol) {
        LogUtil.w(CoreUtils.TAG, TAG, "onNotifyRegisterStatus");
    }

    @Override
    public void onIncomingCall(int call_id, String number, String call_url, CallingProtocol protocol) {
        asyncCall(() -> {
            LogUtil.w(CoreUtils.TAG, TAG, "not support incoming call: " + call_url);
            mEndpoint.epReject(call_id);
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
            Caller caller = getCallerByEPId(call_id);
            if (caller == null) {
                LogUtil.w(CoreUtils.TAG, TAG, "onEstablished, got unknown caller#" + call_id);
                mEndpoint.epHangup(call_id);
                return;
            }

            if (caller.connState == State.None) {
                LogUtil.w(CoreUtils.TAG, TAG, "onEstablished, logical error, the caller state is none: " + call_id);
                return;
            }

            // update state
            caller.connState = State.Done;
        });
    }

    @Override
    public void onFinished(int call_id, int errcode, String reason) {
        asyncCall(() -> {
            Caller caller = getCallerByEPId(call_id);
            if (caller == null) {
                LogUtil.w(CoreUtils.TAG, TAG, "onFinished, got unknown caller#" + call_id);
                mEndpoint.epReleaseCaller(call_id);
                return;
            }

            caller.connState = State.None;
            releaseCaller(caller);
        });
    }

    @Override
    public void onCallerError(int call_id, int errcode, String error) {
        asyncCall(() -> {
            Caller caller = getCallerByEPId(call_id);
            if (caller == null) {
                LogUtil.w(CoreUtils.TAG, TAG, "onCallerError, got unknown caller#" + call_id);
                mEndpoint.epReleaseCaller(call_id);
                return;
            }

            caller.connState = State.None;
            releaseCaller(caller);
        });
    }

    @Override
    public void onEvent(EPObjectType obj_type, int obj_id, EPEvent event, String params) {
        asyncCall(() -> {
            switch (event) {
                case RecvReqOpenVideoExt:
                    // TODO
                    break;
                case FileWrittenCompleted:
                    // notify outside observer
                    if (mObserver != null)
                        mObserver.onRecordingFinished(params);
                    break;
                case SourceDecodingStateChanged:
                    try {
                        JsonObject json = new Gson().fromJson(params, JsonObject.class);
                        boolean ready = json.get("ready").getAsBoolean();
                        int mediaSourceId = json.get("media_source_id").getAsInt();
                        onVideoStreamDecodingReady(obj_type, obj_id, mediaSourceId, ready);
                    } catch (Exception e) {
                        LogUtil.w(CoreUtils.TAG, TAG, "SourceStateChanged, parse params error", e);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    /////////////////////////////// private functions

    private boolean isReady() {
        return mEndpoint != null && mRTWorker != null;
    }

    private void asyncCall(String action, Callback callback, Runnable3 runnable) {
        if (isReady()) {
            mRTWorker.post(() -> {
                if (isReady()) {
                    Result result = runnable.run();
                    CoreUtils.callbackResult(callback, result);
                } else {
                    Result result = genError(action, BaseError.ACTION_CANCELED,
                            "had been released", "非法操作,请先初始化", false);
                    CoreUtils.callbackResult(callback, result);
                }
            });
        } else {
            Result result = genError(action, BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作,请先初始化", false);
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

    private void loadPreferences() {
        mEPFixedConfig = mSPHelper.getEPFixedConfig();
        fixConfig(mEPFixedConfig);

        mAvailableSources = mSPHelper.getAvailableSources();

        mSRAudioFormat = mSPHelper.getSRAudioFormat();

        mSRVideoFormats = new HashMap<>(2);
        mSRVideoFormats.put(SRId.Streaming, mSPHelper.getSRVideoFormat(SRId.Streaming));
        mSRVideoFormats.put(SRId.Recording, mSPHelper.getSRVideoFormat(SRId.Recording));

        mStreamingUrls = mSPHelper.getStreamingUrls();

        mRecProp = mSPHelper.getRecProp();
    }

    private int initEP() {
        int ret = mEndpoint.epInit(this, mEPFixedConfig);
        if (ret != 0)
            return ret;

        mEndpoint.epSwitchAGC(true);
        return 0;
    }

    private void releaseEP() {
        // mEndpoint.epUninit();
    }

    private int initDisplays() {
        mDisplays = new HashMap<>(6);
        mDisplays.put(SurfaceId.PVW, new Display());
        mDisplays.put(SurfaceId.PGM, new Display());
        mDisplays.put(SurfaceId.IN1, new Display());
        mDisplays.put(SurfaceId.IN2, new Display());
        mDisplays.put(SurfaceId.IN3, new Display());
        mDisplays.put(SurfaceId.IN4, new Display());
        return 0;
    }

    private void releaseDisplays() {
        if (mDisplays == null)
            return;

        for (Display display: mDisplays.values()) {
            if (display.epId >= 0) {
                mEndpoint.epRemoveDisplay(display.epId);
                display.epId = -1;
            }
        }
        mDisplays.clear();
        mDisplays = null;
    }

    private int initMixers() {
        // init
        mSpeaker = -1;
        mOutputMixer = new Mixer();
        mEchoMixer = new Mixer();

        // create output mixer
        int mixer = mEndpoint.epAddMixer();
        if (mixer < 0) {
            LogUtil.e(CoreUtils.TAG, TAG, "epAddMixer failed: " + mixer);
            return mixer;
        }
        mOutputMixer.epId = mixer;
        mOutputMixer.mixModes.put(ChannelId.MIC0, LXConst.DEFAULT_MIC_MIX_MODE_4_OUTPUT);

        // create echo mixer
        mixer = mEndpoint.epAddMixer();
        if (mixer < 0) {
            LogUtil.e(CoreUtils.TAG, TAG, "epAddMixer failed: " + mixer);
            return mixer;
        }
        mEchoMixer.epId = mixer;

        // select a speaker by default
        AudioOutputDevice speaker = mEndpoint.epGetDefaultAudioOutputDevice();
        if (speaker == null || speaker.id < 0) {
            LogUtil.e(CoreUtils.TAG, TAG, "can not get audio output device with epGetDefaultAudioOutputDevice");
            return BaseError.INTERNAL_ERROR;
        }

        // bind the local mixer to the speaker
        int ret = mEndpoint.epBindAudioOutputDevice(speaker.id, EPObjectType.Mixer, mEchoMixer.epId, -1);
        if (ret != 0) {
            LogUtil.e(CoreUtils.TAG, TAG, "epBindAudioOutputDevice failed: " + ret);
            return ret;
        }
        mSpeaker = speaker.id;

        return 0;
    }

    private void releaseMixers() {
        // release output mixer
        if (mOutputMixer != null && mOutputMixer.epId >= 0) {
            mEndpoint.epRemoveMixer(mOutputMixer.epId);
        }
        mOutputMixer = null;

        // release echo mixer
        if (mEchoMixer != null && mEchoMixer.epId >= 0) {
            if (mSpeaker >= 0) {
                mEndpoint.epUnBindAudioOutputDevice(mSpeaker, EPObjectType.Mixer, mEchoMixer.epId, -1);
                mSpeaker = -1;
            }
            mEndpoint.epRemoveMixer(mEchoMixer.epId);
        }
        mEchoMixer = null;
    }

    private void loadMic() {
        AudioInputDevice device = mEndpoint.epGetDefaultAudioInputDevice();
        if (device != null) {
            int id = mEndpoint.epAddAudioCapture(device.url);
            if (id >= 0) {
                float volume = getDefaultVolume();
                Source source = Source.buildAudioCapture("默认麦克风", device.id);
                Input mic = new Input(source, ChannelId.MIC0, id, State.Done, volume, isMute(volume));
                mInputs.put(ChannelId.MIC0, mic);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "epAddAudioCapture(" + device.url + ") failed: " + id);
            }
        }
    }

    private void unloadInputs() {
        if (mInputs != null) {
            List<ChannelId> channels = new ArrayList<>(mInputs.keySet());
            for (ChannelId channel: channels)
                unloadInputImpl(channel);
        }
    }

    private Result unloadInputImpl(ChannelId id) {
        Input input = mInputs.remove(id);
        if (input == null)
            return genError("unloadInput", BaseError.TARGET_NOT_FOUND,
                    "the input channel is not found: " + id.name(),
                    "通道未加载: " + id.name());

        logAction("unloadInput", id);

        if (input.epId >= 0) {
            if (input.config.type == SourceType.Caller)
                releaseCaller((Caller) input);
            else
                mEndpoint.epRemoveSource(input.epId);
        }
        return Result.SUCCESS;
    }

    private float getDefaultVolume() {
        return 1.0f;
    }

    private Result switchLayout(SurfaceId id, Layout layout, TransitionDesc transition) {
        Display display = mDisplays.get(id);
        if (display.state != State.Done)
            return genError("switchLayout", BaseError.ACTION_ILLEGAL,
                    id.name() + " window is not ready",
                    id.desc + "窗口未就绪,无法切换布局");

        logAction("switchLayout", id, layout, transition);

        // update layout
        display.layout = layout;

        // gen display config
        DisplayConfig config = transLayout2Config(layout, transition);

        // flush to display
        if (!display.config.isEqual(config)) {
            int ret = mEndpoint.epConfigureDisplay(display.epId, config);
            if (ret != 0)
                return genError("setLDLayout", BaseError.INTERNAL_ERROR,
                        "epConfigureDisplay with layout failed: " + ret,
                        "内部错误,设置布局失败: " + ret);
            display.config = config;
        }

        // flush mixer
        if (id == SurfaceId.PGM)
            flushOutputMixer();

        return Result.SUCCESS;
    }

    private Result setVolumeImpl(int mixId, float volume, MixerTracks tracks) {
        int ret = mEndpoint.epSetMixer(mixId, volume, tracks);
        if (ret != 0)
            return genError("setVolumeImpl", BaseError.INTERNAL_ERROR,
                    "epSetMixer failed: " + ret, "内部错误,设置直播录制音量失败: " + ret);
        return Result.SUCCESS;
    }

    private Result flushEchoMixer() {
        if (!isReady())
            return Result.SUCCESS;

        MixerTracks tracks = getMixerTracks(mEchoMixer.mixModes, null);
        if (!mEchoMixer.tracks.isEqual(tracks)) {
            float volume = getVolume(mEchoMixer.volume, mEchoMixer.muted);
            int ret = mEndpoint.epSetMixer(mEchoMixer.epId, volume, tracks);
            if (ret != 0)
                return genError("flushEchoMixer", BaseError.INTERNAL_ERROR, "epSetMixer with tracks failed: " + ret,
                        "内部错误,设置混音失败: " + ret);
            mEchoMixer.tracks = tracks;
        }
        return Result.SUCCESS;
    }

    private Result flushOutputMixer() {
        if (!isReady())
            return Result.SUCCESS;

        Display display = mDisplays.get(SurfaceId.PGM);
        if (display == null || display.state != State.Done)
            return Result.SUCCESS;

        MixerTracks tracks = getMixerTracks(mOutputMixer.mixModes, display.layout);
        if (!mOutputMixer.tracks.isEqual(tracks)) {
            float volume = getVolume(mOutputMixer.volume, mOutputMixer.muted);
            int ret = mEndpoint.epSetMixer(mOutputMixer.epId, volume, tracks);
            if (ret != 0)
                return genError("flushOutputMixer", BaseError.INTERNAL_ERROR,
                        "epSetMixer with tracks failed: " + ret,
                        "内部错误,设置混音失败: " + ret);
            mOutputMixer.tracks = tracks;
        }

        // liked to flush caller's mixer
        flushCallerMixers();
        return Result.SUCCESS;
    }

    private Result flushCallerMixers() {
        if (!isReady())
            return Result.SUCCESS;

        for (Input input: mInputs.values()) {
            if (input.config.type != SourceType.Caller)
                continue;

            flushCallerMixer((Caller) input);
        }

        return Result.SUCCESS;
    }

    private Result flushCallerMixer(Caller caller) {
        if (mOutputMixer == null)
            return genError("flushCallerMixer", BaseError.ACTION_ILLEGAL,
                    "init first", "非法操作");
        if (mDisplays.get(SurfaceId.PGM).state != State.Done)
            return Result.SUCCESS;

        // strip caller's track
        List<Integer> decodingIds = caller.getDecodingIds(DataType.AUDIO);
        MixerTracks tracks = decodingIds.size() == 0 ? mOutputMixer.tracks :
                MixerTracks.remove(mOutputMixer.tracks, decodingIds);

        // skip no-changed tracks
        Mixer mixer = caller.mixer;
        if (mixer.tracks.isEqual(tracks))
            return Result.SUCCESS;

        // set tracks to the mixer
        float volume = getVolume(mOutputMixer.volume, mOutputMixer.muted);
        int ret = mEndpoint.epSetMixer(mixer.epId, volume, tracks);
        if (ret != 0)
            return genError("flushCallerMixer", BaseError.INTERNAL_ERROR,
                    "epSetMixer for mixer#" + mixer.epId + " failed: " + ret,
                    "刷新caller源混音失败");

        // update tracks
        mixer.tracks = tracks;
        return Result.SUCCESS;
    }

    private Result flushCallerVolume() {
        if (!isReady())
            return Result.SUCCESS;

        for (Input input: mInputs.values()) {
            if (input.config.type != SourceType.Caller)
                continue;

            Caller caller = (Caller) input;
            Mixer mixer = caller.mixer;

            float volume = getVolume(mOutputMixer.volume, mOutputMixer.muted);
            int ret = mEndpoint.epSetMixer(mixer.epId, volume, mixer.tracks);
            if (ret != 0) {
                LogUtil.w(CoreUtils.TAG, TAG, "epSetMixer for mixer#" + mixer.epId + " failed: " + ret);
                continue;
            }
        }

        return Result.SUCCESS;
    }

    private Result switchStreamingState(SRState state) {
        logAction("switchStreamingState", state);

        if (mStreaming.size() > 0) {
            Result result = Result.SUCCESS;
            result.message = "";
            Result tmp;

            Iterator<SR> iterator = mStreaming.iterator();
            while (iterator.hasNext()) {
                SR sr = iterator.next();

                if (state == SRState.Start) {
                    tmp = unPauseSR(SRId.Streaming, sr);
                } else if (state == SRState.Pause) {
                    tmp = pauseSR(SRId.Streaming, sr);
                } else if (state == SRState.Stop) {
                    tmp = stopSR(SRId.Streaming, sr);
                    iterator.remove();
                } else {
                    throw new IllegalStateException("Unexpected state: " + state.name());
                }

                if (!tmp.isSuccessful()) {
                    result.code = tmp.code;
                    result.message += tmp.message + ";";
                }
            }
            return result;
        } else {
            if (state == SRState.Start) {
                Result result = Result.SUCCESS;
                result.message = "";
                Result tmp;

                for (String url: mStreamingUrls) {
                    if (StringUtil.isEmpty(url))
                        continue;

                    SR sr = new SR(-1, SRState.Stop, url);
                    VideoFormat format = mStreaming.size() > 0 ? mStreaming.get(0).vFormat : mSRVideoFormats.get(SRId.Streaming);
                    tmp = startSR(SRId.Streaming, sr, format);
                    if (!tmp.isSuccessful()) {
                        result.code = tmp.code;
                        result.message += tmp.message + ";";
                    }

                    mStreaming.add(sr);
                }

                return result;
            } else {
                return Result.SUCCESS;
            }
        }
    }

    private Result switchRecordingState(SRState state) {
        logAction("switchRecordingState", state);

        if (state == SRState.Start) {
            if (mRecording.state == SRState.Stop) {
                return startSR(SRId.Recording, mRecording, mSRVideoFormats.get(SRId.Recording));
            } else {
                return unPauseSR(SRId.Recording, mRecording);
            }
        } else if (state == SRState.Pause) {
            return pauseSR(SRId.Recording, mRecording);
        } else if (state == SRState.Stop) {
            return stopSR(SRId.Recording, mRecording);
        } else {
            throw new IllegalStateException("Unexpected state: " + state.name());
        }
    }

    private Result startSR(SRId type, SR sr, VideoFormat vFormat) {
        if (sr.epId >= 0)
            return genError("startSR", BaseError.LOGICAL_ERROR,
                    "logical error: residuary outputId(" + sr.epId + ")",
                    "内部逻辑错误(上次残留信息),无法开始" + type.desc);
        if (mDisplays.get(SurfaceId.PGM).state != State.Done)
            return genError("startSR", BaseError.ACTION_ILLEGAL,
                    "Output window is not ready",
                    "PGM窗口未就绪,无法开始" + type.desc);

        if (type == SRId.Recording) {
            // update recording url
            RecProp recProp = new RecProp(mRecProp);
            String namePrefix = CoreUtils.fillFormatName(recProp.nameFormat,
                    mExtHelper.getUserName(), mExtHelper.getTitle());
            sr.url = recProp.toUrl(namePrefix);

            // create file output
            RecSplitStrategy strategy = new RecSplitStrategy(recProp.splitMode, recProp.splitValue);
            sr.epId = mEndpoint.epCreateFileOutput(sr.url, strategy, null);
        } else if (type == SRId.Streaming) {
            // create net output
            sr.epId = mEndpoint.epCreateNetOutput(sr.url);
        } else {
            throw new IllegalStateException("Unexpected value: " + type.name());
        }

        if (sr.epId < 0)
            return genError("startSR", BaseError.INTERNAL_ERROR, "epCreateXXXOutput failed: " + sr.epId,
                    "内部错误(创建输出失败:" + sr.epId + "),无法开始" + type.desc);

        // set output audio stream
        StreamDesc audio_tx_stream = new StreamDesc(DataType.AUDIO, "audio-1", "主音频", EPDir.Outgoing);
        int ret = mEndpoint.epSetOutputStream(sr.epId, audio_tx_stream,
                mSRAudioFormat, EPObjectType.Mixer, mOutputMixer.epId);
        if (ret != 0) {
            stopSR(type, sr);
            return genError("startSR", BaseError.INTERNAL_ERROR, "epSetOutputAudioStream failed: " + ret,
                    "内部错误(添加音频流到输出失败:" + ret + "),无法开始" + type.desc);
        }

        // set output video stream
        StreamDesc video_tx_stream = new StreamDesc(DataType.VIDEO, "video-1", "主视频", EPDir.Outgoing);
        ret = mEndpoint.epSetOutputStream(sr.epId, video_tx_stream,
                vFormat, EPObjectType.Display, mDisplays.get(SurfaceId.PGM).epId);
        if (ret != 0) {
            stopSR(type, sr);
            return genError("startSR", BaseError.INTERNAL_ERROR, "epSetOutputVideoStream failed: " + ret,
                    "内部错误(添加视频流到输出失败:" + ret + "),无法开始" + type.desc);
        }

        // start output
        ret = mEndpoint.epStartOutput(sr.epId);
        if (ret != 0) {
            stopSR(type, sr);
            return genError("startSR", BaseError.INTERNAL_ERROR, "epStartOutput failed: " + ret,
                    "内部错误(启动输出失败:" + ret + "),无法开始" + type.desc);
        }

        // update status
        sr.state = SRState.Start;
        sr.cumulativeTimeMS = 0;
        sr.startTime = System.currentTimeMillis();
        sr.aFormat = new AudioFormat(mSRAudioFormat);
        sr.vFormat = new VideoFormat(vFormat);

        return Result.SUCCESS;
    }

    private Result pauseSR(SRId type, SR sr) {
        if (sr.epId < 0)
            return genError("pauseSR", BaseError.LOGICAL_ERROR,
                    "logical error, lost outputId: " + sr.epId,
                    "内部逻辑错误(丢失信息),无法暂停" + type.desc);
        if (sr.state == SRState.Pause)
            return Result.SUCCESS;

        // pause output
        int ret = mEndpoint.epPauseOutput(sr.epId);
        if (ret != 0)
            return genError("pauseSR", BaseError.INTERNAL_ERROR, "epPauseOutput failed: " + ret,
                    "内部错误(暂停输出失败:" + ret + "),无法暂停" + type.desc);

        // update status
        sr.state = SRState.Pause;
        sr.cumulativeTimeMS += (System.currentTimeMillis() - sr.startTime);
        sr.startTime = 0;

        return Result.SUCCESS;
    }

    private Result unPauseSR(SRId type, SR sr) {
        if (sr.epId < 0)
            return genError("unPauseSR", BaseError.LOGICAL_ERROR,
                    "logical error, lost outputId: " + sr.epId,
                    "内部逻辑错误(丢失信息),无法继续" + type.desc);
        if (sr.state == SRState.Start)
            return Result.SUCCESS;

        // unPause output
        int ret = mEndpoint.epUnPauseOutput(sr.epId);
        if (ret != 0)
            return genError("unPauseSR", BaseError.INTERNAL_ERROR, "epUnPauseOutput failed: " + ret,
                    "内部错误(继续输出失败:" + ret + "),无法继续" + type.desc);

        // update status
        sr.state = SRState.Start;
        sr.startTime = System.currentTimeMillis();

        return Result.SUCCESS;
    }

    private Result stopSR(SRId type, SR sr) {
        if (sr.epId < 0) {
            if (sr.state == SRState.Stop)
                return Result.SUCCESS;
            else
                return genError("stopSR", BaseError.LOGICAL_ERROR,
                        "logical error, lost outputId: " + sr.epId,
                        "内部逻辑错误(丢失信息),无法停止" + type.desc);
        }

        // stop and release output
        if (sr.state != SRState.Stop)
            mEndpoint.epStopOutput(sr.epId);
        mEndpoint.epReleaseOutput(sr.epId);

        // update status
        sr.epId = -1;
        sr.state = SRState.Stop;
        sr.url = null;
        sr.cumulativeTimeMS = 0;
        sr.startTime = 0;
        sr.aFormat = null;
        sr.vFormat = null;

        return Result.SUCCESS;
    }

    private Result/*Caller*/ makeCall(Source config, ChannelId channelId) {
        // create and init a caller
        Result result = createAndInitCaller(config, channelId);
        if (!result.isSuccessful()) {
            int callerId = (int) result.data;
            if (callerId >= 0)
                mEndpoint.epReleaseCaller(callerId);
            return result;
        }

        Caller caller = (Caller) result.data;

        // flush the mixer for the caller
        flushCallerMixer(caller);

        // make call or accept the call
        int ret = mEndpoint.epCall(caller.epId);

        // check the result
        if (ret != 0) {
            releaseCaller(caller);
            return genError("makeCall", BaseError.INTERNAL_ERROR,
                    "epCall failed: " + ret, "内部错误, 呼叫失败: " + ret);
        }

        return Result.buildSuccess(caller);
    }

    private Result/*Caller*/ createAndInitCaller(Source config, ChannelId channelId) {
        // create a caller to call the remote
        int callerId = mEndpoint.epCreateCaller(config.url);
        if (callerId < 0)
            return genError("makeCall", BaseError.INTERNAL_ERROR, callerId,
                    "epCreateCaller failed: " + callerId, "内部错误, 创建呼叫失败: " + callerId);

        // gen caps and bandwidth by SR formats
        VideoFormat videoFormat = mSRVideoFormats.get(SRId.Streaming);
        AudioCapabilities aCaps = getAudioCapability(LXConst.DEFAULT_CALLING_AUDIO_CODECS);
        VideoCapabilities vCaps = getVideoCapability(Arrays.asList(videoFormat));

        // set properties for the caller
        int ret = mEndpoint.epSetCallBandwidth(callerId, videoFormat.bandwidth);
        ret += mEndpoint.epSetCallVideoCapabilities(callerId, vCaps);
        ret += mEndpoint.epSetCallAudioCapabilities(callerId, aCaps);
        if (ret != 0)
            return genError("initCaller", BaseError.INTERNAL_ERROR, callerId,
                    "epSetXXX failed: " + ret, "内部错误, 设置呼叫参数失败: " + ret);

        // create a mixer for this caller
        int mixer = mEndpoint.epAddMixer();
        if (mixer < 0)
            return genError("init", BaseError.INTERNAL_ERROR, callerId,
                    "epAddMixer failed: " + mixer, "内部错误, 创建混音失败: " + mixer);

        // keep this caller
        float volume = getDefaultVolume();
        Caller caller = new Caller(config, channelId, callerId, State.Doing,
                volume, isMute(volume), videoFormat);
        caller.mixer = new Mixer();
        caller.mixer.epId = mixer;

        return Result.buildSuccess(caller);
    }

    private void releaseCaller(Caller caller) {
        if (caller.connState == State.None) {
            // release the caller
            mEndpoint.epReleaseCaller(caller.epId);
        } else {
            // hangup first and remove it
            if (!hangupCaller(caller).isSuccessful())
                mEndpoint.epReleaseCaller(caller.epId);
        }

        // release the mixer of caller
        if (caller.mixer != null && caller.mixer.epId >= 0) {
            mEndpoint.epRemoveMixer(caller.mixer.epId);
            caller.mixer = null;
        }
    }

    private Result hangupCaller(Caller caller) {
        // release all stream of the caller
        List<Tuple<Integer, StreamDesc>> streams = new LinkedList<>();
        for (AVStream stream: caller.aStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (AVStream stream: caller.vStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (AVStream stream: caller.aTxStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (AVStream stream: caller.vTxStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (Tuple<Integer, StreamDesc> stream: streams)
            onStreamClose(EPObjectType.Caller, caller.epId, stream.first, stream.second);

        // hangup
        int ret = mEndpoint.epHangup(caller.epId);
        if (ret != 0) {
            // update status
            caller.connState = State.None;
            return genError("hangup", BaseError.INTERNAL_ERROR,
                    "epHangup failed: " + ret, "内部错误,挂断失败: " + ret);
        }

        return Result.SUCCESS;
    }

    private Input getSourceByEPId(int epId) {
        for (Input input: mInputs.values()) {
            if (input.config.type != SourceType.Caller && input.epId == epId)
                return input;
        }
        return null;
    }

    private Caller getCallerByEPId(int epId) {
        for (Input input: mInputs.values()) {
            if (input.config.type == SourceType.Caller && input.epId == epId)
                return (Caller) input;
        }
        return null;
    }

    private DisplayConfig transLayout2Config(Layout layout, TransitionDesc transition) {
        List<Overlay> inOverlays = layout.getOverlays();
        List<DisplayOverlay> outOverlays = new LinkedList<>();

        // skip empty layout
        if (inOverlays == null || inOverlays.size() == 0)
            return DisplayConfig.buildOverlays(outOverlays);

        // loop to fill overlay
        for (Overlay inOverlay: inOverlays) {

            // skip invalid overlay
            if (!inOverlay.isValid()) {
                LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config skip invalid overlay: " + new Gson().toJson(inOverlay));
                continue;
            }

            if (inOverlay.type == Overlay.Type.Image) {
                Overlay.Image in = (Overlay.Image) inOverlay;
                DisplayOverlay.Image out = new DisplayOverlay.Image(in.imagePath, in.dst);
                out.setSrcRegion(in.src);
                out.setZIndex(in.zIndex);
                out.setTransparency(in.transparency);
                outOverlays.add(out);
            } else if (inOverlay.type == Overlay.Type.Bitmap) {
                LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config skip Bitmap overlay");
            } else if (inOverlay.type == Overlay.Type.Stream) {
                Overlay.Stream in = (Overlay.Stream) inOverlay;

                // check channel id
                ChannelId channel = in.channel;
                if (!channel.isRx || channel.onlyAudio) {
                    LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config skip invalid overlay$2: " + new Gson().toJson(inOverlay));
                    continue;
                }

                // get the input by channel id
                Input input = mInputs.get(channel);
                if (input == null) {
                    LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config, the input channel is empty: " + channel.name());
                    continue;
                }
                if (input.vStreams.size() == 0) {
                    LogUtil.d(CoreUtils.TAG, TAG, "transLayout2Config, the input channel has no video stream: " + channel.name());
                    continue;
                }

                // find the first matched stream
                int extId = -1;
                int mainId = -1;
                for (AVStream stream: input.vStreams) {
                    if (stream.isDecReady()) {
                        if (mainId < 0 && stream.type == DataType.VIDEO)
                            mainId = stream.getDecId();
                        else if (extId < 0 && stream.type == DataType.VIDEO_EXT)
                            extId = stream.getDecId();
                    }
                }
                int decId = mainId < 0 ? extId : mainId;
                if (decId < 0) {
                    LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config, the input channel's video stream is not ready: " + channel.name());
                    continue;
                }

                // build overlay for this stream
                DisplayOverlay.Stream out = new DisplayOverlay.Stream(decId, in.dst);
                out.setSrcRegion(input.srcRegion);
                out.setZIndex(in.zIndex);
                out.setTransparency(in.transparency);
                outOverlays.add(out);
            } else {
                LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config skip unknown overlay: " + new Gson().toJson(inOverlay));
            }
        }

        // build DisplayConfig with overlays
        DisplayConfig config = DisplayConfig.buildOverlays(outOverlays);

        // set background color
        String bgColor = layout.getBackgroundColor();
        if (bgColor != null)
            config.setBackgroundColor(bgColor);

        // set transition
        if (transition != null && transition.type != TransitionMode.Unknown &&
                transition.duration > 0)
            config.setTransition(transition);

        return config;
    }

    private MixerTracks getMixerTracks(Map<ChannelId, MixMode> mixModes, Layout layout) {
        List<MixerTracks.Track> tracks = new LinkedList<>();
        List<Input> inputs = new LinkedList<>();

        // find out all inputs which mixMode is ON
        for (Map.Entry<ChannelId, MixMode> entry: mixModes.entrySet()) {
            if (entry.getValue() == MixMode.On) {
                ChannelId channel = entry.getKey();
                if (channel == ChannelId.Output) {
                    LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Tracks, not support output channel");
                    continue;
                }

                Input input = mInputs.get(channel);
                if (input == null)
                    LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Tracks, the input channel is empty: " + channel.name());
                else
                    inputs.add(input);
            }
        }

        // find out all inputs which mixMode is AFV and contained by the layout
        if (layout != null && layout.getOverlays() != null) {
            List<Overlay> inOverlays = layout.getOverlays();

            for (Overlay inOverlay : inOverlays) {
                // skip invalid overlay
                if (!inOverlay.isValid()) {
                    LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Tracks skip invalid overlay: " + new Gson().toJson(inOverlay));
                    continue;
                }

                // just try to add track for stream overlay
                if (inOverlay.type == Overlay.Type.Stream) {
                    Overlay.Stream in = (Overlay.Stream) inOverlay;

                    // get channel id
                    ChannelId channel = in.channel;
                    if (!channel.isRx || channel.onlyAudio) {
                        LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Tracks skip invalid overlay': " + new Gson().toJson(inOverlay));
                        continue;
                    }

                    // get the input by channel id
                    Input input = mInputs.get(channel);
                    if (input == null) {
                        LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Tracks, the input channel is empty': " + channel.name());
                        continue;
                    }

                    // skip non-AFV or muted channel
                    MixMode mixMode = mixModes.get(channel);
                    if (mixMode == null || mixMode != MixMode.AFV || input.muted)
                        continue;

                    inputs.add(input);
                }
            }
        }

        // loop to fill tracks
        for (Input input: inputs) {
            // skip empty audio streams
            if (input.connState != State.Done || input.aStreams.size() == 0)
                continue;

            // add all matched stream
            for (AVStream stream: input.aStreams) {
                if (stream.isDecReady()) {
                    MixerTracks.Track track = new MixerTracks.Track(stream.getDecId(), input.volume);
                    tracks.add(track);
                }
            }
        }

        return new MixerTracks(tracks);
    }

    private void startDecodingStream(Input input, AVStream stream) {
        // start decoding rx stream
        int decId;
        if (input.config.type == SourceType.Caller)
            decId = mEndpoint.epStartRxStreamDecoding(input.epId, stream.id);
        else
            decId = mEndpoint.epStartSrcStreamDecoding(input.epId, stream.id);
        if (decId < 0) {
            LogUtil.w(CoreUtils.TAG, TAG, "epStartXXStreamDecoding failed: " + decId);
            return;
        }

        stream.setDecId(decId);
        // there is no audio stream decoding ready callback
        stream.setDecReady(stream.type == DataType.AUDIO ? true : false);

        // apply audio stream change
        if (stream.type == DataType.AUDIO) {
            onAudioStreamStateChanged(input, true);
        } else {
            // delay to apply video stream while it is ready
        }
    }

    private void onRxStreamOpen(Input input, int streamId, StreamDesc desc, Object format) {
        List<AVStream> streams = desc.type == DataType.AUDIO ? input.aStreams : input.vStreams;

        // just deal with all audio stream and the first video stream, ignore others
        if ((desc.type == DataType.VIDEO && streams.size() > 0) ||
                (desc.type != DataType.VIDEO && desc.type != DataType.AUDIO)) {
            LogUtil.w(CoreUtils.TAG, TAG, "onRxStreamOpen#" + input.config.type.name() + ", ignore other stream#" + desc.type.name);
            return;
        }

        // keep the stream
        AVStream stream = new AVStream(streamId, desc, format);
        streams.add(stream);

        // skip useless audio stream
        if (desc.type == DataType.AUDIO && !input.config.useAudio) {
            LogUtil.i(CoreUtils.TAG, TAG, "onRxStreamOpen#" + input.channelId.name() + ", ignore useless audio stream");
            return;
        }

        // start to decode this stream
        startDecodingStream(input, stream);
    }

    private void onTxStreamOpen(Caller caller, int streamId, StreamDesc desc, Object format) {
        AVStream stream = new AVStream(streamId, desc, format);

        if (desc.type == DataType.AUDIO) {
            // tx audio channel is ready, start to encode for caller's mixer
            caller.aTxStreams.add(stream);

            AudioFormat audio = (AudioFormat) format;
            int ret = mEndpoint.epStartTxStream(caller.epId, streamId, audio, EPObjectType.Mixer, caller.mixer.epId);
            if (ret != 0) {
                LogUtil.w(CoreUtils.TAG, TAG,"onTxStreamOpen, epStartTxStream failed: " + ret);
                return;
            }
            stream.onEncoding();

        } else if (desc.type == DataType.VIDEO) {
            // tx video channel is ready, start to encode for tx display
            caller.vTxStreams.add(stream);

            // check stream format whether is matched with meeting param
            VideoFormat negFormat = (VideoFormat) format;
            Result result = isSimilarFormat(negFormat, caller.vFormat);
            if (!result.isSuccessful()) {
                List<String> diff = (List<String>) result.data;
                String message = String.format("caller#%d(%s) video format[%s] is not matched with expected[%s]",
                        caller.epId, caller.config.name, diff.get(0), diff.get(1));
                String hint = String.format("呼出视频协商失败: 远端[%s]协商的格式[%s]与互动配置[%s]不符", caller.config.name, diff.get(0), diff.get(1));
                genWarning("onTxStreamOpen", result.code,
                        message + ", but ignore this warning and force to use " + diff.get(1),
                        hint + ",但将忽略该警告并强制使用" + diff.get(1), false);
            }

            // check PGM display
            Display display = mDisplays.get(SurfaceId.PGM);
            if (display == null || display.state != State.Done) {
                LogUtil.w(CoreUtils.TAG, TAG,"onTxStreamOpen, PGM display is not ready");
                return;
            }

            // start this tx stream
            int ret = mEndpoint.epStartTxStream(caller.epId, streamId, caller.vFormat, EPObjectType.Display, display.epId);
            if (ret != 0) {
                LogUtil.w(CoreUtils.TAG, TAG,"onTxStreamOpen, epStartTxStream failed: " + ret);
                return;
            }
            stream.onEncoding();
        }
    }

    private void onRxStreamClose(Input input, int streamId, StreamDesc desc) {
        // stop decoding rx stream
        if (input.config.type == SourceType.Caller)
            mEndpoint.epStopRxStreamDecoding(input.epId, streamId);
        else
            mEndpoint.epStopSrcStreamDecoding(input.epId, streamId);

        // remove the stream
        AVStream stream = input.removeStream(desc.type, streamId);
        if (stream == null)
            return;
        boolean changed = stream.isDecReady();
        stream.onStopped();

        // apply stream change
        if (changed) {
            if (desc.type == DataType.AUDIO)
                onAudioStreamStateChanged(input, false);
            else
                onVideoStreamStateChanged(input, false);
        }
    }

    private void onTxStreamClose(Caller caller, int streamId, StreamDesc desc) {
        mEndpoint.epStopTxStream(caller.epId, streamId);

        AVStream stream = caller.removeTxStream(desc.type, streamId);
        if (stream == null)
            return;
        stream.onStopped();
    }

    private void onSurfaceReady(SurfaceId id) {
        int index = id.ordinal();
        if (index >= SurfaceId.IN1.ordinal() && index <= SurfaceId.IN4.ordinal()) {
            ChannelId channelId = SurfaceId2ChannelId(id);
            if (channelId == null)
                return;

            Input input = mInputs.get(channelId);
            if (input != null && input.getDecodingIds(DataType.VIDEO).size() > 0)
                flushInSurface(id, channelId, true);
        } else if (id == SurfaceId.PVW) {
            // do nothing
        } else if (id == SurfaceId.PGM) {
            int displayId = mDisplays.get(SurfaceId.PGM).epId;

            for (Input input: mInputs.values()) {
                if (input.config.type == SourceType.Caller) {
                    Caller caller = (Caller) input;
                    for (AVStream stream: caller.vTxStreams) {
                        if (!stream.isEncoding()) {
                            int ret = mEndpoint.epStartTxStream(caller.epId, stream.id, caller.vFormat,
                                    EPObjectType.Display, displayId);
                            if (ret != 0)
                                LogUtil.w(CoreUtils.TAG, TAG,"onSurfaceReady, epStartTxStream failed: " + ret);
                            else
                                stream.onEncoding();
                        }
                    }
                }
            }
        }
    }

    private void onAudioStreamStateChanged(Input input, boolean ready) {
        ChannelId channel = input.channelId;

        // check and flush output mixer
        MixMode mixMode = mOutputMixer.mixModes.get(channel);
        if (mixMode != null && mixMode != MixMode.Off) {
            boolean flush = mixMode == MixMode.On;
            if (mixMode == MixMode.AFV) {
                Display display = mDisplays.get(SurfaceId.PGM);
                flush = display.contains(channel);
            }

            if (flush)
                flushOutputMixer();
        }

        // check and flush echo mixer
        mixMode = mEchoMixer.mixModes.get(channel);
        if (mixMode != null && mixMode == MixMode.On)
            flushEchoMixer();

        if (mObserver != null)
            mObserver.onStreamStateChanged(channel, DataType.AUDIO, ready);
    }

    private void onVideoStreamStateChanged(Input input, boolean ready) {
        ChannelId channel = input.channelId;

        // check and flush PVW
        Display display = mDisplays.get(SurfaceId.PVW);
        if (display.state == State.Done && display.contains(channel))
            switchLayout(SurfaceId.PVW, display.layout, TransitionDesc.buildEmpty());

        // check and flush PGM
        display = mDisplays.get(SurfaceId.PGM);
        if (display.state == State.Done && display.contains(channel))
            switchLayout(SurfaceId.PGM, display.layout, TransitionDesc.buildEmpty());

        // check and flush IN#?
        SurfaceId surfaceId = ChannelId2SurfaceId(channel);
        if (surfaceId != null)
            flushInSurface(surfaceId, channel, input.getDecodingIds(DataType.VIDEO).size() > 0);

        if (mObserver != null)
            mObserver.onStreamStateChanged(channel, DataType.VIDEO, ready);
    }

    private void flushInSurface(SurfaceId surfaceId, ChannelId channel, boolean videoReady) {
        Display display = mDisplays.get(surfaceId);
        if (display.state == State.Done) {
            Layout layout = videoReady ? new Layout().setOverlays(
                    Arrays.asList(new Overlay.Stream(channel, Region.buildFull()))) :
                    Layout.buildEmpty();
            switchLayout(surfaceId, layout, TransitionDesc.buildEmpty());
        }
    }

    private void onVideoStreamDecodingReady(EPObjectType parentType, int parentId, int mediaSourceId, boolean ready) {
        String name = (parentType == EPObjectType.Source ? "source#" : "caller#") + parentId;
        Input input = parentType == EPObjectType.Source ? getSourceByEPId(parentId) : getCallerByEPId(parentId);

        if (input == null) {
            LogUtil.w(CoreUtils.TAG, TAG, "onVideoStreamDecodingReady, from unknown " + name);
            return;
        }
        if (input.connState == State.None) {
            LogUtil.w(CoreUtils.TAG, TAG, "onVideoStreamDecodingReady, " + name + " is not connected");
            return;
        }

        AVStream stream = input.findStreamByDecId(DataType.VIDEO, mediaSourceId);
        if (stream == null) {
            LogUtil.w(CoreUtils.TAG, TAG, "onVideoStreamDecodingReady, from " + name + " and can not find the stream by mediaSourceId#" + mediaSourceId);
            return;
        }

        LogUtil.i(CoreUtils.TAG, TAG, "onVideoStreamDecodingReady, video of " + name + " is ready: " + ready);
        stream.setDecReady(ready);

        onVideoStreamStateChanged(input, true);
    }

    ///////////////// static utils

    private static boolean isMute(float volume) {
        return volume <= 0.01f;
    }

    private static float getVolume(float volume, boolean muted) {
        return muted ? 0.0f : volume;
    }

    private static void logAction(String action, Object... params) {
        LXConst.logAction(CoreUtils.TAG, TAG, action, params);
    }

    private static Result genError(String action, int error, String message, String hint) {
        return genError(action, error, message, hint, true);
    }

    private static Result genError(String action, int error, Object data, String message, String hint) {
        Result result = genError(action, error, message, hint, true);
        result.data = data;
        return result;
    }

    private static Result genError(String action, int error, String message, String hint, boolean messageBox) {
        LogUtil.w(CoreUtils.TAG, TAG, action + ":" + message);
        if (messageBox)
            LXConst.gMessageBox.add("ERROR", TAG + "::" + action, hint);
        return new Result(error, hint);
    }

    private static Result genWarning(String action, int error, String message, String hint) {
        return genWarning(action, error, message, hint, true);
    }

    private static Result genWarning(String action, int error, String message, String hint, boolean messageBox) {
        LogUtil.i(CoreUtils.TAG, TAG, action + ":" + message);
        if (messageBox)
            LXConst.gMessageBox.add("WARNING", TAG + "::" + action, hint);
        return new Result(error, hint);
    }

    private static void fixConfig(EPFixedConfig config) {
        String logName;
        int logLevel = LogUtil.getLogLevel();
        switch (logLevel) {
            case Log.VERBOSE:
                logName = "VERBOSE";
                break;
            case Log.DEBUG:
                logName = "DEBUG";
                break;
            case Log.INFO:
                logName = "INFO";
                break;
            case Log.WARN:
                logName = "WARNING";
                break;
            case Log.ERROR:
                logName = "ERROR";
                break;
            default:
                logName = null;
                break;
        }

        if (logName != null && !CompareHelper.isEqual(config.log_level, logName)) {
            LogUtil.w(CoreUtils.TAG, TAG, "fix jni log level to " + logName);
            config.log_level = logName;
        }
    }

    private static AudioCapabilities getAudioCapability(List<AudioCodec> aCodecs) {
        List<AudioCapability> audio = new LinkedList<>();
        for (AudioCodec codec: aCodecs) {
            AudioCapability capability = new AudioCapability(codec.format);
            audio.add(capability);
        }

        AudioCapabilities.Capabilities caps = new AudioCapabilities.Capabilities(audio);
        return new AudioCapabilities(caps, caps);
    }

    private static VideoCapabilities getVideoCapability(List<VideoFormat> formats) {
        List<VideoCapability> video = new LinkedList<>();

        for (VideoFormat format: formats) {
            VideoCapability capability = new VideoCapability(format.codec, format.profile,
                    format.resolution, format.framerate);
            capability.setKeyFrameInterval(format.getKeyIntervalInFrames());

            boolean added = false;
            for (VideoCapability cap: video) {
                if (cap.isEqual(capability)) {
                    added = true;
                    break;
                }
            }

            if (!added)
                video.add(capability);
        }

        VideoCapabilities.Capabilities capabilities = new VideoCapabilities.Capabilities(video, Arrays.asList());
        return new VideoCapabilities(capabilities, capabilities);
    }

    private static ChannelId SurfaceId2ChannelId(SurfaceId id) {
        switch (id) {
            case IN1:
                return ChannelId.IN1;
            case IN2:
                return ChannelId.IN2;
            case IN3:
                return ChannelId.IN3;
            case IN4:
                return ChannelId.IN4;
            default:
                return null;
        }
    }

    private static SurfaceId ChannelId2SurfaceId(ChannelId id) {
        switch (id) {
            case IN1:
                return SurfaceId.IN1;
            case IN2:
                return SurfaceId.IN2;
            case IN3:
                return SurfaceId.IN3;
            case IN4:
                return SurfaceId.IN4;
            default:
                return null;
        }
    }

    private static Result isSimilarFormat(VideoFormat real, VideoFormat expected) {
        if (!CompareHelper.isEqual(real.resolution, expected.resolution))
            return new Result(BaseError.INVALID_PARAM, "not matched", Arrays.asList(real.resolution.name, expected.resolution.name));
        if (!CompareHelper.isEqual(real.codec, expected.codec))
            return new Result(BaseError.INVALID_PARAM, "not matched", Arrays.asList(real.codec.name, expected.codec.name));
        if (!CompareHelper.isEqual(real.profile, expected.profile))
            return new Result(BaseError.INVALID_PARAM, "not matched", Arrays.asList("profile#" + real.profile.name, "profile#" + expected.profile.name));
        if (real.bandwidth == null)
            real.bandwidth = Bandwidth.Unknown;
        if (real.bandwidth.bps > (expected.bandwidth.bps * 2) ||
                expected.bandwidth.bps > (real.bandwidth.bps * 2))
            return new Result(BaseError.INVALID_PARAM, "not matched", Arrays.asList(real.bandwidth.name, expected.bandwidth.name));
        if (real.framerate > (expected.framerate * 1.2) ||
                expected.framerate > (real.framerate * 1.2))
            return new Result(BaseError.INVALID_PARAM, "not matched", Arrays.asList(real.framerate + "fps", expected.framerate + "fps"));
        return Result.SUCCESS;
    }

    private class Display {
        public int epId;
        public State state;
        public Size size;
        public Layout layout;
        public DisplayConfig config;

        public Display() {
            reset();
        }

        public Display(int epId, State state, Size size,
                       Layout layout, DisplayConfig config) {
            this.epId = epId;
            this.state = state;
            this.size = size;
            this.layout = layout;
            this.config = config;
        }

        public void reset() {
            this.epId = -1;
            this.state = State.None;
            this.size = new Size(0, 0);
            this.layout = Layout.buildEmpty();
            this.config = DisplayConfig.buildEmptyOverlays();
        }

        public boolean contains(ChannelId channel) {
            if (layout == null || layout.getOverlays() == null)
                return false;
            for (Overlay overlay: layout.getOverlays()) {
                if (overlay.type != Overlay.Type.Stream)
                    continue;
                Overlay.Stream stream = (Overlay.Stream) overlay;
                if (CompareHelper.isEqual(channel, stream.channel))
                    return true;
            }
            return false;
        }
    }

    private class Mixer {
        public int epId;
        public float volume;
        public boolean muted;
        public Map<ChannelId, MixMode> mixModes;
        public MixerTracks tracks;

        public Mixer() {
            this.epId = -1;
            this.volume = getDefaultVolume();
            this.muted = isMute(volume);
            this.mixModes = new HashMap<>(6);
            this.tracks = MixerTracks.buildEmpty();
        }

        public Mixer(int epId, float volume, Map<ChannelId, MixMode> mixModes, MixerTracks tracks) {
            this.epId = epId;
            this.volume = volume;
            this.muted = isMute(volume);
            this.mixModes = mixModes;
            this.tracks = tracks;
        }
    }

    private class SR {
        public int epId;
        public SRState state;
        public String url;

        public long cumulativeTimeMS;
        public long startTime;

        public AudioFormat aFormat;
        public VideoFormat vFormat;

        public SR(int epId, SRState state, String url) {
            this.epId = epId;
            this.state = state;
            this.url = url;
            this.cumulativeTimeMS = 0;
            this.startTime = 0;
        }
    }

    private class Caller extends Input {
        public final VideoFormat vFormat;

        // tx mixer(volume/muted/mixModes is deprecated)
        public Mixer mixer;

        // tx streams
        public List<AVStream> aTxStreams = new LinkedList<>();
        public List<AVStream> vTxStreams = new LinkedList<>();

        public Caller(Source config, ChannelId channelId, int epId, State connState,
                      float volume, boolean muted, VideoFormat vFormat) {
            super(config, channelId, epId, connState, volume, muted);
            this.vFormat = vFormat;
        }

        public AVStream removeTxStream(DataType type, int streamId) {
            List<AVStream> streams = type == DataType.AUDIO ? aTxStreams : vTxStreams;
            Iterator<AVStream> it = streams.iterator();
            while (it.hasNext()) {
                AVStream stream = it.next();
                if (streamId == stream.id) {
                    it.remove();
                    return stream;
                }
            }

            return null;
        }
    }
}
