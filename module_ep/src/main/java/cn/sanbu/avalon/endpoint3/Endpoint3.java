package cn.sanbu.avalon.endpoint3;

import android.content.Context;
import android.view.Surface;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sanbu.base.BaseError;
import com.sanbu.android.annotation.NonNull;
import com.sanbu.android.annotation.Nullable;
import com.sanbu.media.AudioFormat;
import com.sanbu.media.Bandwidth;
import com.sanbu.network.CallingProtocol;
import com.sanbu.media.DataType;
import com.sanbu.media.Resolution;
import com.sanbu.media.VideoFormat;
import com.sanbu.tools.LogUtil;

import java.util.Arrays;
import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.AudioInputDevice;
import cn.sanbu.avalon.endpoint3.structures.AudioOutputDevice;
import cn.sanbu.avalon.endpoint3.structures.CallingStatistics;
import com.sanbu.media.EPObjectType;
import com.sanbu.network.TransProtocol;

import cn.sanbu.avalon.endpoint3.structures.OutputStatus;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioCapabilities;
import cn.sanbu.avalon.endpoint3.structures.jni.DisplayConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.EPEvent;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.MediaStatistics;
import cn.sanbu.avalon.endpoint3.structures.jni.MixerTracks;
import cn.sanbu.avalon.endpoint3.structures.jni.RecOEConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.RecSplitStrategy;
import cn.sanbu.avalon.endpoint3.structures.jni.Reconnecting;
import cn.sanbu.avalon.endpoint3.structures.jni.RegConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapabilities;
import cn.sanbu.avalon.media.MediaEventId;
import cn.sanbu.avalon.media.VideoEngine;

/*
* 通用的端侧SDK(EP3)
* 功能描述：
*   . 支持视频合成/混音；信号源配置；呼叫/注册；码流输出（文件/网络）
*   . 支持多点同时呼叫，没有MCU概念，内部也没有多点关联关系（因为一带多的实现由应用场景和硬件能力所决定，所以无须考虑复用和可移植）
*   . 呼叫和码流输出支持指定流转发
*   . 支持单个source包含多路音视频流
*   . 支持从一个caller接收多路音视频流，但不支持发送N路(接口支持,内部未实现)
* 设计原则：
*   . 接口可能阻塞，调用者须另起线程执行
*   . 没有业务角色(老师/学生/...)，没有主流辅流，只有流的概念
*   . 不考虑性能限制，不考虑具体硬件环境，仅依托于android
* */
public class Endpoint3 {

    public static void initEnv(Context context, String epLib) {
        // load jni libraries
        System.loadLibrary(epLib);

        // init endpoint jni environment
        // jniEnvInit();
        getInstance().jniEnvInit();
    }

    // Terminal endpoint callbacks
    public interface EPCallback {
        // Internal error occurred
        void onError(int errCode, String reason);

        // Internal event for ep object
        // @param obj_type [IN] type of object
        // @param obj_id [IN] id of object
        // @param event [IN] value of event
        // @param params [IN] params of event:
        //      if event = FileWrittenCompleted, params is file-path(String)
        //      if event = OutputStatusChanged, params is error-code(OutputStatus)
        void onObjEvent(EPObjectType objType, int objId, EPEvent event, Object params);

        // Volume report for audio mixers and sources
        // @param report [IN] serialized string which can be parsed by VolumeReportParser
        void onVolumeReport(String report);
    }

    // Terminal stream callbacks
    public interface StreamCallback {
        // Stream is open/closed which is owned by source/caller/output
        // @param parent_type [IN] type of stream parent
        // @param parent_id [IN] id of stream parent
        // @param stream_id [IN] id of stream
        // @param desc [IN] description of stream
        // @param format [IN] format of stream, @see VideoFormat and AudioFormat
        void onStreamOpen(EPObjectType parentType, int parentId, int streamId, StreamDesc desc, Object format);

        void onStreamClose(EPObjectType parentType, int parentId, int streamId, StreamDesc desc);

        // Rx video stream is ready/unavailable to use which is owned by source/caller
        // @param parent_type [IN] type of stream parent
        // @param parent_id [IN] id of stream parent
        // @param dec_id [IN] decoding id of stream
        void onRxVideoStreamAvailabilityChanged(EPObjectType parentType, int parentId,
                                                int decId, boolean ready);
    }

    // Terminal calling callbacks
    public interface CallingCallback {
        // A new sip or h.323 calling is incoming
        // @param call_id [IN] the incoming call id
        // @param number [IN] the incoming call's number
        // @param call_url [IN] the incoming call's url
        // @param protocol [IN] call protocol
        void onIncomingCall(int callId, String number, String callUrl, CallingProtocol protocol);

        // Remote peer is ring when receiving a call
        // @param call_id [IN] call id
        void onRemoteRinging(int callId);

        // Established the calling
        // @param call_id [IN] call id
        // @param vendor [IN] the peer's vendor information
        // @param name [IN] the peer's vendor display name
        void onEstablished(int callId, String vendor, String name);

        // Calling is finished
        // @param call_id [IN] call id
        // @param reason [IN] reason of finished
        void onFinished(int callId, int errCode, String reason);

        // Caller error occurred
        // @param call_id [IN] call id
        // @param error [IN] error information
        void onCallerError(int callId, int errCode, String reason);
    }

    // Terminal signal registering callbacks
    public interface RegisteringCallback {
        // GateKeeper register, unregister, and status notify
        // @param result [IN] 0 is ok, or error occurred
        void onRegistering(int result, CallingProtocol protocol);

        void onUnRegistering(int result, CallingProtocol protocol);

        void onNotifyRegisterStatus(int result, CallingProtocol protocol);
    }

    // Terminal endpoint single instance
    private static final Endpoint3 g_instance = new Endpoint3();

    public static final Endpoint3 getInstance() {
        return g_instance;
    }

    // surface id of display without show
    private static volatile int m_hidden_display_id = VideoEngine.MAXIMUM_DISPLAY_SURFACE_COUNT;

    // callback for ep and caller, like incoming call, established and so on
    private EPCallback m_ep_cb = null;
    private StreamCallback m_stream_cb = null;
    private CallingCallback m_call_cb = null;
    private RegisteringCallback m_reg_cb = null;

    private Endpoint3() {

    }

    /////////////////////////////// init and configure interfaces

