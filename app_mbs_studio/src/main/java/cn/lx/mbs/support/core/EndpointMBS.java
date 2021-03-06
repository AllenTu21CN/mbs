package cn.lx.mbs.support.core;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.google.gson.Gson;
import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Runnable3;
import com.sanbu.base.State;
import com.sanbu.base.Tuple;
import com.sanbu.board.BoardSupportClient;
import com.sanbu.board.EmptyBoardSupportClient;
import com.sanbu.media.AudioFormat;
import com.sanbu.media.Bandwidth;
import com.sanbu.media.DataType;
import com.sanbu.media.EPObjectType;
import com.sanbu.media.InputType;
import com.sanbu.media.TSLayout;
import com.sanbu.media.VideoFormat;
import com.sanbu.network.CallingDir;
import com.sanbu.network.CallingProtocol;
import com.sanbu.network.TransProtocol;
import com.sanbu.tools.AsyncHelper;
import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import java.io.File;
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
import cn.lx.mbs.support.structures.CommonOverlay;
import cn.lx.mbs.support.structures.OverlayDst;
import cn.lx.mbs.support.structures.OverlaySrc;
import cn.lx.mbs.support.structures.RecProp;
import cn.lx.mbs.support.structures.SRId;
import cn.lx.mbs.support.structures.SRState;
import cn.lx.mbs.support.structures.Source;
import cn.lx.mbs.support.structures.SurfaceId;
import cn.lx.mbs.support.utils.SPHelper;
import cn.sanbu.avalon.endpoint3.Endpoint3;
import cn.sanbu.avalon.endpoint3.structures.AudioInputDevice;
import cn.sanbu.avalon.endpoint3.structures.AudioOutputDevice;
import cn.sanbu.avalon.endpoint3.structures.RTSPSourceConfig;
import cn.sanbu.avalon.endpoint3.structures.VolumeReport;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioCapabilities;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioCapability;
import cn.sanbu.avalon.endpoint3.structures.jni.DisplayConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.DisplayOverlay;
import cn.sanbu.avalon.endpoint3.structures.jni.EPEvent;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.MixerTracks;
import cn.sanbu.avalon.endpoint3.structures.jni.RecSplitStrategy;
import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionMode;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapabilities;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapability;
import cn.sanbu.avalon.media.MediaJni;
import cn.sanbu.avalon.media.VideoEngine;

/*
 * EndpointMBS: ??????EP3?????????????????????SDK???????????????????????????EP????????????,?????????EndpointMBS
 * ????????????:
 *   . ??????MBS????????????????????????: ???????????????????????????????????????????????????????????????????????????
 *   . ???????????????????????????????????????????????????
 *   . ??????????????????????????????????????????, ?????????????????????????????????????????????,???????????????
 *   . ??????????????????
 * */
