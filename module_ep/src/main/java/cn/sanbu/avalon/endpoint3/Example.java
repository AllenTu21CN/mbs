package cn.sanbu.avalon.endpoint3;

import android.content.Context;
import android.view.Surface;

import com.sanbu.board.EmptyBoardSupportClient;
import com.sanbu.tools.CompareHelper;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.AACProfile;
import cn.sanbu.avalon.endpoint3.structures.Region;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.AudioInputDevice;
import cn.sanbu.avalon.endpoint3.structures.AudioSamplerate;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.DisplayCell;
import cn.sanbu.avalon.endpoint3.structures.jni.DisplayConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.EPDir;
import cn.sanbu.avalon.endpoint3.structures.jni.EPEvent;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.EPObjectType;
import cn.sanbu.avalon.endpoint3.structures.H264Profile;
import cn.sanbu.avalon.endpoint3.structures.jni.MixerTracks;
import cn.sanbu.avalon.endpoint3.structures.jni.Reconnecting;
import cn.sanbu.avalon.endpoint3.structures.Resolution;
import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;
import cn.sanbu.avalon.endpoint3.structures.TransProtocol;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapabilities;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoCapability;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;
import cn.sanbu.avalon.media.MediaJni;

public class Example implements Endpoint3.EPCallback, Endpoint3.StreamCallback,
        Endpoint3.CallingCallback, Endpoint3.RegisteringCallback {

    private static final Region SINGLE_SCREEN_REGION = new Region(0.0f, 0.0f, 1.0f, 1.0f);

    public static void firstOfAll(Context context) {
        // init endpoint jni environment
        Endpoint3.initEnv(context, "ep3_android");

        // init media engine jni environment
        MediaJni.initEnv(context, new EmptyBoardSupportClient(), true);
    }

    private static class Caller {
        public int id;
        public int mixer;
        public List<Integer> aRxStreams = new LinkedList<>();
        public List<Integer> aTxStreams = new LinkedList<>();
        public List<Integer> vRxStreams = new LinkedList<>();
        public List<Integer> vTxStreams = new LinkedList<>();
    }

    private Endpoint3 endpoint3;

    private int localDisplay;
    private int localDisplayExt;
    private int localMixer;
    private int speaker;

    private int micSource;
    private int micStream;
    private int hdmiSouce;
    private int hdmiStream;
    private int ipcSource;
    private int ipcStream;

    private int lrDisplay;
    private int lrMixer;
    private int live;

    private int txDisplay;
    private int txDisplayExt;

    private Caller caller1;
    private Caller caller2;

    public void init() {
        initEP();

        initSpeaker();

        initLR();

        addSources();
    }

    public void onLocalDisplayReady(boolean isMain, Surface handle, int format, int width, int height) {
        if (isMain) {
            // create a visible display by main surface
            localDisplay = endpoint3.epAddVisibleDisplay(handle);

            // init it
            endpoint3.epChangeVisibleDisplay(localDisplay, format, width, height);
        } else {
            // create a visible display by ext surface
            localDisplayExt = endpoint3.epAddVisibleDisplay(handle);

            // init it
            endpoint3.epChangeVisibleDisplay(localDisplayExt, format, width, height);
        }
    }

    public void makeP2PCall() {

    }

    public void makeMeeting() {
        // meeting params
        Resolution resolution = Resolution.RES_1080P;
        Bandwidth bandwidth = Bandwidth._4M;
        int framerate = 30;
        VideoCapabilities CALLING_VIDEO_CAPABILITIES_1080P = new VideoCapabilities(
                new VideoCapabilities.Capabilities(
                        Arrays.asList(new VideoCapability(CodecType.H264, H264Profile.BaseLine, resolution, framerate, 10)),
                        Arrays.asList(new VideoCapability(CodecType.H264, H264Profile.BaseLine, resolution, framerate/2, 10))),
                new VideoCapabilities.Capabilities(
                        Arrays.asList(new VideoCapability(CodecType.H264, H264Profile.BaseLine, resolution, framerate, 10)),
                        Arrays.asList(new VideoCapability(CodecType.H264, H264Profile.BaseLine, resolution, framerate/2, 10)))
        );

        // create invisible display as tx video scene
        txDisplay = endpoint3.epAddInvisibleDisplay(resolution.width, resolution.width, framerate);
        txDisplayExt = endpoint3.epAddInvisibleDisplay(resolution.width, resolution.width, framerate / 2);

        startLR();

        // call two remotes
        int callerId = endpoint3.epCreateCaller("sip:10.1.126.101");
        endpoint3.epSetCallBandwidth(callerId, bandwidth);
        endpoint3.epSetCallVideoCapabilities(callerId, CALLING_VIDEO_CAPABILITIES_1080P);
        int mixer = endpoint3.epAddMixer();
        caller1 = new Caller();
        caller1.id = callerId;
        caller1.mixer = mixer;
        endpoint3.epCall(callerId);

        callerId = endpoint3.epCreateCaller("sip:10.1.126.101");
        endpoint3.epSetCallBandwidth(callerId, bandwidth);
        endpoint3.epSetCallVideoCapabilities(callerId, CALLING_VIDEO_CAPABILITIES_1080P);
        endpoint3.epCall(callerId);
        mixer = endpoint3.epAddMixer();
        caller2 = new Caller();
        caller2.id = callerId;
        caller2.mixer = mixer;
    }

    /////////////////////////////// private functions

    private void initEP() {
        // create and get endpoint3 instance
        endpoint3 = Endpoint3.getInstance();

        // init endpoint3
        EPFixedConfig config = buildEPFixedConfig();
        endpoint3.setCallingCallback(this);
        endpoint3.setCallingCallback(this);
        endpoint3.epInit(config, this, this);

        // config other properties
        endpoint3.epSwitchAGC(true);
    }

    private void initSpeaker() {
        // create a mixer as local audio scene
        localMixer = endpoint3.epAddMixer();

        // select a speaker
        speaker = endpoint3.epGetDefaultAudioOutputDevice().id;

        // bind the local mixer to the speaker
        endpoint3.epBindAudioOutputDevice(speaker, EPObjectType.Mixer, localMixer, -1);
    }

    private void initLR() {
        // create a invisible display as lr display
        lrDisplay = endpoint3.epAddInvisibleDisplay(1920, 1080, 30);

        // create a mixer as lr audio scene
        lrMixer = endpoint3.epAddMixer();
    }

    private void addSources() {
        // add a mic source, get its stream later
        AudioInputDevice device = endpoint3.epGetDefaultAudioInputDevice();
        micSource = endpoint3.epAddAudioCapture(device.url);

        // add a hdmi video source, get its stream later
        hdmiSouce = endpoint3.epAddVideoCapture(EPConst.LOCAL_VIDEO_CAPTURE1, Resolution.RES_1080P);

        // add a ipc video source, get its stream later
        String ipcUrl = "rtsp://10.10.10.10:554/ch3";
        Reconnecting reconnecting = new Reconnecting(-1, 3000, 8000, 100);
        ipcSource = endpoint3.epAddRTSPSource(ipcUrl, TransProtocol.TCP, reconnecting);
    }

    private void startLR() {
        live = endpoint3.epCreateNetOutput("rtmp://127.0.0.1:11935/live/test");

        StreamDesc desc = new StreamDesc(DataType.AUDIO, DataType.AUDIO.name, DataType.AUDIO.name, EPDir.Outgoing);
        AudioFormat audioFormat = new AudioFormat(CodecType.AAC, AudioSamplerate.HZ_48K, 2, Bandwidth._32K, AACProfile.LD);
        endpoint3.epSetOutputStream(live, desc, audioFormat, EPObjectType.Mixer, lrMixer);

        desc = new StreamDesc(DataType.VIDEO, DataType.VIDEO.name, DataType.VIDEO.name, EPDir.Outgoing);
        VideoFormat videoFormat = new VideoFormat(CodecType.H264, H264Profile.BaseLine, Resolution.RES_1080P, 30, Bandwidth._4M, 100);
        endpoint3.epSetOutputStream(live, desc, videoFormat, EPObjectType.Display, lrDisplay);

        endpoint3.epStartOutput(live);
    }

    /////////////////////////////// implementation of Endpoint3.Callback

    @Override
    public void onStreamOpen(EPObjectType parentType, int parentId, int streamId, StreamDesc desc, Object format) {
        if (parentType == EPObjectType.Source) {
            if (parentId == micSource) {
                micStream = streamId;
                // this is not necessary for mic stream
                int decId = endpoint3.epStartSrcStreamDecoding(parentId, streamId);

                MixerTracks.Track track = new MixerTracks.Track(decId, 1.0f);
                MixerTracks tracks = new MixerTracks(Arrays.asList(track));
                endpoint3.epSetMixer(caller1.mixer, 1.0f, tracks);
                endpoint3.epSetMixer(caller2.mixer, 1.0f, tracks);
                endpoint3.epSetMixer(lrMixer, 1.0f, tracks);
            } else if (parentId == hdmiSouce) {
                hdmiStream = streamId;
                // this is not necessary for hdmi stream
                int decId = endpoint3.epStartSrcStreamDecoding(parentId, streamId);

                DisplayCell cell = DisplayCell.buildStream(0, decId);
                DisplayConfig config = DisplayConfig.buildOverlays(Arrays.asList(SINGLE_SCREEN_REGION), Arrays.asList(cell));
                endpoint3.epConfigureDisplay(txDisplayExt, config);
                endpoint3.epConfigureDisplay(lrDisplay, config);
            } else if (parentId == ipcSource) {
                ipcStream = streamId;
                int decId = endpoint3.epStartSrcStreamDecoding(parentId, streamId);

                DisplayCell cell = DisplayCell.buildStream(0, decId);
                DisplayConfig config = DisplayConfig.buildOverlays(Arrays.asList(SINGLE_SCREEN_REGION), Arrays.asList(cell));
                endpoint3.epConfigureDisplay(txDisplay, config);
                endpoint3.epConfigureDisplay(lrDisplay, config);
            }

        } else if (parentType == EPObjectType.Caller) {
            Caller caller = parentId == caller1.id ? caller1 : caller2;

            if (desc.direction == EPDir.Outgoing) {
                if (desc.type == DataType.AUDIO) {
                    AudioFormat aFormat = (AudioFormat) format;
                    caller.aTxStreams.add(streamId);
                    endpoint3.epStartTxStream(caller.id, streamId, aFormat, EPObjectType.Mixer, caller.mixer);
                } else if (desc.type == DataType.VIDEO || desc.type == DataType.VIDEO_EXT) {
                    VideoFormat vFormat = (VideoFormat) format;
                    caller.vTxStreams.add(streamId);
                    int display = CompareHelper.isEqual(desc.name, DataType.VIDEO_EXT.name) ? txDisplay : txDisplayExt;
                    endpoint3.epStartTxStream(caller.id, streamId, vFormat, EPObjectType.Display, display);
                }
            } else if (desc.direction == EPDir.Incoming) {
                if (desc.type == DataType.AUDIO) {
                    caller.aRxStreams.add(streamId);
                    int decId = endpoint3.epStartRxStreamDecoding(caller.id, streamId);

                    MixerTracks.Track track = new MixerTracks.Track(decId, 1.0f);
                    MixerTracks tracks = new MixerTracks(Arrays.asList(track));
                    endpoint3.epSetMixer(localMixer, 1.0f, tracks);
                } else if (desc.type == DataType.VIDEO || desc.type == DataType.VIDEO_EXT) {
                    caller.vRxStreams.add(streamId);
                    int decId = endpoint3.epStartRxStreamDecoding(caller.id, streamId);

                    DisplayCell cell = DisplayCell.buildStream(0, decId);
                    DisplayConfig config = DisplayConfig.buildOverlays(Arrays.asList(SINGLE_SCREEN_REGION), Arrays.asList(cell));
                    if (desc.type == DataType.VIDEO_EXT)
                        endpoint3.epConfigureDisplay(localDisplayExt, config);
                    else
                        endpoint3.epConfigureDisplay(localDisplay, config);
                }
            }
        }
    }

    @Override
    public void onStreamClose(EPObjectType parentType, int parentId, int streamId, StreamDesc desc) {

    }

    @Override
    public void onRxVideoStreamAvailabilityChanged(EPObjectType parentType, int parentId, int decId, boolean ready) {

    }

    @Override
    public void onIncomingCall(int callId, String number, String callUrl, CallingProtocol protocol) {

    }

    @Override
    public void onFinished(int callId, int errCode, String reason) {
        Caller caller = callId == caller1.id ? caller1 : caller2;

        endpoint3.epRemoveMixer(caller.mixer);
        endpoint3.epReleaseCaller(caller.id);
        caller = null; // caller1 or caller2

        // TODO
    }

    @Override
    public void onEstablished(int callId, String vendor, String name) {
        endpoint3.epOpenExtTxStream(callId, new StreamDesc(DataType.VIDEO, DataType.VIDEO_EXT.name, "辅流", EPDir.Outgoing));
    }

    /////////////////////////////// static private utils

    private static final EPFixedConfig buildEPFixedConfig() {
        return new EPFixedConfig(
                true, true, true,
                5060, 1720, 17070, 20000, 21999,
                20000, 21999, "", "",
                1, true, "DEBUG", "#0F0F0F"
        );
    }

    ///////////////////// ignore callbacks

    @Override
    public void onError(int errCode, String reason) {

    }

    @Override
    public void onRegistering(int result, CallingProtocol protocol) {

    }

    @Override
    public void onUnRegistering(int result, CallingProtocol protocol) {

    }

    @Override
    public void onNotifyRegisterStatus(int result, CallingProtocol protocol) {

    }

    @Override
    public void onRemoteRinging(int callId) {

    }

    @Override
    public void onCallerError(int callId, int errCode, String reason) {

    }

    @Override
    public void onEvent(EPObjectType objType, int objId, EPEvent event, String params) {

    }
}