    // @brief Inits endpoint.
    // @param config [IN] endpoint fixed config
    // @param epCallback [IN] callback for ep
    // @param streamCallback [IN] callback for stream
    // @return 0 is suc, or failed
    public int epInit(EPFixedConfig config, EPCallback epCallback,
                      StreamCallback streamCallback) {
        m_ep_cb = epCallback;
        m_stream_cb = streamCallback;

        String json = new Gson().toJson(config);
        LogUtil.i(EPConst.TAG, "epInit: " + json);
        int ret = jniEpInit(json);

        LogUtil.w(EPConst.TAG, "TODO: fix RTSP_TRANS_PROTO as RTP/AVP/TCP");
        jniEpConfigure(EPConst.EP_PROPERTY_RTSP_TRANS_PROTO, TransProtocol.RTP_TCP.name);

        return ret;
    }

    // @brief Uninits endpoint, release resources.
    // @Note, Must release all caller, lives, records, source before call @epUninit.
    public int epUninit() {
        LogUtil.i(EPConst.TAG, "epUninit");
        return jniEpUninit();
    }

    // @brief Set callbacks for calling.
    // @param callback [IN] callback for calling
    public void setCallingCallback(CallingCallback callback) {
        LogUtil.i(EPConst.TAG, "setCallingCallback");
        m_call_cb = callback;
    }

    // @brief Set callbacks for signal registering.
    // @param callback [IN] callback for registering
    public void setRegisteringCallback(RegisteringCallback callback) {
        LogUtil.i(EPConst.TAG, "setRegisteringCallback");
        m_reg_cb = callback;
    }

    // @brief Configures endpoint properties, like bandwidth, capabilities and so on.
    // @return 0 is suc, or failed.

    public int epSwitchAGC(boolean enabled) {
        int ret = jniEpConfigure(EPConst.EP_PROPERTY_AGC_SWITCH, String.valueOf(enabled));
        EPConst.logAction("epSwitchAGC", ret, enabled);
        return ret;
    }

    public int epSetVolumeReportInterval(int intervalInFrames) {
        int ret = jniEpConfigure(EPConst.EP_PROPERTY_VOLUME_REPORT_INTERVAL, String.valueOf(intervalInFrames));
        EPConst.logAction("epSetVolumeReportInterval", ret, intervalInFrames);
        return ret;
    }

    // @brief Get statistics for the media engine
    public MediaStatistics getMediaStatistics() {
        String statistics = jniEpGetMediaStatistics();
        return new Gson().fromJson(statistics, MediaStatistics.class);
    }

    public String /**@see MediaStatistics*/ getMediaStatisticsStr() {
        return jniEpGetMediaStatistics();
    }

    /////////////////////////////// display interfaces

    // @brief Adds video visible display
    // @param handle [IN] android surface handle
    // @return display(surface) id
    public int epAddVisibleDisplay(@NonNull Surface handle) {
        int ret = epAddVisibleDisplayImpl(handle);
        EPConst.logAction("epAddDisplay(visible)", ret);
        return ret;
    }

    private int epAddVisibleDisplayImpl(@NonNull Surface handle) {
        int display_id = VideoEngine.getInstance().registerDisplaySurface(handle);
        if (display_id < 0) {
            LogUtil.e(EPConst.TAG, "VideoEngine registerDisplaySurface failed: " + display_id);
            return display_id;
        }

        int ret = jniEpAddDisplay(display_id);
        if (ret != 0)
            return ret > 0 ? 0 - ret : ret;
        return display_id;
    }

    // @brief Adds video invisible display with properties: resolution and rate
    // @param handle [IN] android surface handle
    // @param width  [IN] display resolution
    // @param height [IN] display resolution
    // @param rate   [IN] display frame rate
    // @return display(surface) id
    public int epAddInvisibleDisplay(int width, int height, int rate) {
        int ret = epAddInvisibleDisplayImpl(width, height, rate);
        EPConst.logAction("epAddDisplay(invisible)", ret, width + "x" + height + "@" + rate);
        return ret;
    }

    private int epAddInvisibleDisplayImpl(int width, int height, int rate) {
        int display_id = m_hidden_display_id++;
        int ret = jniEpAddDisplay2(display_id, width, height, rate);
        if (ret != 0)
            return ret > 0 ? 0 - ret : ret;
        return display_id;
    }

    // @brief Changes display when android surface on change
    // @param display_id [IN] display(surface) id
    // @param format [IN] changed format
    // @param width [IN] changed width
    // @param height [IN] changed height
    // @return 0 is suc, or failed.
    public int epChangeVisibleDisplay(int display_id, int format, int width, int height) {
        int ret = VideoEngine.getInstance().onDisplaySurfaceChanged(display_id, format, width, height) ? 0 : -1;
        EPConst.logAction("epChangeVisibleDisplay", ret, String.format("#%d, %dx%d (format=%d)", display_id, width, height, format));
        return ret;
    }

    // @brief Removes video display
    // @param display_id [IN] display id which needs to remove
    // @return 0 is suc, or failed.
    public int epRemoveDisplay(int display_id) {
        int ret = epRemoveDisplayImpl(display_id);
        EPConst.logAction("epRemoveDisplay", ret, display_id);
        return ret;
    }

    private int epRemoveDisplayImpl(int display_id) {
        if (display_id < 0)
            return 0;

        if (display_id < VideoEngine.MAXIMUM_DISPLAY_SURFACE_COUNT) {
            if (!VideoEngine.getInstance().unregisterDisplaySurface(display_id)) {
                LogUtil.e(EPConst.TAG, "VideoEngine unregisterDisplaySurface failed");
                return BaseError.INTERNAL_ERROR;
            }
        }

        return jniEpRemoveDisplay(display_id);
    }

    // @brief Configures video display
    // @param display_id [IN] display(surface) id
    // @param config [IN] display config
    // @return 0 is suc, or failed.
    public int epConfigureDisplay(int display_id, DisplayConfig config) {
        String json = new Gson().toJson(config);
        int ret = jniEpConfigureDisplay(display_id, json);
        EPConst.logAction("epConfigureDisplay", ret, display_id, json);
        return ret;
    }

    /////////////////////////////// audio device interfaces

    // @brief Get default audio input device
    // @return audio input device
    public AudioInputDevice epGetDefaultAudioInputDevice() {
        // TODO: jni/audio engine supports this api
        return EPConst.LOCAL_DEFAULT_AUDIO_CAPTURE;
    }

    // @brief Enumerate all available audio input devices
    // @return all audio input devices
    public List<AudioInputDevice> epEnumAudioInputDevices() {
        // TODO: jni/audio engine supports this api
        return Arrays.asList(epGetDefaultAudioInputDevice());
    }

    // @brief Get default audio output device
    // @return audio output device
    public AudioOutputDevice epGetDefaultAudioOutputDevice() {
        // TODO: jni/audio engine supports this api
        return EPConst.LOCAL_DEFAULT_AUDIO_SPEAKER;
    }