public class EndpointMBS implements Endpoint3.EPCallback, Endpoint3.StreamCallback,
        Endpoint3.CallingCallback, Endpoint3.RegisteringCallback {

    private static final String TAG = EndpointMBS.class.getSimpleName();

    // @brief ??????????????????????????????
    public interface Observer {
        // @brief ????????????
        void onRecordingFinished(String path);

        // @brief ?????????????????????
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
                VideoEngine.setNoSignalText("Camera?????????");
                VideoEngine.setLoadingText("?????????");
            }

            // init media engine jni environment
            BoardSupportClient client = new EmptyBoardSupportClient();
            MediaJni.initEnv(context, client, LXConst.USING_INTERNAL_NO_SIGNAL_IMG);

            // register custom programs
            List<TSLayout> customLayouts = TSLayout.getCustomLayouts();
            for (TSLayout layout : customLayouts)
                MediaJni.registerCustomProgram(layout.name(), layout.customProgram);

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

    /////////////////////////////// ????????????????????????

    // @brief ?????????EP-MBS;??????????????????????????????????????????
    // @param sp [IN], ?????????????????????
    // @param rtWorker [IN], ??????????????????
    // @param ob [IN], ???????????????
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

    // @brief ????????????
    // @return 0 is suc, or failed.
    public int release() {
        // block to release all handlers
        if (mRTWorker != null) {

            AsyncHelper async = new AsyncHelper();
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

                async.notify2(0);
            });

            int ret = (int) async.wait2(4000, -1);
            if (ret != 0)
                LogUtil.w(CoreUtils.TAG, TAG, "release timeout");
        }
        return 0;
    }

    /////////////////////////////// UI???????????????

    // @brief ????????????????????????
    // @param id [IN] ??????ID
    // @param handle [IN] ??????surface
    public void onSurfaceCreated(SurfaceId id, Surface handle) {
        logAction("onSurfaceCreated", id);

        asyncCall("onSurfaceCreated", null, () -> {
            Display display = mDisplays.get(id);
            if (display == null)
                return genError("onSurfaceCreated", BaseError.LOGICAL_ERROR,
                        id.name() + " display not support", "????????????????????????: " + id.desc);
            if (display.epId >= 0)
                return genError("onSurfaceCreated", BaseError.LOGICAL_ERROR,
                        id.name() + " display had created", "????????????????????????: " + id.desc);

            // create display
            display.epId = mEndpoint.epAddVisibleDisplay(handle);
            if (display.epId < 0)
                return genError("onSurfaceCreated", BaseError.INTERNAL_ERROR,
                        "epAddVisibleDisplay#" + id.name() + " failed: " + display.epId, "????????????????????????: " + id.desc);

            // update state
            display.state = State.Doing;
            return Result.SUCCESS;
        });
    }

    // @brief ????????????????????????
    // @param id [IN] ??????ID
    public void onSurfaceChanged(SurfaceId id, int format, int width, int height) {
        logAction("onSurfaceChanged", id, width, height);

        asyncCall("onSurfaceChanged", null, () -> {
            Display display = mDisplays.get(id);
            if (display == null)
                return genError("onSurfaceChanged", BaseError.LOGICAL_ERROR,
                        id.name() + " display not support", "????????????????????????: " + id.desc);
            if (display.epId < 0)
                return genError("onSurfaceChanged", BaseError.LOGICAL_ERROR,
                        "had not created display#" + id.name(), "????????????,?????????????????????: " + id.desc);

            // re-config display size
            int ret = mEndpoint.epChangeVisibleDisplay(display.epId, format, width, height);
            if (ret != 0)
                return genError("onSurfaceChanged", BaseError.INTERNAL_ERROR,
                        "epChangeVisibleDisplay#" + id.name() + " failed: " + ret, "????????????????????????: " + id.desc);

            // init display background color
            String color = mEPFixedConfig.background_color;
            mEndpoint.epConfigureDisplay(display.epId, DisplayConfig.buildBackgroundColor(color));

            display.size = new Size(width, height);
            display.state = State.Done;

            onSurfaceReady(id);
            return Result.SUCCESS;
        });
    }

    // @brief ????????????????????????
    // @param id [IN] ??????ID
    public void onSurfaceDestroyed(SurfaceId id) {
        logAction("onSurfaceDestroyed", id);

        asyncCall("onSurfaceDestroyed", null, () -> {
            Display display = mDisplays.get(id);
            if (display == null)
                return genError("onSurfaceDestroyed", BaseError.LOGICAL_ERROR,
                        id.name() + " display not support", "????????????????????????: " + id.desc);

            if (display.epId >= 0) {
                mEndpoint.epRemoveDisplay(display.epId);
                display.reset();
            }

            return Result.SUCCESS;
        });
    }

    /////////////////////////////// ???????????????

    // @brief ?????????????????????
    public List<Source> getAvailableSources() {
        if (isReady())
            return mAvailableSources;
        else
            return Collections.emptyList();
    }

    // @brief ???????????????
    public void addSource(Source source, Callback/*int sourceId*/ callback) {
        asyncCall("addSource", callback, () -> {
            int maxId = -1;
            for (Source src : mAvailableSources) {
                if (src.id == source.id ||
                        (src.type == source.type && src.url.equals(source.url)))
                    return genError("addSource", BaseError.ACTION_ILLEGAL,
                            "had added a same source: " + src.url, "?????????????????????,??????????????????");
                maxId = Math.max(maxId, src.id);
            }

            logAction("addSource", source);

            int sourceId = source.id >= 0 ? source.id : maxId + 1;
            mAvailableSources.add(Source.copy(sourceId, source));

            mSPHelper.setAvailableSources(mAvailableSources);
            return Result.buildSuccess(sourceId);
        });
    }

    // @brief ???????????????(??????????????????????????????)
    public void removeSource(int sourceId, Callback callback) {
        asyncCall("removeSource", callback, () -> {
            for (int i = 0; i < mAvailableSources.size(); ++i) {
                if (mAvailableSources.get(i).id == sourceId) {
                    logAction("removeSource", sourceId);
                    mAvailableSources.remove(i);

                    mSPHelper.setAvailableSources(mAvailableSources);
                    return Result.SUCCESS;
                }
            }

            return genError("removeSource", BaseError.TARGET_NOT_FOUND,
                    "not found the target source#" + sourceId, "????????????????????????");
        });
    }

    // @brief ???????????????(??????????????????????????????)
    public void updateSource(Source source, Callback callback) {
        asyncCall("updateSource", callback, () -> {
            for (int i = 0; i < mAvailableSources.size(); ++i) {
                if (mAvailableSources.get(i).id == source.id) {
                    logAction("updateSource", source);
                    mAvailableSources.remove(i);
                    mAvailableSources.add(source);

                    mSPHelper.setAvailableSources(mAvailableSources);
                    return Result.SUCCESS;
                }
            }

            return genError("updateSource", BaseError.TARGET_NOT_FOUND,
                    "not found the target source#" + source.id, "????????????????????????");
        });
    }

    /////////////////////////////// ????????????????????????

    // @brief ???????????????????????????????????????
    public AudioFormat getSRAudioFormat() {
        if (isReady())
            return mSRAudioFormat;
        else
            return LXConst.DEFAULT_SR_AUDIO_FORMAT;
    }

    // @brief ???????????????????????????????????????(????????????)
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

    // @brief ???????????????????????????????????????
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

    // @brief ???????????????????????????????????????(????????????)
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

    // @brief ??????????????????
    public List<String> getStreamingUrls() {
        if (isReady())
            return mStreamingUrls;
        else
            return Collections.emptyList();
    }

    // @brief ??????????????????(????????????)
    public void setStreamingUrls(List<String> urls, Callback callback) {
        asyncCall("setStreamingUrls", callback, () -> {

            // ignore the case that the streaming has been started

            logAction("setStreamingUrls", urls);

            mStreamingUrls = new ArrayList<>(urls);
            mSPHelper.setStreamingUrls(mStreamingUrls);
            return Result.SUCCESS;
        });
    }

    // @brief ??????????????????
    public RecProp getRecProp() {
        if (isReady())
            return mRecProp;
        else
            return LXConst.DEFAULT_SR_REC_PROP;
    }

    // @brief ??????????????????(????????????)
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

    /////////////////////////////// ????????????

    // @brief ??????????????????
    public void loadInput(ChannelId id, int sourceId, Callback callback) {
        asyncCall("loadInput", callback, () -> {
            if (mInputs.containsKey(id))
                return genError("loadInput", BaseError.ACTION_ILLEGAL,
                        "the input channel had loaded: " + id.name(),
                        id.name() + "????????????, ???????????????????????????????????????");

            Source source = mAvailableSources.get(sourceId);
            if (source == null)
                return genError("loadInput", BaseError.TARGET_NOT_FOUND,
                        "not find the source#" + sourceId,
                        "?????????????????????: " + sourceId);

            logAction("loadInput", id, sourceId);

            int epId;
            Caller caller = null;
            if (source.type == InputType.VideoCapture) {
                epId = mEndpoint.epAddOriginCamera(source.url, source.resolution);
            } else if (source.type == InputType.RTSP) {
                TransProtocol protocol = source.overTCP ? TransProtocol.TCP : TransProtocol.RTP;
                epId = mEndpoint.epAddRTSPSource(source.url,
                        new RTSPSourceConfig(LXConst.SOURCE_RECONNECTING, protocol));
            } else if (source.type == InputType.File) {
                if (!new File(source.path).exists())
                    return genError("loadInput", BaseError.TARGET_NOT_FOUND,
                            "not found the file: " + source.path,
                            "?????????????????????: " + source.path);

                epId = mEndpoint.epAddFileSource(source.url, source.loop ? -1 : 1);
            } else if (source.type == InputType.RTMP) {
                epId = mEndpoint.epAddNetSource(source.url, LXConst.SOURCE_RECONNECTING);
            } else if (source.type == InputType.RMSP) {
                epId = mEndpoint.epAddRMSPSource(source.url, source.videoFormat, source.audioFormat);
            } else if (source.type == InputType.Caller) {
                Result result = makeCall(source, id);
                if (!result.isSuccessful())
                    return result;
                caller = (Caller) result.data;
                epId = caller.epId;
            } else {
                return genError("loadInput", BaseError.ACTION_UNSUPPORTED,
                        "not support this type of source: " + source.type.name(),
                        "????????????????????????????????????: " + source.type.name());
            }

            if (epId < 0)
                return genError("loadInput", BaseError.INTERNAL_ERROR,
                        "epAddXXSource failed: " + epId,
                        "????????????,???????????????: " + epId);

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

    // @brief ??????????????????
    public void unloadInput(ChannelId id, Callback callback) {
        asyncCall("unloadInput", callback, () -> unloadInputImpl(id));
    }

    // @brief CUT????????????(??????)
    public void cutInputVideo(ChannelId id, Callback callback) {
        asyncCall("cutInputVideo", callback, () -> {
            Layout layout = new Layout().addOverlays(TSLayout.A, Arrays.asList(OverlaySrc.buildStream(id)));
            return switchLayout(SurfaceId.PGM, layout, TransitionDesc.buildEmpty());
        });
    }

    // @brief ??????????????????(??????)
    public void pauseInputVideo(ChannelId id, Callback callback) {
        asyncCall("pauseInputVideo", callback, () ->
                genError("pauseInputVideo", BaseError.ACTION_UNSUPPORTED,
                        "not implement", "????????????")
        );
    }

    /////////////////////////////// ????????????

    // @brief ?????????????????????????????????
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

    // @brief ?????????????????????????????????
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
                            "?????????????????????????????????: " + id.name());

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

    // @brief ??????????????????????????????????????????
    public Map<ChannelId, MixMode> getOutputAudioMixModes() {
        if (isReady())
            return mOutputMixer.mixModes;
        else
            return Collections.emptyMap();
    }

    // @brief ??????????????????????????????????????????
    public void setOutputAudioMixModes(ChannelId id, MixMode mode, Callback callback) {
        asyncCall("setOutputAudioMixModes", callback, () -> {
            MixMode current = mOutputMixer.mixModes.get(id);
            if (current != null && current == mode)
                return Result.SUCCESS;

            if (id == ChannelId.MIC0 && mode == MixMode.AFV)
                return genError("setOutputAudioMixModes", BaseError.ACTION_UNSUPPORTED,
                        "mic channel not supports AFV mode",
                        "MIC???????????????AFV??????");

            Input input = mInputs.get(id);
            if (input == null)
                return genError("setOutputAudioMixModes", BaseError.TARGET_NOT_FOUND,
                        "the input channel is not loaded: " + id.name(),
                        "?????????????????????????????????: " + id.name());

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

    // @brief ?????????????????????????????????
    public Map<ChannelId, Boolean> getSoloAudioOnOff() {
        if (!isReady())
            return Collections.emptyMap();

        Map<ChannelId, Boolean> result = new HashMap<>(mEchoMixer.mixModes.size());
        for (Map.Entry<ChannelId, MixMode> entry : mEchoMixer.mixModes.entrySet())
            result.put(entry.getKey(), entry.getValue() == MixMode.On ? true : false);
        return result;
    }

    // @brief ???????????????????????????????????????
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
                            "?????????????????????????????????: " + id.name());

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

    /////////////////////////////// ??????????????????

    // @brief ??????????????????
    public void switchSRState(SRId id, SRState state, Callback callback) {
        asyncCall("switchSRState", callback, () ->
                id == SRId.Streaming ? switchStreamingState(state) : switchRecordingState(state)
        );
    }

    /////////////////////////////// ????????????

    // @brief ??????????????????
    public void setPVWLayout(Layout layout, Callback callback) {
        asyncCall("setPVWLayout", callback, () ->
                switchLayout(SurfaceId.PVW, layout, TransitionDesc.buildEmpty()));
    }

    // @brief ?????????????????????????????????
    public void switchPVW2PGM(TransitionDesc transition, Callback callback) {
        asyncCall("switchPVW2PGM", callback, () -> {
            Display display = mDisplays.get(SurfaceId.PVW);
            return switchLayout(SurfaceId.PGM, display.layout, transition);
        });
    }

    /////////////////////////////// implementation of Endpoint3.EPCallback

    @Override
    public void onError(int errCode, String reason) {
        LogUtil.w(CoreUtils.TAG, TAG, "onError(EP): " + errCode + ", " + reason);
    }

    @Override
    public void onObjEvent(EPObjectType objType, int objId, EPEvent event, Object params) {
        asyncCall(() -> {
            switch (event) {
                case RecvReqOpenVideoExt:
                    // TODO
                    break;
                case FileWrittenCompleted:
                    // notify outside observer
                    if (mObserver != null)
                        mObserver.onRecordingFinished((String) params);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onVolumeReport(VolumeReport report) {
        LogUtil.d(CoreUtils.TAG, TAG, "onVolumeReport");
    }

    /////////////////////////////// implementation of Endpoint3.StreamCallback

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

                if (desc.direction == CallingDir.Incoming)
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
                    if (desc.direction == CallingDir.Outgoing)
                        mEndpoint.epStopTxStream(parent_id, stream_id);
                    else if (desc.direction == CallingDir.Incoming)
                        mEndpoint.epStopRxStreamDecoding(parent_id, stream_id);
                    return;
                }

                if (desc.direction == CallingDir.Incoming)
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
    public void onRxVideoStreamAvailabilityChanged(EPObjectType parentType, int parentId,
                                                   int decId, boolean ready) {
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

        AVStream stream = input.findStreamByDecId(DataType.VIDEO, decId);
        if (stream == null) {
            LogUtil.w(CoreUtils.TAG, TAG, "onVideoStreamDecodingReady, from " + name + " and can not find the stream by mediaSourceId#" + decId);
            return;
        }

        LogUtil.i(CoreUtils.TAG, TAG, "onVideoStreamDecodingReady, video of " + name + " is ready: " + ready);
        stream.setDecReady(ready);

        onVideoStreamStateChanged(input, true);
    }

    /////////////////////////////// implementation of Endpoint3.CallingCallback

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

    /////////////////////////////// implementation of Endpoint3.RegisteringCallback

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
                            "had been released", "????????????,???????????????", false);
                    CoreUtils.callbackResult(callback, result);
                }
            });
        } else {
            Result result = genError(action, BaseError.ACTION_ILLEGAL,
                    "init first", "????????????,???????????????", false);
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
        int ret = mEndpoint.epInit(mEPFixedConfig, this, this);
        if (ret != 0)
            return ret;

        mEndpoint.setCallingCallback(this);
        mEndpoint.setRegisteringCallback(this);

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

        for (Display display : mDisplays.values()) {
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
                Source source = Source.buildAudioCapture("???????????????", device.id);
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
            for (ChannelId channel : channels)
                unloadInputImpl(channel);
        }
    }

    private Result unloadInputImpl(ChannelId id) {
        Input input = mInputs.remove(id);
        if (input == null)
            return genError("unloadInput", BaseError.TARGET_NOT_FOUND,
                    "the input channel is not found: " + id.name(),
                    "???????????????: " + id.name());

        logAction("unloadInput", id);

        if (input.epId >= 0) {
            if (input.config.type == InputType.Caller)
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
                    id.desc + "???????????????,??????????????????");

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
                        "????????????,??????????????????: " + ret);
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
                    "epSetMixer failed: " + ret, "????????????,??????????????????????????????: " + ret);
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
                        "????????????,??????????????????: " + ret);
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
                        "????????????,??????????????????: " + ret);
            mOutputMixer.tracks = tracks;
        }

        // liked to flush caller's mixer
        flushCallerMixers();
        return Result.SUCCESS;
    }

    private Result flushCallerMixers() {
        if (!isReady())
            return Result.SUCCESS;

        for (Input input : mInputs.values()) {
            if (input.config.type != InputType.Caller)
                continue;

            flushCallerMixer((Caller) input);
        }

        return Result.SUCCESS;
    }

    private Result flushCallerMixer(Caller caller) {
        if (mOutputMixer == null)
            return genError("flushCallerMixer", BaseError.ACTION_ILLEGAL,
                    "init first", "????????????");
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
                    "??????caller???????????????");

        // update tracks
        mixer.tracks = tracks;
        return Result.SUCCESS;
    }

    private Result flushCallerVolume() {
        if (!isReady())
            return Result.SUCCESS;

        for (Input input : mInputs.values()) {
            if (input.config.type != InputType.Caller)
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

                for (String url : mStreamingUrls) {
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
                    "??????????????????(??????????????????),????????????" + type.desc);
        if (mDisplays.get(SurfaceId.PGM).state != State.Done)
            return genError("startSR", BaseError.ACTION_ILLEGAL,
                    "Output window is not ready",
                    "PGM???????????????,????????????" + type.desc);

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
                    "????????????(??????????????????:" + sr.epId + "),????????????" + type.desc);

        // set output audio stream
        StreamDesc audio_tx_stream = new StreamDesc(DataType.AUDIO, "audio-1", "?????????", CallingDir.Outgoing);
        int ret = mEndpoint.epSetOutputStream(sr.epId, audio_tx_stream,
                mSRAudioFormat, EPObjectType.Mixer, mOutputMixer.epId);
        if (ret != 0) {
            stopSR(type, sr);
            return genError("startSR", BaseError.INTERNAL_ERROR, "epSetOutputAudioStream failed: " + ret,
                    "????????????(??????????????????????????????:" + ret + "),????????????" + type.desc);
        }

        // set output video stream
        StreamDesc video_tx_stream = new StreamDesc(DataType.VIDEO, "video-1", "?????????", CallingDir.Outgoing);
        ret = mEndpoint.epSetOutputStream(sr.epId, video_tx_stream,
                vFormat, EPObjectType.Display, mDisplays.get(SurfaceId.PGM).epId);
        if (ret != 0) {
            stopSR(type, sr);
            return genError("startSR", BaseError.INTERNAL_ERROR, "epSetOutputVideoStream failed: " + ret,
                    "????????????(??????????????????????????????:" + ret + "),????????????" + type.desc);
        }

        // start output
        ret = mEndpoint.epStartOutput(sr.epId);
        if (ret != 0) {
            stopSR(type, sr);
            return genError("startSR", BaseError.INTERNAL_ERROR, "epStartOutput failed: " + ret,
                    "????????????(??????????????????:" + ret + "),????????????" + type.desc);
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
                    "??????????????????(????????????),????????????" + type.desc);
        if (sr.state == SRState.Pause)
            return Result.SUCCESS;

        // pause output
        int ret = mEndpoint.epPauseOutput(sr.epId);
        if (ret != 0)
            return genError("pauseSR", BaseError.INTERNAL_ERROR, "epPauseOutput failed: " + ret,
                    "????????????(??????????????????:" + ret + "),????????????" + type.desc);

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
                    "??????????????????(????????????),????????????" + type.desc);
        if (sr.state == SRState.Start)
            return Result.SUCCESS;

        // unPause output
        int ret = mEndpoint.epUnPauseOutput(sr.epId);
        if (ret != 0)
            return genError("unPauseSR", BaseError.INTERNAL_ERROR, "epUnPauseOutput failed: " + ret,
                    "????????????(??????????????????:" + ret + "),????????????" + type.desc);

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
                        "??????????????????(????????????),????????????" + type.desc);
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
                    "epCall failed: " + ret, "????????????, ????????????: " + ret);
        }

        return Result.buildSuccess(caller);
    }

    private Result/*Caller*/ createAndInitCaller(Source config, ChannelId channelId) {
        // create a caller to call the remote
        int callerId = mEndpoint.epCreateCaller(config.url);
        if (callerId < 0)
            return genError("makeCall", BaseError.INTERNAL_ERROR, callerId,
                    "epCreateCaller failed: " + callerId, "????????????, ??????????????????: " + callerId);

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
                    "epSetXXX failed: " + ret, "????????????, ????????????????????????: " + ret);

        // create a mixer for this caller
        int mixer = mEndpoint.epAddMixer();
        if (mixer < 0)
            return genError("init", BaseError.INTERNAL_ERROR, callerId,
                    "epAddMixer failed: " + mixer, "????????????, ??????????????????: " + mixer);

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
        for (AVStream stream : caller.aStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (AVStream stream : caller.vStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (AVStream stream : caller.aTxStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (AVStream stream : caller.vTxStreams)
            streams.add(new Tuple<>(stream.id, stream));
        for (Tuple<Integer, StreamDesc> stream : streams)
            onStreamClose(EPObjectType.Caller, caller.epId, stream.first, stream.second);

        // hangup
        int ret = mEndpoint.epHangup(caller.epId);
        if (ret != 0) {
            // update status
            caller.connState = State.None;
            return genError("hangup", BaseError.INTERNAL_ERROR,
                    "epHangup failed: " + ret, "????????????,????????????: " + ret);
        }

        return Result.SUCCESS;
    }

    private Input getSourceByEPId(int epId) {
        for (Input input : mInputs.values()) {
            if (input.config.type != InputType.Caller && input.epId == epId)
                return input;
        }
        return null;
    }

    private Caller getCallerByEPId(int epId) {
        for (Input input : mInputs.values()) {
            if (input.config.type == InputType.Caller && input.epId == epId)
                return (Caller) input;
        }
        return null;
    }

    private List<DisplayOverlay> genCommonOverlays(List<CommonOverlay> inOverlays) {
        if (inOverlays == null)
            return null;

        List<DisplayOverlay> outOverlays = new LinkedList<>();
        for (CommonOverlay in : inOverlays) {

            // skip invalid overlay
            if (!in.isValid()) {
                LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config skip invalid overlay: " + new Gson().toJson(in));
                continue;
            }

            // gen overlay
            DisplayOverlay overlay = genOverlay(in.src, in.dst);
            if (overlay == null)
                continue;

            // add to out list
            outOverlays.add(overlay);
        }

        return outOverlays;
    }

    private DisplayOverlay genCustomOverlay(TSLayout layout, List<OverlaySrc> srcList) {
        if (layout == null || srcList == null)
            return null;

        List<Integer> streamIds = new LinkedList<>();
        for (OverlaySrc src : srcList) {
            if (!src.isValid() || src.type != OverlaySrc.Type.Stream) {
                streamIds.add(-1);
            } else {
                int decId = getChannelVideoDecId(src.channel);
                streamIds.add(decId);
            }
        }

        DisplayOverlay overlay = DisplayOverlay.buildCustom(layout.name());
        overlay.setCustomStreams(streamIds);
        return overlay;
    }

    private DisplayOverlay genOverlay(OverlaySrc src, OverlayDst dst) {
        switch(src.type) {
            case Stream:
                return genStreamOverlay(src, dst);
            case Image:
                return genImageOverlay(src, dst);
            case Bitmap:
                return genBitmapOverlay(src, dst);
            default:
                throw new IllegalStateException("Unexpected value: " + src.type);
        }
    }

    private DisplayOverlay genImageOverlay(OverlaySrc src, OverlayDst dst) {
        DisplayOverlay overlay = DisplayOverlay.buildImage(src.imagePath, dst.region);
        overlay.setSrcRegion(src.region);
        overlay.setZIndex(dst.zIndex);
        overlay.setTransparency(dst.transparency);
        return overlay;
    }

    private DisplayOverlay genStreamOverlay(OverlaySrc src, OverlayDst dst) {
        // get video decode id by channel
        ChannelId channel = src.channel;
        int decId = getChannelVideoDecId(channel);
        if (decId < 0)
            return null;

        // get the input by channel id
        Input input = mInputs.get(channel);

        // build overlay for this stream
        DisplayOverlay overlay = DisplayOverlay.buildStream(decId, dst.region);
        overlay.setSrcRegion(input.srcRegion);
        overlay.setZIndex(dst.zIndex);
        overlay.setTransparency(dst.transparency);
        return overlay;
    }

    private DisplayOverlay genBitmapOverlay(OverlaySrc src, OverlayDst dst) {
        LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config skip Bitmap overlay");
        return null;
    }

    private int getChannelVideoDecId(ChannelId channel) {
        // check channel id
        if (!channel.isRx || channel.onlyAudio) {
            LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config, the channel has not input video stream: " + channel.name());
            return -1;
        }

        // get the input by channel id
        Input input = mInputs.get(channel);
        if (input == null) {
            LogUtil.w(CoreUtils.TAG, TAG, "transLayout2Config, the input channel is empty: " + channel.name());
            return -1;
        }
        if (input.vStreams.size() == 0) {
            LogUtil.d(CoreUtils.TAG, TAG, "transLayout2Config, the input channel has no video stream: " + channel.name());
            return -1;
        }

        // find the first matched stream
        int extId = -1;
        int mainId = -1;
        for (AVStream stream : input.vStreams) {
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
            return -1;
        }

        return decId;
    }

    private DisplayConfig transLayout2Config(Layout layout, TransitionDesc transition) {
        // gen common overlays
        List<DisplayOverlay> commonOverlays = genCommonOverlays(layout.getCommonOverlays());

        // gen custom overlay
        DisplayOverlay customOverlay = genCustomOverlay(layout.getCustomLayout(), layout.getCustomSrcList());

        // build DisplayConfig with overlays
        DisplayConfig config = DisplayConfig.buildEmptyOverlays();
        if (layout.isCommonAtHead()) {
            if (commonOverlays != null)
                config.addOverlays(commonOverlays);
            if (customOverlay != null)
                config.addOverlay(customOverlay);
        } else {
            if (customOverlay != null)
                config.addOverlay(customOverlay);
            if (commonOverlays != null)
                config.addOverlays(commonOverlays);
        }

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
        for (Map.Entry<ChannelId, MixMode> entry : mixModes.entrySet()) {
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
        if (layout != null && layout.isValid()) {
            List<OverlaySrc> srcList = new LinkedList<>();

            // push src of common overlay
            List<CommonOverlay> commonOverlays = layout.getCommonOverlays();
            if (commonOverlays != null) {
                for (CommonOverlay overlay : commonOverlays)
                    srcList.add(overlay.src);
            }

            // push src of custom overlay
            List<OverlaySrc> customSrcList = layout.getCustomSrcList();
            if (customSrcList != null)
                srcList.addAll(customSrcList);

            for (OverlaySrc src : srcList) {
                // skip invalid overlay
                if (!src.isValid())
                    continue;

                // just try to add track for stream overlay
                if (src.type == OverlaySrc.Type.Stream) {
                    // get channel id
                    ChannelId channel = src.channel;
                    if (!channel.isRx || channel.onlyAudio)
                        continue;

                    // get the input by channel id
                    Input input = mInputs.get(channel);
                    if (input == null)
                        continue;

                    // skip non-AFV or muted channel
                    MixMode mixMode = mixModes.get(channel);
                    if (mixMode == null || mixMode != MixMode.AFV || input.muted)
                        continue;

                    inputs.add(input);
                }
            }
        }

        // loop to fill tracks
        for (Input input : inputs) {
            // skip empty audio streams
            if (input.connState != State.Done || input.aStreams.size() == 0)
                continue;

            // add all matched stream
            for (AVStream stream : input.aStreams) {
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
        if (input.config.type == InputType.Caller)
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
                LogUtil.w(CoreUtils.TAG, TAG, "onTxStreamOpen, epStartTxStream failed: " + ret);
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
                String hint = String.format("????????????????????????: ??????[%s]???????????????[%s]???????????????[%s]??????", caller.config.name, diff.get(0), diff.get(1));
                genWarning("onTxStreamOpen", result.code,
                        message + ", but ignore this warning and force to use " + diff.get(1),
                        hint + ",????????????????????????????????????" + diff.get(1), false);
            }

            // check PGM display
            Display display = mDisplays.get(SurfaceId.PGM);
            if (display == null || display.state != State.Done) {
                LogUtil.w(CoreUtils.TAG, TAG, "onTxStreamOpen, PGM display is not ready");
                return;
            }

            // start this tx stream
            int ret = mEndpoint.epStartTxStream(caller.epId, streamId, caller.vFormat, EPObjectType.Display, display.epId);
            if (ret != 0) {
                LogUtil.w(CoreUtils.TAG, TAG, "onTxStreamOpen, epStartTxStream failed: " + ret);
                return;
            }
            stream.onEncoding();
        }
    }

    private void onRxStreamClose(Input input, int streamId, StreamDesc desc) {
        // stop decoding rx stream
        if (input.config.type == InputType.Caller)
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

            for (Input input : mInputs.values()) {
                if (input.config.type == InputType.Caller) {
                    Caller caller = (Caller) input;
                    for (AVStream stream : caller.vTxStreams) {
                        if (!stream.isEncoding()) {
                            int ret = mEndpoint.epStartTxStream(caller.epId, stream.id, caller.vFormat,
                                    EPObjectType.Display, displayId);
                            if (ret != 0)
                                LogUtil.w(CoreUtils.TAG, TAG, "onSurfaceReady, epStartTxStream failed: " + ret);
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
            Layout layout = videoReady ?
                    new Layout().addOverlays(TSLayout.A, Arrays.asList(OverlaySrc.buildStream(channel))) :
                    Layout.buildEmpty();
            switchLayout(surfaceId, layout, TransitionDesc.buildEmpty());
        }
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
        for (AudioCodec codec : aCodecs) {
            AudioCapability capability = new AudioCapability(codec.format);
            audio.add(capability);
        }

        AudioCapabilities.Capabilities caps = new AudioCapabilities.Capabilities(audio);
        return new AudioCapabilities(caps, caps);
    }

    private static VideoCapabilities getVideoCapability(List<VideoFormat> formats) {
        List<VideoCapability> video = new LinkedList<>();

        for (VideoFormat format : formats) {
            VideoCapability capability = new VideoCapability(format.codec, format.profile,
                    format.resolution, format.framerate);
            capability.setKeyFrameInterval(format.getKeyIntervalInFrames());

            boolean added = false;
            for (VideoCapability cap : video) {
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
            if (layout == null || layout.getCommonOverlays() == null)
                return false;
            for (CommonOverlay overlay : layout.getCommonOverlays()) {
                if (overlay.src.type != OverlaySrc.Type.Stream)
                    continue;
                if (CompareHelper.isEqual(channel, overlay.src.channel))
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