    // @brief Enumerate all available audio output devices
    // @return all audio output devices
    public List<AudioOutputDevice> epEnumAudioOutputDevices() {
        // TODO: jni/audio engine supports this api
        return Arrays.asList(epGetDefaultAudioOutputDevice());
    }

    // @brief Bind a audio stream to a output device
    // @param device_id [IN] id of audio output device
    // @param in_type [IN] type of input parent
    // @param in_id [IN] id of input parent
    // @param in_stream_id [IN] input stream id which can be -1 if input type is mixer
    // @return @return 0 is suc, or failed.
    public int epBindAudioOutputDevice(int device_id, EPObjectType in_type, int in_id, int in_stream_id) {
        if (in_type == EPObjectType.Stream || in_type == EPObjectType.Display ||
                in_type == EPObjectType.Output) {
            LogUtil.e(EPConst.TAG, "epBindAudioOutputDevice, in_type is invalid: " + in_type.name());
            return BaseError.INVALID_PARAM;
        }
        if (in_type != EPObjectType.Mixer && in_stream_id < 0) {
            LogUtil.e(EPConst.TAG, String.format("epBindAudioOutputDevice, invalid params: in_type:%s, in_stream_id:%d",
                    in_type.name(), in_stream_id));
            return BaseError.INVALID_PARAM;
        }

        int ret = jniEpBindAudioOutputDevice(device_id, in_type.id, in_id, in_stream_id);
        EPConst.logAction("epBindAudioOutputDevice", ret, String.format("device_id:%d, in_type:%s, in_id:%d in_stream_id:%d",
                device_id, in_type.name(), in_id, in_stream_id));
        return ret;
    }

    // @brief unBind the audio stream from the output device
    // @param device_id [IN] id of audio output device
    // @param in_type [IN] type of input parent
    // @param in_id [IN] id of input parent
    // @param in_stream_id [IN] input stream id which can be -1 if input type is mixer
    // @return @return 0 is suc, or failed.
    public int epUnBindAudioOutputDevice(int device_id, EPObjectType in_type, int in_id, int in_stream_id) {
        if (in_type == EPObjectType.Stream || in_type == EPObjectType.Display ||
                in_type == EPObjectType.Output) {
            LogUtil.e(EPConst.TAG, "epUnBindAudioOutputDevice, in_type is invalid: " + in_type.name());
            return BaseError.INVALID_PARAM;
        }
        if (in_type != EPObjectType.Mixer && in_stream_id < 0) {
            LogUtil.e(EPConst.TAG, String.format("epBindAudioOutputDevice, invalid params: in_type:%s, in_stream_id:%d",
                    in_type.name(), in_stream_id));
            return BaseError.INVALID_PARAM;
        }

        int ret = jniEpUnBindAudioOutputDevice(device_id, in_type.id, in_id, in_stream_id);
        EPConst.logAction("epUnBindAudioOutputDevice", ret, String.format("device_id:%d, in_type:%s, in_id:%d in_stream_id:%d",
                device_id, in_type.name(), in_id, in_stream_id));
        return ret;
    }

    /////////////////////////////// audio mixer interfaces

    // @brief Adds audio mixer
    // @return mixer id
    public int epAddMixer() {
        int ret = jniEpAddMixer();
        EPConst.logAction("epAddMixer", ret);
        return ret;
    }

    // @brief Removes audio mixer
    // @param mixer_id [IN] id of mixer would be removed
    // @return @return 0 is suc, or failed.
    public int epRemoveMixer(int mixer_id) {
        int ret = jniEpRemoveMixer(mixer_id);
        EPConst.logAction("epRemoveMixer", ret, mixer_id);
        return ret;
    }

    // @brief Sets audio mixer volume
    // @param mixer_id [IN] mixer id
    // @param volume [IN] mixer volume
    // @param tracks [IN] mixer tracks
    // @return @return 0 is suc, or failed.
    public int epSetMixer(int mixer_id, float volume, MixerTracks tracks) {
        JsonObject object = new Gson().toJsonTree(tracks).getAsJsonObject();
        object.addProperty("volume", volume);
        String config = object.toString();

        if (!isValidVolume(volume)) {
            LogUtil.e(EPConst.TAG, "invalid volume: " + config);
            return BaseError.INVALID_PARAM;
        }
        if (!tracks.isValid()) {
            LogUtil.e(EPConst.TAG, "MixerTracks is invalid: " + config);
            return BaseError.INVALID_PARAM;
        }

        int ret = jniEpConfigureMixer(mixer_id, config);
        EPConst.logAction("epSetMixer", ret, mixer_id, config);
        return ret;
    }

    // @brief Sends DTMF digital through a audio mixer
    // @param mixer_id [IN] mixer id
    // @param key [IN] phone key
    // @return 0 is suc or failed
    public int epSendDTMFOnMixer(int mixer_id, char key) {
        int ret = jniEpSendDTMFOnMixer(mixer_id, key);
        EPConst.logAction("epSendDTMFOnMixer", ret, mixer_id, key);
        return ret;
    }

    /////////////////////////////// source(rtsp/rtmp/file/capture) interfaces

    // @brief Adds origin camera.
    // @param url [IN] source url, e.g. "device://video/0" (camera id)
    // @param resolution [IN] source resolution, RES_UNKNOWN means auto
    // @return source_id
    public int epAddOriginCamera(String url, @NonNull Resolution resolution) {
        String json;

        JsonObject properties = new JsonObject();
        properties.addProperty("resolution",
                resolution == Resolution.RES_UNKNOWN ? "auto" : resolution.toSize().toString());
        json = properties.toString();

        int ret = jniEpAddSource(url, json);
        EPConst.logAction("epAddSource(OriginCamera)", ret, url, json);
        return ret;
    }

    // @brief Adds audio capture source(MIC).
    // @param url [IN] source url, e.g. "device://audio/0"
    // @return source_id
    public int epAddAudioCapture(String url) {
        int ret = jniEpAddSource(url, "");
        EPConst.logAction("epAddSource(AudioCapture)", ret, url);
        return ret;
    }

    // @brief Adds rtsp source
    // @param url [IN] source url, e.g. rtsp/rtmp/...
    // @param trans_protocol [IN] protocol of media transport
    // @param reconnecting [IN] configure of reconnecting
    // @return source_id
    public int epAddRTSPSource(String url, @Nullable TransProtocol trans_protocol, @NonNull Reconnecting reconnecting) {
        JsonObject properties = new JsonObject();
        if (trans_protocol != null && trans_protocol != TransProtocol.UNSPECIFIED)
            properties.addProperty("protocol", trans_protocol.value);
        properties.add("reconnect", new Gson().toJsonTree(reconnecting));
        String json = properties.toString();

        LogUtil.w(EPConst.TAG, "TODO: rtsp trans protocol is not used: " + trans_protocol);

        int ret = jniEpAddSource(url, json);
        EPConst.logAction("epAddSource(RTSP)", ret, url, json);
        return ret;
    }

    // @brief Adds rmsp source
    // @param url [IN] source url, e.g. rmsp/rmsi...
    // @param video [IN] format of video stream
    // @param audio [IN] format of audio stream
    // @return source_id
    public int epAddRMSPSource(String url, @Nullable VideoFormat video, @Nullable AudioFormat audio) {
        if (video == null && audio == null) {
            LogUtil.e(EPConst.TAG, "epAddRMSPSource, invalid media format");
            return BaseError.INVALID_PARAM;
        }

        JsonObject properties = new JsonObject();
        if (video != null)
            properties.add("video", new Gson().toJsonTree(video));
        if (audio != null)
            properties.add("audio", new Gson().toJsonTree(audio));
        String json = properties.toString();

        int ret = jniEpAddSource(url, json);
        EPConst.logAction("epAddSource(RMSP)", ret, url, json);
        return ret;
    }

    // @brief Adds common net source
    // @param url [IN] source url, e.g. rtmp/...
    // @param reconnecting [IN] configure of reconnecting
    // @return source_id
    public int epAddNetSource(String url, @NonNull Reconnecting reconnecting) {
        JsonObject properties = new JsonObject();
        properties.add("reconnect", new Gson().toJsonTree(reconnecting));
        String json = properties.toString();

        int ret = jniEpAddSource(url, json);
        EPConst.logAction("epAddSource(Net)", ret, url, json);
        return ret;
    }

    // @brief Start to decode source stream
    // @param source_id [IN] source id
    // @param stream_id [IN] stream id of source
    // @return raw stream id, or < 0 is failed
    public int epStartSrcStreamDecoding(int source_id, int stream_id) {
        int ret = jniEpStartSrcStreamDecoding(source_id, stream_id);
        EPConst.logAction("epStartSrcStreamDecoding", ret, source_id, stream_id);
        return ret;
    }

    // @brief Stop to decode source stream
    // @param source_id [IN] source id
    // @param stream_id [IN] stream id of source
    // @return 0 is suc or failed
    public int epStopSrcStreamDecoding(int source_id, int stream_id) {
        int ret = jniEpStopSrcStreamDecoding(source_id, stream_id);
        EPConst.logAction("epStopSrcStreamDecoding", ret, source_id, stream_id);
        return ret;
    }

    // @brief Removes a source
    // @param source_id [IN] source id
    // @return 0 is suc or failed
    public int epRemoveSource(int source_id) {
        int ret = jniEpRemoveSource(source_id);
        EPConst.logAction("epRemoveSource", ret, source_id);
        return ret;
    }

    /////////////////////////////// calling interfaces

    // @brief Create a call handler in H.323/SIP/others protocol which would be used for calling remote
    // @param url [IN] caller url: h323:10.1.11.150, h323:1002, sip:... and others
    // @return call id, and all other ops need this call id.
    public int epCreateCaller(String url) {
        int ret = jniEpCreateCall(url);
        EPConst.logAction("epCreateCaller", ret, url);
        return ret;
    }

    // @brief Configures properties for the call, like bandwidth, capabilities and so on. And must be called before epCall or epAccept
    // @return 0 is suc, or failed.
    public int epSetCallBandwidth(int call_id, Bandwidth bandwidth) {
        int ret = jniEpConfigureCall(call_id, EPConst.EP_PROPERTY_BAND_WIDTH, String.valueOf(bandwidth.bps));
        EPConst.logAction("epSetCallBandwidth", ret, call_id, bandwidth.bps);
        return ret;
    }

    public int epSetCallAudioCapabilities(int call_id, AudioCapabilities capabilities) {
        String caps = new Gson().toJson(capabilities);
        int ret = jniEpConfigureCall(call_id, EPConst.EP_PROPERTY_AUDIO_CAPABILITIES, caps);
        EPConst.logAction("epSetCallAudioCapabilities", ret, call_id, caps);
        return ret;
    }

    public int epSetCallVideoCapabilities(int call_id, VideoCapabilities capabilities) {
        String caps = new Gson().toJson(capabilities);
        int ret = jniEpConfigureCall(call_id, EPConst.EP_PROPERTY_VIDEO_CAPABILITIES, caps);
        EPConst.logAction("epSetCallVideoCapabilities", ret, call_id, caps);
        return ret;
    }

    // @brief Makes a call, call the remote peer
    // @param call_id [IN] caller id
    // @return 0 is suc or failed
    public int epCall(int call_id) {
        int ret = jniEpCall(call_id);
        EPConst.logAction("epCall", ret, call_id);
        return ret;
    }

    // @brief Hangs up a call
    // @param call_id [IN] caller id
    // @return 0 is suc or failed
    public int epHangup(int call_id) {
        int ret = jniEpHangup(call_id);
        EPConst.logAction("epHangup", ret, call_id);
        return ret;
    }

    // @brief Releases a call
    // @param call_id [IN] caller id
    // @return 0 is suc or failed
    public int epReleaseCaller(int call_id) {
        int ret = jniEpRelease(call_id);
        EPConst.logAction("epReleaseCaller", ret, call_id);
        return ret;
    }

    // @brief Recalls
    // @param call_id [IN] caller id
    // @return 0 is suc or failed
    public int epRecall(int call_id) {
        int ret = jniEpRecall(call_id);
        EPConst.logAction("epRecall", ret, call_id);
        return ret;
    }

    // @brief Accepts a incoming call
    // @param call_id [IN] caller id
    // @return 0 is suc or failed
    public int epAccept(int call_id) {
        int ret = jniEpAccept(call_id);
        EPConst.logAction("epAccept", ret, call_id);
        return ret;
    }

    // @brief Rejects a incoming call
    // @param call_id [IN] caller id
    // @return 0 is suc or failed
    public int epReject(int call_id) {
        int ret = jniEpReject(call_id);
        EPConst.logAction("epReject", ret, call_id);
        return ret;
    }

    // @brief Opens ext tx stream
    // @param call_id [IN] caller id
    // @param tx_desc [IN] description of tx stream
    // @return 0 is suc or failed
    public int epOpenExtTxStream(int call_id, StreamDesc tx_desc) {
        String desc = new Gson().toJson(tx_desc);
        int ret = jniEpOpenExtTxStream(call_id, desc);
        EPConst.logAction("epOpenExtTxStream", ret, call_id, desc);
        return ret;
    }

    // @brief Closes ext tx stream
    // @param call_id [IN] caller id
    // @param stream_id [IN] stream id
    // @return 0 is suc or failed
    public int epCloseExtTxStream(int call_id, int stream_id) {
        int ret = jniEpCloseExtTxStream(call_id);
        EPConst.logAction("epCloseExtTxStream", ret, call_id, stream_id);
        return ret;
    }

    // @brief Closes ext rx stream
    // @param call_id [IN] caller id
    // @param stream_id [IN] stream id
    // @return 0 is suc or failed
    public int epCloseExtRxStream(int call_id, int stream_id) {
        EPConst.logAction("epCloseExtRxStream", BaseError.ACTION_UNSUPPORTED, call_id, stream_id);
        return BaseError.ACTION_UNSUPPORTED;
    }

    // @brief Sets and start calling tx stream
    // @param call_id [IN] caller id
    // @param tx_stream_id [IN] calling tx stream id which from @onStreamOpen callback
    // @param in_type [IN] type of input parent
    // @param in_id [IN] id of input parent
    // @param in_stream_id [IN] input stream id which can be -1 if input type is display or mixer
    // @param by_forward [IN] forward media stream or output from encoder
    // @return 0 is suc, or failed.

    public int epStartTxStream(int call_id, int tx_stream_id, Object tx_format,
                               EPObjectType in_type, int in_id) {
        return epStartTxStream(call_id, tx_stream_id, tx_format, in_type, in_id, -1);
    }

    public int epStartTxStream(int call_id, int tx_stream_id, Object tx_format,
                               EPObjectType in_type, int in_id, int in_stream_id) {
        return epStartTxStream(call_id, tx_stream_id, tx_format, in_type, in_id, in_stream_id, false);
    }

    public int epStartTxStreamByForward(int call_id, int tx_stream_id,
                                        EPObjectType in_type, int in_id, int in_stream_id) {
        return epStartTxStream(call_id, tx_stream_id, null, in_type, in_id, in_stream_id, true);
    }

    private int epStartTxStream(int call_id, int tx_stream_id, Object tx_format,
                                EPObjectType in_type, int in_id, int in_stream_id, boolean by_forward) {
        if (in_type != EPObjectType.Display && in_type != EPObjectType.Mixer && in_stream_id < 0) {
            LogUtil.e(EPConst.TAG, "epStartTxStream, invalid in_stream_id: " + in_stream_id);
            return BaseError.INVALID_PARAM;
        }

        String format = tx_format == null ? "{}" : new Gson().toJson(tx_format);
        int ret = jniEpStartTxStream(call_id, tx_stream_id, format, in_type.id, in_id, in_stream_id, by_forward);
        EPConst.logAction("epStartTxStream", ret, String.format("call_id: %d, tx_stream_id: %d, format: %s, in_type: %s, in_id: %d, in_stream_id: %d, by_forward: %s",
                call_id, tx_stream_id, format, in_type.name(), in_id, in_stream_id, by_forward));
        return ret;
    }

    // @brief Stop calling tx stream
    // @param call_id [IN] caller id
    // @param tx_stream_id [IN] calling tx stream id
    // @return 0 is suc, or failed.
    public int epStopTxStream(int call_id, int tx_stream_id) {
        int ret = jniEpStopTxStream(call_id, tx_stream_id);
        EPConst.logAction("epStopTxStream", ret, call_id, tx_stream_id);
        return ret;
    }

    // @brief Start to decode calling rx stream
    // @param call_id [IN] caller id
    // @param rx_stream_id [IN] calling rx stream id
    // @return raw stream id, or < 0 is failed
    public int epStartRxStreamDecoding(int call_id, int rx_stream_id) {
        int ret = jniEpStartRxStreamDecoding(call_id, rx_stream_id);
        EPConst.logAction("epStartRxStreamDecoding", ret, call_id, rx_stream_id);
        return ret;
    }

    // @brief Stop to decode calling rx stream
    // @param call_id [IN] caller id
    // @param rx_stream_id [IN] calling rx stream id
    // @return 0 is suc or failed
    public int epStopRxStreamDecoding(int call_id, int rx_stream_id) {
        int ret = jniEpStopRxStreamDecoding(call_id, rx_stream_id);
        EPConst.logAction("epStopRxStreamDecoding", ret, call_id, rx_stream_id);
        return ret;
    }

    // @brief send key frame request of caller through a tx video stream
    // @param call_id [IN] caller id
    // @param tx_video_id [IN] calling tx video stream id
    // @return 0 is suc or failed
    public int epSendKeyFrameRequest(int call_id, int tx_video_id) {
        int ret = jniEpSendKeyFrameRequest(call_id, tx_video_id);
        EPConst.logAction("epSendKeyFrameRequest", ret, call_id, tx_video_id);
        return ret;
    }

    // @brief Get calling statistics for the caller
    // @param call_id [IN] caller id
    // @return calling statistics
    // NOTE:
    // - bitrate unit: bps
    // - frame rate: fps, double
    public CallingStatistics getCallingStatistics(int call_id) {
        String statistics = jniEpGetCallingStatistics(call_id);
        // LogUtil.d(EPConst.TAG, "getCallingStatistics(#" + call_id + "): " + statistics);

        cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics statis = new Gson().fromJson(statistics,
                cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.class);
        return CallingStatistics.build(statis).update();
    }

    /////////////////////////////// register gk and sip-server interfaces

    // @brief Registers GK
    // @param config [IN] config of h323 registering
    // @return 0 is suc or failed
    public int epRegisterGK(RegConfig.H323 config) {
        String json = new Gson().toJson(config);
        int ret = jniEpRegisterGK(json);
        EPConst.logAction("epRegisterGK", ret, json);
        return ret;
    }

    // @brief Unregisters GK
    // @return 0 is suc or failed
    public int epUnregisterGK() {
        int ret = jniEpUnregisterGK();
        EPConst.logAction("epUnregisterGK", ret);
        return ret;
    }

    // @brief Registers SIP
    // @param config [IN] config of SIP registering
    // @return 0 is suc or failed
    public int epRegisterSIP(RegConfig.SIP config) {
        String json = new Gson().toJson(config);
        int ret = jniEpRegisterSIP(json);
        EPConst.logAction("epRegisterSIP", ret, json);
        return ret;
    }

    // @brief Unregisters SIP
    // @return 0 is suc or failed
    public int epUnregisterSIP() {
        int ret = jniEpUnregisterSIP();
        EPConst.logAction("epUnregisterSIP", ret);
        return ret;
    }

    /////////////////////////////// output(file or net stream) interfaces

    // @brief create a net output(rmsp/rtmp)
    // @param url [IN] url or output
    // @return output id, or < 0 failed.
    public int epCreateNetOutput(String url) {
        int ret = jniEpCreateOutput(url, "");
        EPConst.logAction("epCreateOutput(Net)", ret, url);
        return ret;
    }

    // @brief create a net output(mp4/flv)
    // @param target_path [IN] target path of output file. it is formative, e.g. /sdcard/test_%T.flv
    // @param split [IN] strategy of file splitting
    // @param oe [IN] config for OE(opening and ending)
    // @return output id, or < 0 failed.
    public int epCreateFileOutput(String target_path, RecSplitStrategy split, @Nullable RecOEConfig oe) {
        JsonObject json = new JsonObject();
        json.add("split", new Gson().toJsonTree(split));
        if (oe != null)
            json.add("oe", new Gson().toJsonTree(oe));
        String properties = json.toString();
        int ret = jniEpCreateOutput(target_path, properties);
        EPConst.logAction("epCreateOutput(File)", ret, target_path, properties);
        return ret;
    }

    // @brief sets or adds output stream
    // @param output_id [IN] output id
    // @param tx_desc [IN] description of output tx stream
    // @param tx_format [IN] {@link AudioFormat or VideoFormat}
    // @param in_type [IN] type of input parent
    // @param in_id [IN] id of input parent
    // @param in_stream_id [IN] input stream id which can be -1 if input type is display or mixer
    // @return 0 is suc or failed

    public int epSetOutputStream(int output_id, StreamDesc tx_desc, Object tx_format,
                                 EPObjectType in_type, int in_id) {
        return epSetOutputStream(output_id, tx_desc, tx_format, in_type, in_id, -1);
    }

    public int epSetOutputStream(int output_id, StreamDesc tx_desc, Object tx_format,
                                 EPObjectType in_type, int in_id, int in_stream_id) {
        if (tx_format == null || (!(tx_format instanceof AudioFormat) && !(tx_format instanceof VideoFormat))) {
            LogUtil.e(EPConst.TAG, "epSetOutputStream, invalid tx_format: " +
                    (tx_format == null ? "null" : tx_format.getClass().getSimpleName()));
            return BaseError.INVALID_PARAM;
        }
        if (in_type != EPObjectType.Display && in_type != EPObjectType.Mixer && in_stream_id < 0) {
            LogUtil.e(EPConst.TAG, "epSetOutputStream, invalid in_stream_id: " + in_stream_id);
            return BaseError.INVALID_PARAM;
        }

        String desc = new Gson().toJson(tx_desc);
        String format = new Gson().toJson(tx_format);
        int ret = jniEpSetOutputStream(output_id, desc, format, in_type.id, in_id, in_stream_id, false);
        EPConst.logAction("epSetOutputStream", ret, String.format("output_id: %d, tx_desc: %s, tx_format: %s, in_type: %s, in_id: %d, in_stream_id: %d",
                output_id, desc, format, in_type.name(), in_id, in_stream_id));
        return ret;
    }

    public int epSetOutputStreamByForward(int output_id, StreamDesc tx_desc,
                                          EPObjectType in_type, int in_id, int in_stream_id) {
        if (in_type == EPObjectType.Display || in_type == EPObjectType.Mixer) {
            LogUtil.e(EPConst.TAG, "epSetOutputStreamByForward, invalid in_type: " + in_type.name());
            return BaseError.INVALID_PARAM;
        }

        String desc = new Gson().toJson(tx_desc);
        int ret = jniEpSetOutputStream(output_id, desc, "", in_type.id, in_id, in_stream_id, true);
        EPConst.logAction("epSetOutputStreamByForward", ret, String.format("output_id: %d, tx_desc: %s, in_type: %s, in_id: %d, in_stream_id: %d",
                output_id, desc, in_type.name(), in_id, in_stream_id));
        return ret;
    }

    // @brief start output
    // @param output_id [IN] output id
    // @return 0 is suc or failed
    public int epStartOutput(int output_id) {
        int ret = jniEpStartOutput(output_id);
        EPConst.logAction("epStartOutput", ret, output_id);
        return ret;
    }

    // @brief pause output
    // @param output_id [IN] output id
    // @return 0 is suc or failed
    public int epPauseOutput(int output_id) {
        int ret = jniEpPauseOutput(output_id);
        EPConst.logAction("epPauseOutput", ret, output_id);
        return ret;
    }

    // @brief unPause output
    // @param output_id [IN] output id
    // @return 0 is suc or failed
    public int epUnPauseOutput(int output_id) {
        int ret = jniEpUnPauseOutput(output_id);
        EPConst.logAction("epUnPauseOutput", ret, output_id);
        return ret;
    }

    // @brief stop output
    // @param output_id [IN] output id
    // @return 0 is suc or failed
    public int epStopOutput(int output_id) {
        int ret = jniEpStopOutput(output_id);
        EPConst.logAction("epStopOutput", ret, output_id);
        return ret;
    }

    // @brief release output
    // @param output_id [IN] output id
    // @return 0 is suc or failed
    public int epReleaseOutput(int output_id) {
        int ret = jniEpReleaseOutput(output_id);
        EPConst.logAction("epReleaseOutput", ret, output_id);
        return ret;
    }

    /////////////////////////////// callbacks for ep

    public void onError(int errcode, String error) {
        LogUtil.i(EPConst.TAG, String.format("onError, error:%s errcode:%d", error, errcode));
        if (m_ep_cb == null)
            throw new RuntimeException("NOT set ep callback to ep3");

        m_ep_cb.onError(errcode, error);
    }

    public void onEvent(int obj_type, int obj_id, int event, String params) {
        EPObjectType type = EPObjectType.fromId(obj_type);
        if (type == null) {
            LogUtil.w(EPConst.TAG, String.format("onEvent got invalid params, obj_type:%d obj_id:%d event:%d params:%s",
                    obj_type, obj_id, event, params));
            return;
        }

        if (event >= 0) {
            EPEvent evt = EPEvent.fromValue(event);
            if (evt == null) {
                LogUtil.w(EPConst.TAG, String.format("onEvent got invalid params, obj_type:%s obj_id:%d event:%d params:%s",
                        type.name(), obj_id, event, params));
                return;
            }

            LogUtil.i(EPConst.TAG, String.format("onEvent, obj_type:%s obj_id:%d event:%s params:%s",
                    type.name(), obj_id, evt.name(), params));

            if (m_ep_cb == null)
                throw new RuntimeException("NOT set ep callback to ep3");

            try {
                if (evt == EPEvent.OutputStatusChanged)
                    m_ep_cb.onObjEvent(type, obj_id, evt, OutputStatus.fromCode(Integer.valueOf(params)));
                else
                    m_ep_cb.onObjEvent(type, obj_id, evt, params);
            } catch (Exception e) {
                LogUtil.w(EPConst.TAG, "onEvent error", e);
            }

        } else {
            // re-calc event id
            event *= -1;

            MediaEventId evt = MediaEventId.fromValue(event);
            if (evt == MediaEventId.UNKNOWN) {
                LogUtil.w(EPConst.TAG, String.format("onEvent got invalid params, obj_type:%s obj_id:%d event:%d params:%s",
                        type.name(), obj_id, event, params));
                return;
            }

            if (evt != MediaEventId.AUDIO_VOLUME_REPORT) {
                LogUtil.i(EPConst.TAG, String.format("onMediaEvent, obj_type:%s obj_id:%d event:%s params:%s",
                        type.name(), obj_id, evt.name(), params));
            }

            switch (evt) {
                case SOURCE_DECODING_STATE_CHANGED:
                    if (m_stream_cb == null)
                        throw new RuntimeException("NOT set stream callback to ep3");

                    try {
                        JsonObject json = new Gson().fromJson(params, JsonObject.class);
                        boolean ready = json.get("ready").getAsBoolean();
                        int mediaSourceId = json.get("media_source_id").getAsInt();

                        m_stream_cb.onRxVideoStreamAvailabilityChanged(type, obj_id, mediaSourceId, ready);
                    } catch (Exception e) {
                        LogUtil.w(EPConst.TAG, "SOURCE_DECODING_STATE_CHANGED error", e);
                    }

                    break;

                case AUDIO_VOLUME_REPORT:
                    if (m_ep_cb != null)
                        m_ep_cb.onVolumeReport(params);
                    break;

                default:
                    return;
            }
        }
    }

    /////////////////////////////// callbacks for stream

    public void onStreamOpen(int parent_type, int parent_id, int stream_id, String desc, String format) {
        EPObjectType type = EPObjectType.fromId(parent_type);
        StreamDesc description = new Gson().fromJson(desc, StreamDesc.class);
        if (type == null || !description.isValid()) {
            LogUtil.d(EPConst.TAG, String.format("onStreamOpen, parent_type:%d parent_id:%d stream_id:%d desc:%s format:%s",
                    parent_type, parent_id, stream_id, desc, format));
            LogUtil.w(EPConst.TAG, "invalid stream, ignore it");
            return;
        }

        LogUtil.i(EPConst.TAG, String.format("onStreamOpen, parent_type:%s parent_id:%d stream_id:%d desc:%s format:%s",
                type.name(), parent_id, stream_id, desc, format));

        Object avFormat;
        if (description.type == DataType.AUDIO) {
            avFormat = new Gson().fromJson(format, AudioFormat.class);
        } else if (description.type == DataType.VIDEO || description.type == DataType.VIDEO_EXT) {
            avFormat = new Gson().fromJson(format, VideoFormat.class);
        } else {
            avFormat = format;
        }

        if (m_stream_cb == null)
            throw new RuntimeException("NOT set stream callback to ep3");

        m_stream_cb.onStreamOpen(type, parent_id, stream_id, description, avFormat);
    }

    public void onStreamClose(int parent_type, int parent_id, int stream_id, String desc) {
        EPObjectType type = EPObjectType.fromId(parent_type);
        StreamDesc description = new Gson().fromJson(desc, StreamDesc.class);
        if (type == null || !description.isValid()) {
            LogUtil.d(EPConst.TAG, String.format("onStreamClose, parent_type:%d parent_id:%d stream_id:%d desc:%s",
                    parent_type, parent_id, stream_id, desc));
            LogUtil.w(EPConst.TAG, "invalid stream, ignore it");
            return;
        }

        LogUtil.i(EPConst.TAG, String.format("onStreamClose, parent_type:%s parent_id:%d stream_id:%d desc:%s",
                type.name(), parent_id, stream_id, desc));

        if (m_stream_cb == null)
            throw new RuntimeException("NOT set stream callback to ep3");

        m_stream_cb.onStreamClose(type, parent_id, stream_id, description);
    }

    /////////////////////////////// callbacks for calling

    public void onIncomingCall(int call_id, String number, String call_url, String protocol) {
        LogUtil.i(EPConst.TAG, String.format("onIncomingCall, call_id:%d number:%s call_url:%s protocol:%s",
                call_id, number, call_url, protocol));
        if (m_call_cb == null)
            throw new RuntimeException("NOT set calling callback to ep3");

        m_call_cb.onIncomingCall(call_id, number, call_url, CallingProtocol.fromName(protocol));
    }

    public void onRemoteRinging(int call_id) {
        LogUtil.i(EPConst.TAG, String.format("onRemoteRinging, call_id:%d", call_id));
        if (m_call_cb == null)
            throw new RuntimeException("NOT set calling callback to ep3");

        m_call_cb.onRemoteRinging(call_id);
    }

    public void onEstablished(int call_id, String vendor, String display_name) {
        LogUtil.i(EPConst.TAG, String.format("onEstablished, call_id:%d vendor:%s display_name:%s",
                call_id, vendor, display_name));
        if (m_call_cb == null)
            throw new RuntimeException("NOT set calling callback to ep3");

        m_call_cb.onEstablished(call_id, vendor, display_name);
    }

    public void onFinished(int call_id, int errcode, String reason) {
        LogUtil.i(EPConst.TAG, String.format("onFinished, call_id:%d reason:%s errcode:%d", call_id, reason, errcode));

        if (m_call_cb == null)
            throw new RuntimeException("NOT set calling callback to ep3");

        m_call_cb.onFinished(call_id, errcode, reason);
    }

    public void onCallerError(int call_id, int errcode, String error) {
        LogUtil.i(EPConst.TAG, String.format("onCallerError, call_id:%d error:%s errcode:%d", call_id, error, errcode));

        if (m_call_cb == null)
            throw new RuntimeException("NOT set calling callback to ep3");

        m_call_cb.onCallerError(call_id, errcode, error);
    }

    /////////////////////////////// callbacks for registering

    public void onRegistering(int result, String protocol) {
        LogUtil.i(EPConst.TAG, String.format("onRegistering, result:%d protocol:%s", result, protocol));
        if (m_reg_cb == null)
            throw new RuntimeException("NOT set registering callback to ep3");

        m_reg_cb.onRegistering(result, CallingProtocol.fromName(protocol));
    }

    public void onUnRegistering(int result, String protocol) {
        LogUtil.i(EPConst.TAG, String.format("onUnRegistering, result:%d protocol:%s", result, protocol));
        if (m_reg_cb == null)
            throw new RuntimeException("NOT set registering callback to ep3");

        m_reg_cb.onUnRegistering(result, CallingProtocol.fromName(protocol));
    }

    public void onNotifyRegisterStatus(int result, String protocol) {
        LogUtil.d(EPConst.TAG, String.format("onNotifyRegisterStatus, result:%d protocol:%s", result, protocol));
        if (m_reg_cb == null)
            throw new RuntimeException("NOT set registering callback to ep3");

        m_reg_cb.onNotifyRegisterStatus(result, CallingProtocol.fromName(protocol));
    }

    /////////////////////////////// init and configure native interfaces

    // Inits jni environment
    // NOTE: ONLY this method is called ONCE in single instance constuctor function.
    // private static native int jniEnvInit();
    private native int jniEnvInit();

    /// Endpoint
    // Inits endpoint
    private native int jniEpInit(String config);

    // Uninits endpoint, release resources. when exit, call this method
    private native int jniEpUninit();

    // Configures endpoint, like agc, aec and so on.
    // Param is in JSON-FORMAT.
    private native int jniEpConfigure(String property, String value);

    // Get statistics for media engine
    private native String jniEpGetMediaStatistics();

    /////////////////////////////// display native interfaces

    // Add video display
    private native int jniEpAddDisplay(int display_id);

    // Add video display with properties: resolution and rate
    private native int jniEpAddDisplay2(int display_id, int width, int height, int rate);

    // Remove video display
    private native int jniEpRemoveDisplay(int display_id);

    // Config video display
    private native int jniEpConfigureDisplay(int display_id, String config);

    /////////////////////////////// audio device native interfaces

    // Bind a audio stream to a output device
    private native int jniEpBindAudioOutputDevice(int device_id, int input_parent_type, int input_parent_id, int input_stream_id);

    private native int jniEpUnBindAudioOutputDevice(int device_id, int input_parent_type, int input_parent_id, int input_stream_id);

    /////////////////////////////// audio mixer native interfaces

    // Add audio mixer
    private native int jniEpAddMixer();

    // Remove audio mixer
    private native int jniEpRemoveMixer(int mixer_id);

    // Config audio mixer
    /*
     * {
     *   "volume": <number>  // (float: 0.0~1.0)
     *   "tracks" : [
     *     { "type" : "stream", "id" : <id>, "volume" : <number> },
     *     { "type" : "stream", "id" : <id>, "volume" : <number> },
     *     ...
     *   ]
     * }
     * */
    private native int jniEpConfigureMixer(int mixer_id, String config);

    // Dtmf signal
    private native int jniEpSendDTMFOnMixer(int mixer_id, char key);

    /////////////////////////////// source(rtsp/rtmp/file/capture) native interfaces

    // Add a net source
    // @properties e.g.
    // capture:
    // {
    //      "resolution": "1920x1080",
    //      "framerate":  60
    // }
    // net source:
    // {
    //      "reconnect": {
    //          "count": 1,
    //          "min_interval_ms": 1000,
    //          "max_interval_ms": 10000,
    //          "interval_step_ms": 1000
    //      }
    // }
    private native int jniEpAddSource(String url, String properties);

    // Start to decode source stream
    private native int jniEpStartSrcStreamDecoding(int source_id, int stream_id);

    // Stop to decode source stream
    private native int jniEpStopSrcStreamDecoding(int source_id, int stream_id);

    // Remove the source
    private native int jniEpRemoveSource(int source_id);

    /////////////////////////////// calling native interfaces

    // Create a call handler in H.323/SIP/others protocol which would be used for calling remote
    // Return call id, and all other ops need this call id.
    private native int jniEpCreateCall(String url);

    // Configures the call, like bandwidth, capabilities and so on.
    // Param is in JSON-FORMAT.
    private native int jniEpConfigureCall(int call_id, String property, String value);

    // Makes a call, call the remote peer
    private native int jniEpCall(int call_id);

    // Hangs up a call
    private native int jniEpHangup(int call_id);

    // Releases a call
    private native int jniEpRelease(int call_id);

    // Recalls
    private native int jniEpRecall(int call_id);

    // Accepts a incoming call
    private native int jniEpAccept(int call_id);

    // Rejects a incoming call
    private native int jniEpReject(int call_id);

    // Opens ext tx stream
    private native int jniEpOpenExtTxStream(int call_id, String tx_desc);

    // Closes ext tx stream
    private native int jniEpCloseExtTxStream(int call_id);

    // Set and start calling tx stream
    private native int jniEpStartTxStream(int call_id, int tx_stream_id, String tx_format, int in_type,
                                          int in_id, int in_stream_id, boolean by_forward);

    // Stop calling tx stream
    private native int jniEpStopTxStream(int call_id, int tx_stream_id);

    // Start to decode calling rx stream
    private native int jniEpStartRxStreamDecoding(int call_id, int tx_stream_id);

    // Stop to decode calling rx stream
    private native int jniEpStopRxStreamDecoding(int call_id, int tx_stream_id);

    // Send key frame request
    private native int jniEpSendKeyFrameRequest(int call_id, int tx_video_id);

    // Get ep calling statistics
    private native String jniEpGetCallingStatistics(int call_id);

    /// Register gk and sip server.
    // Register GK server
    private native int jniEpRegisterGK(String config);

    // Unregister GK server
    private native int jniEpUnregisterGK();

    // Register SIP server
    private native int jniEpRegisterSIP(String config);

    // Unregister SIP server
    private native int jniEpUnregisterSIP();

    /////////////////////////////// output(file or net stream) native interfaces

    // Create output
    private native int jniEpCreateOutput(String url, String properties);

    // Set output stream
    private native int jniEpSetOutputStream(int output_id, String tx_stream, String tx_format,
                                            int in_type, int in_id, int in_stream_id, boolean by_forward);

    // Start output
    private native int jniEpStartOutput(int output_id);

    // Pause output
    private native int jniEpPauseOutput(int output_id);

    // UnPause output
    private native int jniEpUnPauseOutput(int output_id);

    // Stop output
    private native int jniEpStopOutput(int output_id);

    // Release output
    private native int jniEpReleaseOutput(int output_id);

    /////////////////////////////// private utils

    private static boolean isValidVolume(float volume) {
        return volume >= 0.0f && volume <= 1.0f;
    }

} // End of Endpoint3
