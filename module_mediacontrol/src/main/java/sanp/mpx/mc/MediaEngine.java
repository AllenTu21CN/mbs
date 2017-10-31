package sanp.mpx.mc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Choreographer;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sanp.avalon.libs.SimpleTesting;
import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.Tuple;
import sanp.avalon.libs.media.audio.AudioCapturer;
import sanp.avalon.libs.media.audio.AudioDecoder;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVDefines.DataFlag;
import sanp.avalon.libs.media.base.AVDefines.DataType;
import sanp.avalon.libs.media.audio.AudioEncoder;

import sanp.avalon.libs.media.base.AVPacket;
import sanp.mpx.mc.gles.Drawable2d;
import sanp.mpx.mc.gles.FlatShadedProgram;
import sanp.mpx.mc.gles.FullFrameRect;
import sanp.mpx.mc.gles.OffscreenSurface;
import sanp.mpx.mc.gles.Sprite2d;
import sanp.mpx.mc.gles.Texture2dProgram;
import sanp.mpx.mc.gles.WindowSurface;
import sanp.mpx.mc.gles.GlUtil;
import sanp.mpx.mc.gles.EglCore;

public final class MediaEngine implements Choreographer.FrameCallback {
    public static final int MAXIMUM_VIDEO_STREAM_COUNT = 6;
    public static final int MAXIMUM_VIDEO_SCENE_COUNT = 2;
    public static final int MAXIMUM_VIDEO_SINK_COUNT = 3;
    public static final int MAXIMUM_AUDIO_STREAM_COUNT = MAXIMUM_VIDEO_STREAM_COUNT;
    public static final int MAXIMUM_AUDIO_SINK_COUNT = MAXIMUM_VIDEO_SINK_COUNT;
    public static final float VIDEO_RENDER_FPS = 60.0f;
    private static final long VIDEO_RENDER_INTERVAL_NS = Math.round(1000000000L / VIDEO_RENDER_FPS);
    private static final long VIDEO_RENDER_INTERVAL_MS = Math.round(1000L / VIDEO_RENDER_FPS);
    private static final long VIDEO_RENDER_PRECISION_NS = 5000000;
    private static final boolean VIDEO_ENCODING_DROPPING = (VIDEO_RENDER_FPS >= 48.0f);

    private static MediaEngine mInstance = null;
    private Callback mCallback = null;
    private static Context mContext = null;

    private int mObjectSeq = 0;
    private int mVideoStreamIds[]   = new int[MAXIMUM_VIDEO_STREAM_COUNT];
    private int mVideoSceneIds[]    = new int[MAXIMUM_VIDEO_SCENE_COUNT];
    private int mVideoSinkIds[]     = new int[MAXIMUM_VIDEO_SINK_COUNT];
    private int mAudioStreamIds[]   = new int[MAXIMUM_AUDIO_STREAM_COUNT];
    private int mAudioSinkIds[]     = new int[MAXIMUM_AUDIO_SINK_COUNT];

    private VideoEngine mVideoEngine;
    private VideoEngineHandler mVideoEngineHandler;

    private AudioEngine mAudioEngine;

    private long mNextRenderTimeNs = 0;

    interface Callback {
        // Status changed
        void onVideoStreamStarted(int streamId);
        void onVideoStreamFinished(int streamId);
        void onVideoStreamError(int streamId, int errorCode);

        void onVideoStreamResolutionChanged(int streamId, int width, int height,
                                            int previousWidth, int previousHeight);
        void onVideoStreamForamtChanged(int streamId, int format, int previousFormat);
        void onVideoStreamStatistics(int streamId, float fps, int kbps);

        void onVideoSinkStarted(int sinkId);
        void onVideoSinkFinished(int sinkId);
        void onVideoSinkError(int sinkId, int errorCode);
        void onVideoSinkStatistics(int sinkId, float fps, int kbps);

        void onVideoRendererFpsUpdated(float fps, long droppedFrame);

        // Operation done
        void onAddVideoStreamDone(boolean success, String errorMessage);
        void onRemoveVideoStreamDone(boolean success, String errorMessage);

        void onAddVideoSceneDone(boolean success, String errorMessage);
        void onRemoveVideoSceneDone(boolean success, String errorMessage);

        void onPostVideoSceneCommandsDone(boolean success, String errorMessage);

        void onAddVideoSinkDone(boolean success, String errorMessage);
        void onRemoveVideoSinkDone(boolean success, String errorMessage);
        void onConfigureVideoSinkDone(boolean success, String errorMessage);
        void onStartVideoSinkDone(boolean success, String errorMessage);
        void onStopVideoSinkDone(boolean success, String errorMessage);
    }

    public static class VideoSinkConfig {
        public static final String MIME_TYPE_H264 = "video/avc";

        public int width;
        public int height;
        public int frameRate;
        public String mimeType;
        public int bitrate;
        public int keyFrameInterval;

        public VideoSinkConfig() {
        }
        
        public VideoSinkConfig(String codec, int w, int h, int f, int b, int i) {
            mimeType = codec;
            width = w;
            height = h;
            frameRate = f;
            bitrate = b;
            keyFrameInterval = i;
        }

        public void from(VideoSinkConfig other) {
            mimeType = other.mimeType;
            width = other.width;
            height = other.height;
            frameRate = other.frameRate;
            bitrate = other.bitrate;
            keyFrameInterval = other.keyFrameInterval;
        }

        public boolean isValid() {
            /*
                <MediaCodec name="OMX.google.h264.encoder" type="video/avc">
                    <!-- profiles and levels:  ProfileBaseline : Level41 -->
                    <Limit name="size" min="16x16" max="1920x1088" />
                    <Limit name="alignment" value="2x2" />
                    <Limit name="block-size" value="16x16" />
                    <Limit name="blocks-per-second" range="1-244800" />
                    <Limit name="bitrate" range="1-12000000" />
                </MediaCodec>
             */
            if ((width >= 16 && height >= 16)
                    && (width <= 1920 && height <= 1088)
                    && ((width % 2 == 0) && (height % 2 == 0))
                    && (bitrate >= 1 && bitrate <= 12000000)
                    && (frameRate >= 1 && frameRate <= 30)
                    && (keyFrameInterval >= 1)
                    && (mimeType == MIME_TYPE_H264)) {
                return true;
            } else {
                return false;
            }
        }
        
        public boolean isEqual(VideoSinkConfig other) {
            return (width == other.width && 
                    height == other.height &&
                    frameRate == other.frameRate &&
                    mimeType == other.mimeType &&
                    bitrate == other.bitrate);
        }
    }

    public static class AudioSinkConfig {
        public static final String DefaultCodec = AudioEncoder.Supporting.ENC_NAME_AAC;
        public static final int DefaultProfile = AudioEncoder.Supporting.ENC_AAC_PROFILE_LC;
        public static final int DefaultBitrate = 64000;

        public AudioEncoder.Format encFormat = null;

        public AudioSinkConfig() {
            encFormat = new AudioEncoder.Format(DefaultCodec, DefaultBitrate, DefaultProfile);
        }

        public AudioSinkConfig(String codec, int bitrate, int profile) {
            encFormat = new AudioEncoder.Format(codec, bitrate, profile);
        }

        public void from(AudioSinkConfig other) {
            encFormat.from(other.encFormat);
        }

        public boolean isValid() {
            return AudioEncoder.isValid(encFormat);
        }

        public boolean isEqual(AudioSinkConfig other) {
            return encFormat.isEqual(other.encFormat);
        }
    }

    public static class SinkOutput {
        public int track_id = -1;
        public AVChannel output_channel = null;
        public SinkOutput() { }
        public SinkOutput(int id, AVChannel channel) {
            track_id = id;
            output_channel = channel;
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof SinkOutput) {
                SinkOutput o = (SinkOutput)other;
                return (track_id == o.track_id &&
                        output_channel == o.output_channel); //TODO: is OK?
            }
            return false;
        }
    }

    /*
     * Choreographer callback, called near vsync.
     *
     * @see android.view.Choreographer.FrameCallback#doFrame(long)
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mVideoEngineHandler != null) {
            if(VIDEO_RENDER_FPS >= 59.0f) {
                Choreographer.getInstance().postFrameCallback(this);
            } else {
                long delayMs;
                long currNs = System.nanoTime();
                if(mNextRenderTimeNs == 0) {
                    mNextRenderTimeNs = currNs + VIDEO_RENDER_INTERVAL_NS;
                    delayMs = VIDEO_RENDER_INTERVAL_MS;
                } else {
                    mNextRenderTimeNs += VIDEO_RENDER_INTERVAL_NS;
                    if (mNextRenderTimeNs > currNs + VIDEO_RENDER_PRECISION_NS) {
                        delayMs = (mNextRenderTimeNs - currNs) / 1000000;
                    } else {
                        delayMs = 0;
                        LogManager.d("!!!Performance issue: Choreographer is late to callback");
                    }
                }
                Choreographer.getInstance().postFrameCallbackDelayed(this, delayMs);
            }
            mVideoEngineHandler.sendRenderFrame(frameTimeNanos);
        }
    }

    public static MediaEngine allocateInstance(Context context) {
        if (mInstance == null) {
            synchronized (MediaEngine.class) {
                if (mInstance ==null) {
                    mInstance = new MediaEngine(context);
                }
            }
        }

        return mInstance;
    }

    public static MediaEngine getInstance() {
        return mInstance;
    }

    private MediaEngine(Context context) {
        LogManager.i("Constructing MediaEngine singleton object.");

        if (context != null) {
            mContext = context;
        }

        // Start video render thread
        LogManager.i("Starting video render thread.");
        mVideoEngine = new VideoEngine();
        mVideoEngine.setName("Video Renderer");
        mVideoEngine.start();
        mVideoEngine.waitUntilReady();

        // Get render handler
        mVideoEngineHandler = mVideoEngine.getHandler();
        if (mVideoEngineHandler != null) {
            // TODO: Extra initialize operations
        }

        mAudioEngine = new AudioEngine();

        // Start choreographer
        LogManager.i("Starting choreographer.");
        mNextRenderTimeNs = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    /**
     * Set callback object
     * @param callback object
     */
    public void setCallback(Callback callback) {
        // FIXME: thread safe?
        if (callback != null && mCallback != null) {
            LogManager.w("Media engine callback object already exists!");
        }

        mCallback = callback;
    }

    public Callback getCallback() {
        return mCallback;
    }

    private int getInternalObjectId() {
        return ++mObjectSeq;
    }

    /**
     * Add video stream
     *
     * @param url source url
     *            http://10.1.0.75:8080/test-1.mp4
     *            rtsp://10.1.0.75/hk
     *            file:///data/video/test-1.mp4
     * @return stream id
     */
    public int addVideoStream(String url) {
        // TODO: Validate url partern

        // Find empty cell
        for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
            if (mVideoStreamIds[i] == 0) {
                mVideoStreamIds[i] = getInternalObjectId();

                LogManager.i("Adding video stream #" + mVideoStreamIds[i] + " url=" + url);
                mVideoEngineHandler.sendAddVideoStream(mVideoStreamIds[i], url);

                return mVideoStreamIds[i];
            }
        }

        LogManager.e("addVideoStream: Can not add any more video streams!");
        return -1;
    }

    public int addVideoStream(@NonNull AVChannel channel) {
        // Find empty cell
        for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
            if (mVideoStreamIds[i] == 0) {
                mVideoStreamIds[i] = getInternalObjectId();

                LogManager.i("Adding video stream #" + mVideoStreamIds[i] + " format="
                        + channel.getMediaFormat().toString());
                mVideoEngineHandler.sendAddVideoStream(mVideoStreamIds[i], channel);

                return mVideoStreamIds[i];
            }
        }

        LogManager.e("addVideoStream: Can not add any more video streams!");
        return -1;
    }

    public int addVideoStream(int cameraId) {
        // Find empty cell
        for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
            if (mVideoStreamIds[i] == 0) {
                mVideoStreamIds[i] = getInternalObjectId();

                LogManager.i("Adding video stream #" + mVideoStreamIds[i] + " cameraId=" + cameraId);
                mVideoEngineHandler.sendAddVideoStream(mVideoStreamIds[i], cameraId);

                return mVideoStreamIds[i];
            }
        }

        LogManager.e("addVideoStream: Can not add any more video streams!");
        return -1;
    }

    /**
     * Remove video stream
     *
     * @param streamId
     * @return success or failure
     */
    public boolean removeVideoStream(int streamId) {
        if (streamId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                if (mVideoStreamIds[i] == streamId) {
                    // Found an empty slot
                    LogManager.i("Removing video stream #" + mVideoStreamIds[i]);
                    mVideoEngineHandler.sendRemoveVideoStream(mVideoStreamIds[i]);
                    mVideoStreamIds[i] = 0;

                    return true;
                }
            }
        }

        LogManager.e("removeVideoStream: stream #" + streamId + " not exists!");
        return false;
    }

    /**
     * Add a new video scene
     *
     * @return scene id
     */
    public int addVideoScene() {
        // Find empty cell
        for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
            if (mVideoSceneIds[i] == 0) {
                mVideoSceneIds[i] = getInternalObjectId();

                LogManager.i("Adding video scene #" + mVideoSceneIds[i]);
                mVideoEngineHandler.sendAddVideoScene(mVideoSceneIds[i]);

                return mVideoSceneIds[i];
            }
        }

        LogManager.e("addVideScene: Can not add any more video scenes!");
        return -1;
    }

    public int addVideoScene(Surface surface) {
        // Find empty cell
        for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
            if (mVideoSceneIds[i] == 0) {
                mVideoSceneIds[i] = getInternalObjectId();

                LogManager.i("Adding video scene (with surface) #" + mVideoSceneIds[i]);
                mVideoEngineHandler.sendAddVideoScene(mVideoSceneIds[i], surface);

                return mVideoSceneIds[i];
            }
        }

        LogManager.e("addVideoScene: Can not add any more video scenes!");
        return -1;
    }

    /**
     * Remove video scene
     *
     * @param sceneId
     * @return success or failure
     */
    public boolean removeVideoScene(int sceneId) {
        if (sceneId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoSceneIds[i] == sceneId) {
                    LogManager.i("Removing video scene #" + mVideoSceneIds[i]);
                    mVideoEngineHandler.sendRemoveVideoScene(mVideoSceneIds[i]);
                    mVideoSceneIds[i] = 0;

                    return true;
                }
            }
        }

        LogManager.e("removeVideoScene: scene id not exists!");
        return false;
    }

    public void notifyDisplaySurfaceChanged(int sceneId, int format, int width, int height) {
        if (sceneId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoSceneIds[i] == sceneId) {
                    LogManager.i("Notifing video scene #" + mVideoSceneIds[i]
                            + " display surface changed.");
                    mVideoEngineHandler.sendDisplaySurfaceChanged(sceneId, width, height);

                    return;
                }
            }
        }

        LogManager.e("notifyDisplaySurfaceChanged: scene id not exists!");
    }

    public void switchTestingItems(int sceneId, boolean able) {
        if (sceneId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoSceneIds[i] == sceneId) {
                    LogManager.i("switch video scene #" + mVideoSceneIds[i]
                            + " testing items " + able);
                    mVideoEngineHandler.sendSwitchTestingItems(sceneId, able);
                    return;
                }
            }
        }
        LogManager.e("switchTestingItems: scene id not exists!");
    }

    /**
     * Scene Commands:
     *   [Scene]
     *       set scene background-color <color> (string)
     *       set scene background-image <file> (string)
     *       set scene display-name <string> (charset: utf-8, optional: base64)
     *       set scene display-name-alignment <horizontal:vertical> (left|right|hcenter:top|bottom|vcenter)
     *       set scene display-name-style-sheet <string> (optional: base64)
     *       set scene display-name-visible <true|false>
     *       set scene frozen <true|false>
     *
     *   [Stream]
     *       set stream <id> src-rect <left:top:width:height> (float: 0.0~1.0)
     *       set stream <id> dst-rect <left:top:width:height> (float: 0.0~1.0)
     *       set stream <id> z-index <number> (int: 0~9999)
     *       set stream <id> opacity <number> (float: 0.0~1.0)
     *       set stream <id> visible <true|false>
     *       set stream <id> display-name <string> (charset: utf-8, optional: base64)
     *       set stream <id> display-name-alignment <horizontal:vertical> (left|right|hcenter:top|bottom|vcenter)
     *       set stream <id> display-name-style-sheet <string> (optional: base64)
     *       set stream <id> display-name-visible <true|false>
     *
     *   [Marquee]
     *       set marquee text <string> (charset: utf-8, optional: base64)
     *       set marquee alignment <vertical> (top|vcenter|bottom)
     *       set marquee direction <direction> (left|right)
     *       set marquee style-sheet <string> (optional: base64)
     *       set marquee scroll-delay <number> (int: 1~999ms)
     *       set marquee scroll-step <number> (int: 1~999px)
     *       set marquee visible <true|false>
     *
     *   [Debug]
     *       set debug-info visible <true|false>
     *       set id visible <true:false>
     *       dump frame <id> <file>
     *       dump internals <file>
     */
    public void postVideoSceneCommands(int videoSceneId, String[] commands) {
        if (videoSceneId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoSceneIds[i] == videoSceneId) {
                    mVideoEngineHandler.sendPostSceneCommands(videoSceneId, commands);
                    return;
                }
            }
        }

        LogManager.e("postVideoSceneCommands: scene id not exists!");
    }

    public int addVideoSink(int sceneId) {
        // Find empty cell
        for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
            if (mVideoSceneIds[i] == sceneId) {
                for (int j = 0; j < MAXIMUM_VIDEO_SINK_COUNT; ++j) {
                    if (mVideoSinkIds[j] == 0) {
                        mVideoSinkIds[j] = getInternalObjectId();
                        LogManager.i("Adding video sink #" + mVideoSinkIds[j]
                                + " to scene #" + mVideoSceneIds[i]);
                        mVideoEngineHandler.sendAddVideoSink(
                                mVideoSceneIds[i], mVideoSinkIds[j]);

                        return mVideoSinkIds[j];
                    }
                }

                LogManager.e("Can not add any more video sinks!");
                return -1;
            }
        }

        LogManager.e("addVideoSink: scene #" + sceneId + " not exists!");
        return -1;
    }

    public boolean removeVideoSink(int sinkId) {
        if (sinkId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinkIds[i] == sinkId) {
                    // Found an empty slot
                    LogManager.i("Removing video sink #" + mVideoSinkIds[i]);
                    mVideoEngineHandler.sendRemoveVideoSink(mVideoSinkIds[i]);
                    mVideoSinkIds[i] = 0;

                    return true;
                }
            }
        }

        LogManager.e("removeVideoSink: sink #" + sinkId + " not exists!");
        return false;
    }

    public void configureVideoSink(int sinkId, VideoSinkConfig config) {
        // Validate configuration
        if (!config.isValid()) {
            LogManager.e("configureVideoSink: configuration is invalid!");
            // TODO: Notify config video sink failed!
            return;
        }

        if (sinkId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinkIds[i] == sinkId) {
                    // Found an empty slot
                    LogManager.i("Configuring video sink #" + mVideoSinkIds[i]);
                    mVideoEngineHandler.sendConfigVideoSink(mVideoSinkIds[i], config);

                    return;
                }
            }
        }

        LogManager.e("configureVideoSink: sink #" + sinkId + " not exists!");
    }

    public void addVideoSinkOutput(int sinkId, SinkOutput output) {
        if (sinkId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinkIds[i] == sinkId) {
                    // Found an empty slot
                    LogManager.i("Add video sink output#" + mVideoSinkIds[i]);
                    mVideoEngineHandler.sendAddVideoSinkOutput(mVideoSinkIds[i], output);
                    return;
                }
            }
        }
        LogManager.e("addVideoSinkOutput: sink #" + sinkId + " not exists!");
    }
    
    public void removeVideoSinkOutput(int sinkId, SinkOutput output) {
        if (sinkId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinkIds[i] == sinkId) {
                    // Found an empty slot
                    LogManager.i("Remove video sink output#" + mVideoSinkIds[i]);
                    mVideoEngineHandler.sendRemoveVideoSinkOutput(mVideoSinkIds[i], output);
                    return;
                }
            }
        }
        LogManager.e("removeVideoSinkOutput: sink #" + sinkId + " not exists!");
    }
    
    public void startVideoSink(int sinkId) {
        if (sinkId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinkIds[i] == sinkId) {
                    // Found an empty slot
                    LogManager.i("Starting video sink #" + mVideoSinkIds[i]);
                    mVideoEngineHandler.sendStartVideoSink(mVideoSinkIds[i]);

                    return;
                }
            }
        }

        LogManager.e("startVideoSink: sink #" + sinkId + " not exists!");
    }

    public void stopVideoSink(int sinkId) {
        if (sinkId > 0) {
            // Search in ids array
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinkIds[i] == sinkId) {
                    // Found an empty slot
                    LogManager.i("Stopping video sink #" + mVideoSinkIds[i]);
                    mVideoEngineHandler.sendStopVideoSink(mVideoSinkIds[i]);

                    return;
                }
            }
        }

        LogManager.e("stopVideoSink: sink #" + sinkId + " not exists!");
    }

    public int addAudioStream(@NonNull AVChannel channel) {
        // Find empty cell
        for (int i = 0; i < MAXIMUM_AUDIO_STREAM_COUNT; ++i) {
            if (mAudioStreamIds[i] == 0) {
                 int streamId = getInternalObjectId();
                mAudioStreamIds[i] = streamId;
                mAudioEngine.addStream(streamId, channel);
                LogManager.i("Adding audio stream #" + streamId);
                return streamId;
            }
        }

        LogManager.e("addAudioStream: Can not add any more audio streams!");
        return -1;
    }

    public boolean removeAudioStream(int streamId) {
        if (streamId > 0) {
            synchronized(mAudioStreamIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_STREAM_COUNT; ++i) {
                    if (mAudioStreamIds[i] == streamId) {
                        LogManager.i("Removing audio stream #" + streamId);
                        mAudioEngine.removeStream(streamId);
                        mAudioStreamIds[i] = 0;
                        return true;
                    }
                }
            }
        }
        LogManager.e("removeAudioStream: stream #" + streamId + " not exists!");
        return false;
    }

    public int addAudioSink() {
        synchronized(mAudioSinkIds) {
            for (int j = 0; j < MAXIMUM_AUDIO_SINK_COUNT; ++j) {
                if (mAudioSinkIds[j] == 0) {
                    int sinkId = getInternalObjectId();
                    mAudioSinkIds[j] = sinkId;
                    mAudioEngine.addSink(sinkId);
                    LogManager.i("Adding audio sink #" + sinkId);
                    return sinkId;
                }
            }
        }
        LogManager.e("Can not add any more audio sinks!");
        return -1;
    }

    public boolean removeAudioSink(int sinkId) {
        if (sinkId > 0) {
            synchronized(mAudioSinkIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_SINK_COUNT; ++i) {
                    if (mAudioSinkIds[i] == sinkId) {
                        LogManager.i("Removing audio sink #" + sinkId);
                        mAudioEngine.removeSink(sinkId);
                        mAudioSinkIds[i] = 0;
                        return true;
                    }
                }
            }
        }
        LogManager.e("removeAudioSink: sink #" + sinkId + " not exists!");
        return false;
    }

    public boolean configureAudioSink(int sinkId, AudioSinkConfig config) {
        if (!config.isValid()) {
            LogManager.e("configureAudioSink: configuration is invalid!");
            return false;
        }

        if (sinkId > 0) {
            synchronized(mAudioSinkIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_SINK_COUNT; ++i) {
                    if (mAudioSinkIds[i] == sinkId) {
                        LogManager.i("Configuring audio sink #" + sinkId);
                        mAudioEngine.configureSink(sinkId, config);
                        return true;
                    }
                }
            }
        }
        LogManager.e("configureAudioSink: sink #" + sinkId + " not exists!");
        return false;
    }

    public boolean addAudioSinkOutput(int sinkId, SinkOutput output) {
        if (sinkId > 0) {
            synchronized(mAudioSinkIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_SINK_COUNT; ++i) {
                    if (mAudioSinkIds[i] == sinkId) {
                        LogManager.i("Add audio sink output#" + sinkId);
                        mAudioEngine.addSinkOutput(sinkId, output);
                        return true;
                    }
                }
            }
        }
        LogManager.e("addAudioSinkOutput: sink #" + sinkId + " not exists!");
        return false;
    }

    public boolean removeAudioSinkOutput(int sinkId, SinkOutput output) {
        if (sinkId > 0) {
            synchronized(mAudioSinkIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_SINK_COUNT; ++i) {
                    if (mAudioSinkIds[i] == sinkId) {
                        LogManager.i("Remove audio sink output#" + sinkId);
                        mAudioEngine.removeSinkOutput(sinkId, output);
                        return true;
                    }
                }
            }
        }
        LogManager.e("removeAudioSinkOutput: sink #" + sinkId + " not exists!");
        return false;
    }

    public boolean startAudioSink(int sinkId) {
        if (sinkId > 0) {
            synchronized(mAudioSinkIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_SINK_COUNT; ++i) {
                    if (mAudioSinkIds[i] == sinkId) {
                        LogManager.i("Starting audio sink #" + sinkId);
                        mAudioEngine.startSink(sinkId);
                        return true;
                    }
                }
            }
        }
        LogManager.e("startAudioSink: sink #" + sinkId + " not exists!");
        return false;
    }

    public boolean stopAudioSink(int sinkId) {
        if (sinkId > 0) {
            synchronized(mAudioSinkIds) {
                for (int i = 0; i < MAXIMUM_AUDIO_SINK_COUNT; ++i) {
                    if (mAudioSinkIds[i] == sinkId) {
                        LogManager.i("Stopping audio sink #" + sinkId);
                        mAudioEngine.stopSink(sinkId);
                        return true;
                    }
                }
            }
        }
        LogManager.e("stopAudioSink: sink #" + sinkId + " not exists!");
        return false;
    }

    public MediaFormat getAudioSinkBasicFormat(String mime) {
        return mAudioEngine.getSinkBasicFormat(mime);
    }

    private class VideoEngine extends Thread {
        private final static int ALIGNMENT_LEFT       = 0x0001;
        private final static int ALIGNMENT_RIGHT      = 0x0002;
        private final static int ALIGNMENT_HCENTER    = 0x0004;
        private final static int ALIGNMENT_TOP        = 0x0020;
        private final static int ALIGNMENT_BOTTOM     = 0x0040;
        private final static int ALIGNMENT_VCENTER    = 0x0080;

        private final String[] SCENE_COMMAND_REG_EXP_ARRAY = {
            // Scene
            "^set\\s(scene)\\s(background-color)\\s(#[0-9a-fA-F]{6})$",
            "^set\\s(scene)\\s(background-image)\\s(\\S+)$",
            "^set\\s(scene)\\s(display-name)\\s(\\S+)$",
            "^set\\s(scene)\\s(display-name-alignment)\\s(left|right|hcenter):" +
                "(top|bottom|vcenter)$",
            "^set\\s(scene)\\s(display-name-style-sheet)\\s(\\S+)$",
            "^set\\s(scene)\\s(display-name-visible)\\s(true|false)$",

            // Stream
            "^set\\s(stream)\\s([0-9]+)\\s(src-rect)\\s((?:0\\.[0-9]+)|(?:1\\.0+)):" +
                "((?:0\\.[0-9]+)|(?:1\\.0+)):((?:0\\.[0-9]+)|(?:1\\.0+)):((?:0\\.[0-9]+)|(?:1\\.0+))$",
            "^set\\s(stream)\\s([0-9]+)\\s(dst-rect)\\s((?:0\\.[0-9]+)|(?:1\\.0+)):" +
                "((?:0\\.[0-9]+)|(?:1\\.0+)):((?:0\\.[0-9]+)|(?:1\\.0+)):((?:0\\.[0-9]+)|(?:1\\.0+))$",
            "^set\\s(stream)\\s([0-9]+)\\s(z-index)\\s([0-9]{1,4})$",
            "^set\\s(stream)\\s([0-9]+)\\s(opacity)\\s((?:0\\.[0-9]+)|(?:1\\.0+))$",
            "^set\\s(stream)\\s([0-9]+)\\s(visible)\\s(true|false)$",
        };

        Pattern[] mSceneCommandPatternArray = new Pattern[SCENE_COMMAND_REG_EXP_ARRAY.length];

        private volatile VideoEngineHandler mVideoEngineHandler;

        private Object mStartLock = new Object();
        private boolean mReady = false;

        private EglCore mEglCore;
        //private Surface mStreamSurfaces[] = new Surface[MAXIMUM_VIDEO_STREAM_COUNT];

        private VideoStream mVideoStreams[] = new VideoStream[MAXIMUM_VIDEO_STREAM_COUNT];
        private VideoScene mVideoScenes[] = new VideoScene[MAXIMUM_VIDEO_SCENE_COUNT];
        private VideoSink mVideoSinks[] = new VideoSink[MAXIMUM_VIDEO_SINK_COUNT];

        // FPS / drop counter.
        private long mRefreshPeriodNanos = VIDEO_RENDER_INTERVAL_NS; // TODO:
        private long mFpsCountStartNanos;
        private int mFpsCountFrame;
        private int mDroppedFrames;
        private boolean mPreviousWasDropped;
        private boolean mRecordedPrevious;

        private CameraManager mCameraMgr = null;

        public VideoEngine() {
            // Compile scene command regular expressions
            for (int i = 0; i < SCENE_COMMAND_REG_EXP_ARRAY.length; ++i) {
                mSceneCommandPatternArray[i] = Pattern.compile(SCENE_COMMAND_REG_EXP_ARRAY[i]);
            }

            // Enumerate local cameras and HDMI-IN devices
            mCameraMgr = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
            /*
            try {
                String[] cameraIds = mCameraMgr.getCameraIdList();
                LogManager.i("Got camera id list length = " + cameraIds.length);
                for (int i = 0; i < cameraIds.length; ++i) {
                    LogManager.i("Camera Id: " + cameraIds[i]);
                    CameraCharacteristics camCaps = mCameraMgr.getCameraCharacteristics(cameraIds[i]);
                    Size pixSize = camCaps.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    LogManager.i("  Sensor pixel array size: " + pixSize);
                }
            } catch (CameraAccessException e) {
                LogManager.e(e.toString());
            }
            //*/
        }

        @Override
        public void run() {
            // Create looper
            Looper.prepare();

            // Create handler
            mVideoEngineHandler = new VideoEngineHandler(this);

            // Prepare EGL and OpenGL ES
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

            // Notify
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();    // signal waitUntilReady()
            }

            // Enter message looper
            Looper.loop();

            LogManager.d("Render thread looper exited!");
            releaseGl();
            mEglCore.release();

            synchronized (mStartLock) {
                mReady = false;
            }
        }

        public void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        public VideoEngineHandler getHandler() {
            return mVideoEngineHandler;
        }

        private void releaseGl() {
            GlUtil.checkGlError("releaseGl start");

            // TODO:

            mEglCore.makeNothingCurrent();
        }

        private void renderFrame(long timeStampNanos) {

            // long curMS = System.currentTimeMillis();

            // TODO:
            long diff = System.nanoTime() - timeStampNanos;
            long max = mRefreshPeriodNanos - 2000000;   // if we're within 2ms, don't bother
            if (diff > max) {
                // Too much, drop a frame
                //LogManager.d("diff is " + (diff / 1000000.0) + " ms, max " + (max / 1000000.0) +
                //        ", skipping render");
                mRecordedPrevious = false;
                mPreviousWasDropped = true;
                mDroppedFrames++;
                return;
            }

            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null) {
                    mVideoScenes[i].update(timeStampNanos);
                    mVideoScenes[i].draw(timeStampNanos, VIDEO_ENCODING_DROPPING ? mRecordedPrevious : true);
                }
            }

            mRecordedPrevious = !mRecordedPrevious;
            mPreviousWasDropped = false;

            // Update the FPS counter.
            //
            // Ideally we'd generate something approximate quickly to make the UI look
            // reasonable, then ease into longer sampling periods.
            final int NUM_FRAMES = 120;
            final long ONE_TRILLION = 1000000000000L;
            if (mFpsCountStartNanos == 0) {
                mFpsCountStartNanos = timeStampNanos;
                mFpsCountFrame = 0;
            } else {
                mFpsCountFrame++;
                if (mFpsCountFrame == NUM_FRAMES) {
                    // compute thousands of frames per second
                    long elapsed = timeStampNanos - mFpsCountStartNanos;
                    float fps = (NUM_FRAMES * ONE_TRILLION / elapsed) / 1000.f;
                    // LogManager.i("FPS: " + fps + ", Dropped: " + mDroppedFrames);

                    // Invoke callback
                    MediaEngine.Callback cb = MediaEngine.getInstance().getCallback();
                    if (cb != null) {
                        cb.onVideoRendererFpsUpdated(fps, mDroppedFrames);
                    }

                    // Reset fps counter
                    mFpsCountStartNanos = timeStampNanos;
                    mFpsCountFrame = 0;
                }
            }

            // LogManager.d("render-frame spent " + (System.currentTimeMillis() - curMS) + " ms");
        }

        private void notifyDisplaySurfaceChanged(int sceneId, int width, int height) {
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null && mVideoScenes[i].getId() == sceneId) {
                    mVideoScenes[i].onDisplaySurfaceChanged(width, height);
                    return;
                }
            }
        }

        private void switchTestingItems(int sceneId, boolean able) {
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null && mVideoScenes[i].getId() == sceneId) {
                    mVideoScenes[i].onSwitchTestingItems(able);
                    return;
                }
            }
        }

        private void addStream(int streamId, String url) {
            // Find empty slot for new stream
            for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                if (mVideoStreams[i] == null) {
                    mVideoStreams[i] = new VideoStream(streamId, url);

                    // Add to existing scenes
                    for (int j = 0; j < MAXIMUM_VIDEO_SCENE_COUNT; ++j) {
                        if (mVideoScenes[j] != null) {
                            mVideoScenes[j].addStream(mVideoStreams[i]);
                        }
                    }

                    return;
                }
            }

            LogManager.e("Can not add any more video stream!");
            // TODO: Notify adding video stream failed!
        }

        private void addStream(int streamId, AVChannel channel) {
            // Find empty slot for new stream
            for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                if (mVideoStreams[i] == null) {
                    mVideoStreams[i] = new VideoStream(streamId, channel);

                    // Add to existing scenes
                    for (int j = 0; j < MAXIMUM_VIDEO_SCENE_COUNT; ++j) {
                        if (mVideoScenes[j] != null) {
                            mVideoScenes[j].addStream(mVideoStreams[i]);
                        }
                    }

                    return;
                }
            }

            LogManager.e("Can not add any more video stream!");
            // TODO: Notify adding video stream failed!
        }

        private void addStream(int streamId, int cameraId) {
            // Find empty slot for new stream
            for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                if (mVideoStreams[i] == null) {
                    mVideoStreams[i] = new VideoStream(streamId, cameraId);

                    // Add to existing scenes
                    for (int j = 0; j < MAXIMUM_VIDEO_SCENE_COUNT; ++j) {
                        if (mVideoScenes[j] != null) {
                            mVideoScenes[j].addStream(mVideoStreams[i]);
                        }
                    }

                    return;
                }
            }

            LogManager.e("Can not add any more video stream!");
            // TODO: Notify adding video stream failed!
        }

        private void removeStream(int streamId) {
            // Remove stream from all scenes
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null) {
                    mVideoScenes[i].removeStream(streamId);
                }
            }

            // Release stream object
            for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                if (mVideoStreams[i] != null && mVideoStreams[i].getId() == streamId) {
                    mVideoStreams[i].release();
                    mVideoStreams[i] = null;
                }
            }

            // TODO: Notify video stream removed successfully.
        }

        private void addScene(int sceneId) {
            // Off-screen only
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] == null) {
                    mVideoScenes[i] = new VideoScene(sceneId);
                    for (int j = 0; j < MAXIMUM_VIDEO_STREAM_COUNT; ++j) {
                        if (mVideoStreams[j] != null) {
                            mVideoScenes[i].addStream(mVideoStreams[j]);
                        }
                    }
                    return;
                }
            }

            LogManager.e("Can not add any more video scene!");
            // TODO: Notify adding video scene failed!
        }

        private void addScene(int sceneId, Surface surface) {
            // On-screen and off-screen
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] == null) {
                    mVideoScenes[i] = new VideoScene(sceneId, surface);
                    for (int j = 0; j < MAXIMUM_VIDEO_STREAM_COUNT; ++j) {
                        if (mVideoStreams[j] != null) {
                            mVideoScenes[i].addStream(mVideoStreams[j]);
                        }
                    }
                    return;
                }
            }

            LogManager.e("Can not add any more video scene!");
            // TODO: Notify adding video scene failed!
        }

        private void removeScene(int sceneId) {
            // TODO:
        }

        private void postSceneCommands(int sceneId, String[] commands) {
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null && mVideoScenes[i].getId() == sceneId) {
                    mVideoScenes[i].processCommands(commands);
                    return;
                }
            }

            LogManager.e("Video scene #" + sceneId + " not exist!");
            // TODO: Notify posting video scene commands failed!
        }

        private void addSink(int sceneId, int sinkId) {
            // Find video scene
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null && mVideoScenes[i].getId() == sceneId) {
                    // Find empty cell
                    for (int j = 0; j < MAXIMUM_VIDEO_SINK_COUNT; ++j) {
                        if (mVideoSinks[j] == null) {
                            // Allocate sink object
                            mVideoSinks[j] = new VideoSink(sinkId, sceneId);
                            // Adding to scene
                            mVideoScenes[i].addSink(mVideoSinks[j]);
                            return;
                        }
                    }

                    LogManager.e("Can not add any more video sinks!");
                    return;
                }
            }

            LogManager.e("Video scene #" + sceneId + " not exists!");
            return;
        }

        private void removeSink(int sinkId) {
            // Remove sink from scenes
            boolean removed = false;
            for (int i = 0; i < MAXIMUM_VIDEO_SCENE_COUNT; ++i) {
                if (mVideoScenes[i] != null) {
                    removed = mVideoScenes[i].removeSink(sinkId);
                    if(removed)
                        break;
                }
            }
            if(!removed) {
                LogManager.i("Sink #" + sinkId + " not found in any video scene");
            }

            // Release sink object
            for (int j = 0; j < MAXIMUM_VIDEO_SINK_COUNT; ++j) {
                if (mVideoSinks[j] != null && mVideoSinks[j].getId() == sinkId) {
                    mVideoSinks[j].release();
                    mVideoSinks[j] = null;
                    break;
                }
            }

            // TODO: Notify video sink removed successfully.
        }

        private void configureSink(int sinkId, VideoSinkConfig config) {
            // Find video sink
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinks[i] != null && mVideoSinks[i].getId() == sinkId) {
                    // Allocate sink object
                    mVideoSinks[i].configure(config);

                    int sceneId = mVideoSinks[i].getSceneId();
                    // Notify scene update sink configuration
                    for (int j = 0; j < MAXIMUM_VIDEO_SCENE_COUNT; ++j) {
                        if (mVideoScenes[j] != null && mVideoScenes[j].getId() == sceneId) {
                            mVideoScenes[j].updateSinkConfig(sinkId);

                            return;
                        }
                    }

                    LogManager.e("Configure video sink failed!");
                }
            }
        }
        
        private void addSinkOutput(int sinkId, SinkOutput output) {
            // Find video sink
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinks[i] != null && mVideoSinks[i].getId() == sinkId) {
                    mVideoSinks[i].addOutput(output);
                }
            }
        }
        
        private void removeSinkOutput(int sinkId, SinkOutput output) {
            // Find video sink
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinks[i] != null && mVideoSinks[i].getId() == sinkId) {
                    mVideoSinks[i].removeOutput(output);
                }
            }
        }

        private void startSink(int sinkId) {
            // Find video sink
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinks[i] != null && mVideoSinks[i].getId() == sinkId) {
                    mVideoSinks[i].start();
                    return;
                }
            }

            LogManager.e("startSink: sink #" + sinkId + " not exists!");
            // TODO: Notify start video sink failed
        }

        private void stopSink(int sinkId) {
            // Find video sink
            for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                if (mVideoSinks[i] != null && mVideoSinks[i].getId() == sinkId) {
                    mVideoSinks[i].stop();
                    return;
                }
            }

            LogManager.e("stopSink: sink #" + sinkId + " not exists!");
            // TODO: Notify stop video sink failed
        }

        private Bitmap renderTextAsBitmap(String text, float textSize, int textColor) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(textSize);
            paint.setColor(textColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
            paint.setTextAlign(Paint.Align.LEFT);

            float baseline = -paint.ascent(); // ascent() is negative
            int width = (int) (paint.measureText(text) + 0.5f); // round
            int height = (int) (baseline + paint.descent() + 0.5f);

            Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            image.eraseColor(Color.TRANSPARENT);

            // Draw text
            Canvas canvas = new Canvas(image);
            canvas.drawText(text, 0, baseline, paint);

            return image;
        }

        private class VideoStream implements Runnable {
            public static final int CAMERA_TEXTURE_WIDTH = 1920;
            public static final int CAMERA_TEXTURE_HEIGHT = 1080;

            private int mId;

            private String mUrl;

            private MediaCodec mMediaCodec;
            private AVChannel mAVChannel;

            private int mCameraId = -1;

            private Semaphore mCameraOpenCloseLock = new Semaphore(1);
            private CameraDevice mCameraDevice;
            private final CameraDevice.StateCallback mCameraStateCallback
                    = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    // This method is called when the camera is opened.
                    LogManager.d("Camera StateCallback onOpened!");
                    mCameraOpenCloseLock.release();
                    mCameraDevice = cameraDevice;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    mCameraDevice = null;
                }
            };

            private CaptureRequest.Builder mCameraPreviewRequestBuilder;
            private CaptureRequest mCameraPreviewRequest;
            private CameraCaptureSession mCameraCaptureSession;

            private HandlerThread mCameraBackgroundThread;
            private Handler mCameraBackgroundHandler;

            private Texture2dProgram mTextureProgram;
            private ScaledDrawable2d mRectDrawable;
            private Sprite2d mRect;

            // Orthographic projection matrix.
            private float[] mDisplayProjectionMatrix = new float[16];

            private SurfaceTexture mOutputSurfaceTexture;
            private Object mUpdateSurfaceLock = new Object();
            private boolean mUpdateSurface;

            private Thread mDecodeThread = null;
            private Object mStopFence = new Object();
            private volatile boolean mStopFlag;

            public VideoStream(int id, String url) {
                // TODO: Validate url
                mId = id;
                mUrl = url;

                createOutputSurface();

                mDecodeThread = new Thread(this, "VideoStream#" + mId);
                mDecodeThread.start();
            }

            public VideoStream(int id, AVChannel channel) {
                mId = id;
                mAVChannel = channel;

                createOutputSurface();

                mDecodeThread = new Thread(this, "VideoStream#" + mId);
                mDecodeThread.start();
            }

            public VideoStream(int id, int cameraId) {
                LogManager.i("Construct VideoStream#" + id + " with cameraId=" + cameraId);

                mId = id;
                mCameraId = cameraId;

                createOutputSurface();

                // If the size is not set by the application, it will be set to be the
                // smallest supported size less than 1080p, by the camera device.
                mOutputSurfaceTexture.setDefaultBufferSize(CAMERA_TEXTURE_WIDTH,
                                                           CAMERA_TEXTURE_HEIGHT);

                // Prepare background thread and handler for repeating capture request.
                mCameraBackgroundThread = new HandlerThread("CameraBackground");
                mCameraBackgroundThread.start();
                mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());

                try {
                    if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }

                    mCameraMgr.openCamera(String.valueOf(cameraId), mCameraStateCallback, null);
                } catch (CameraAccessException e) {
                   LogManager.e(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                } catch (SecurityException e) {
                   LogManager.e(e);
                } catch (IllegalArgumentException e) {
                   LogManager.e(e);
                }
            }

            private void createCameraPreviewSession() {
                try {
                    // This is the output Surface we need to start preview.
                    Surface surface = new Surface(mOutputSurfaceTexture);

                    // We set up a CaptureRequest.Builder with the output Surface.
                    mCameraPreviewRequestBuilder
                            = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mCameraPreviewRequestBuilder.addTarget(surface);

                    // Here, we create a CameraCaptureSession for camera preview.
                    mCameraDevice.createCaptureSession(
                        Arrays.asList(surface),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    LogManager.e("The camera is already closed");
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                mCameraCaptureSession = cameraCaptureSession;
                                try {
                                    // Auto focus should be continuous for camera preview.
                                    mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                    // Finally, we start displaying the camera preview.
                                    mCameraPreviewRequest = mCameraPreviewRequestBuilder.build();
                                    mCameraCaptureSession.setRepeatingRequest(
                                            mCameraPreviewRequest, null, mCameraBackgroundHandler);
                                } catch (CameraAccessException e) {
                                   LogManager.e(e);
                                }
                            }

                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                LogManager.e("CameraCaptureSession onConfigureFailed!");
                            }
                        }, null
                    );
                } catch (CameraAccessException e) {
                   LogManager.e(e);
                }
            }

            private void createOutputSurface() {
                mTextureProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
                int textureId = mTextureProgram.createTextureObject();
                mOutputSurfaceTexture = new SurfaceTexture(textureId);
                mOutputSurfaceTexture.setOnFrameAvailableListener(
                        new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                //LogManager.d("On decoded video frame available!");
                                synchronized (mUpdateSurfaceLock) {
                                    mUpdateSurface = true;
                                }
                            }
                        }
                );

                mRectDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
                mRect = new Sprite2d(mRectDrawable);
                mRect.setTexture(textureId);

                // Orthographic Projection
                Matrix.orthoM(mDisplayProjectionMatrix, 0, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f);
            }

            public int getId() {
                return mId;
            }

            public void release() {
                // Stop decoder thread
                synchronized (mStopFence) {
                    mStopFlag = true;
                }

                // Stop camera background thread
                if (mCameraBackgroundThread != null) {
                    mCameraBackgroundThread.quitSafely();
                    try {
                        mCameraBackgroundThread.join();
                        mCameraBackgroundThread = null;
                        mCameraBackgroundHandler = null;
                    } catch (InterruptedException e) {
                       LogManager.e(e);
                    }
                }
            }

            public void draw(RectF srcRect, RectF dstRect) {
                // Update texture image
                updateTexture();

                // Update texture coordinates by srcRect
                float textureCoordArray[] = {
                    srcRect.left, srcRect.bottom,   // 0 bottom left
                    srcRect.right, srcRect.bottom,  // 1 bottom right
                    srcRect.left, srcRect.top,      // 2 top left
                    srcRect.right, srcRect.top      // 3 top right
                };

                FloatBuffer fb = GlUtil.createFloatBuffer(textureCoordArray);

                // Translate canvas coordinates to OpenGL coordinates
                float posX = dstRect.left;
                float posY = 1.0f - dstRect.bottom;

                mRect.setPosition(posX, posY);
                mRect.setRotation(0.0f);
                mRect.setScale(dstRect.width(), dstRect.height());
                mRect.draw(mTextureProgram, mDisplayProjectionMatrix, fb);
            }

            public void run() {
                LogManager.i("Video stream #" + mId + " thread started!");

                MediaFormat videoFormat;
                String videoMime;

                if (mUrl != null) {
                    // TODO: Unimplemented!
                } else if (mAVChannel != null) {
                    videoFormat = mAVChannel.getMediaFormat();
                    videoMime = videoFormat.getString(MediaFormat.KEY_MIME);

                    try {
                        mMediaCodec = MediaCodec.createDecoderByType(videoMime);
                        //mMediaCodec = MediaCodec.createByCodecName("OMX.google.h264.decoder");
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Create decoder for MIME type " + videoMime + " failed!");
                    }

                    // Configure media codec and start it!
                    mMediaCodec.configure(videoFormat, new Surface(mOutputSurfaceTexture), null, 0);
                    mMediaCodec.start();

                    final int TIMEOUT_USEC = 10000; // 10ms
                    final int TIMEOUT_MS = 10; // 10ms
                    ByteBuffer[] decoderInputBuffers = mMediaCodec.getInputBuffers();
                    MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                    int inputChunk = 0;
                    long firstInputTimeNsec = -1;

                    final int NUM_FRAMES = 120;
                    long periodStartTimeNS = 0;
                    int periodFrameCount = 0;
                    int periodFrameBytes = 0;

                    //boolean loopPlay = true;
                    boolean outputDone = false;
                    boolean inputDone = false;
                    AVPacket packet = null;
                    LogManager.d("Enter video decoding loop.");
                    while (!outputDone) {
                        // Check stop flag
                        if (mStopFlag) {
                            LogManager.i("Video stream #" + mId + " thread exited!");

                            // Stop and release codec
                            mMediaCodec.stop();
                            mMediaCodec.release();
                            mMediaCodec = null;

                            // Quit thread
                            return;
                        }

                        // Feed more data to the decoder.
                        if (!inputDone) {
                            do {
                                if(packet == null) {
                                    try {
                                        packet = mAVChannel.pollBusyPacket(TIMEOUT_MS);
                                    } catch (InterruptedException e) {
                                        LogManager.w("Poll on AVChannel was interrupted!");
                                        mStopFlag = true;
                                        continue;
                                    }
                                    if(packet == null) {
                                        // no encoded frame in channel
                                        // goto `check decoded frame in MC`
                                        break; 
                                    }
                                }

                                int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                                if(inputBufIndex < 0) {
                                    LogManager.i("decoder input buffer is full, try later. stream#" + mId);
                                    // decoder has no enough buffer for this encoded frame
                                    // goto `check decoded frame in MC`
                                    break;
                                }
                                
                                if(packet.isEmpty()) {
                                    // End of stream -- send empty frame with EOS flag set.
                                    mMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                    LogManager.d("Sent input EOS");
                                    packet.reset();
                                    mAVChannel.putIdlePacket(packet);
                                    packet = null;
                                    break;
                                } else {
                                    long pktPts = packet.getPts();
                                    int pktLen = packet.getPayload().limit();
                                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                                    try {
                                        // Read the sample data into the ByteBuffer.
                                        // This neither respects nor
                                        // updates inputBuf's position, limit, etc.
                                        inputBuf.clear();
                                        inputBuf.put(packet.getPayload());
                                        inputBuf.flip();
                                    } catch (BufferOverflowException e) { 
                                        LogManager.w("this ByteBuffer in MediaCodec is not enough for the frame[" + pktLen + "]");
                                    }
                                    
                                    packet.reset();
                                    mAVChannel.putIdlePacket(packet);
                                    packet = null;
                                    
                                    if (firstInputTimeNsec == -1) {
                                        firstInputTimeNsec = System.nanoTime();
                                    }

                                    mMediaCodec.queueInputBuffer(inputBufIndex, 0, pktLen, pktPts, 0/*flags*/);
                                    //LogManager.d("Put " + new_datas.length + "bytes to MC, totally " + frame_cnt + "frames");
                                    periodFrameBytes += pktLen;
                                }
                            } while(false);
                        }

                        while (!outputDone) { //get-out all decoded frames from MC
                            
                            int decoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                // no output available yet
                                //LogManager.d("No output from decoder available");
                                break;
                            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                // not important for us, since we're using Surface
                                LogManager.d("Decoder output buffers changed");
                            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                                if(mCallback != null) {
                                    int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
                                    int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
                                    mCallback.onVideoStreamResolutionChanged(mId, width, height, -1, -1);
                                }
                                LogManager.d("Decoder output format changed: " + newFormat);
                            } else if (decoderStatus < 0) {
                                throw new RuntimeException(
                                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                                decoderStatus);
                            } else { // decoderStatus >= 0
                                if (firstInputTimeNsec != 0) {
                                    // Log the delay from the first buffer of input to the first buffer
                                    // of output.
                                    long nowNsec = System.nanoTime();
                                    LogManager.d("Startup lag "
                                            + ((nowNsec-firstInputTimeNsec) / 1000000.0) + " ms");
                                    firstInputTimeNsec = 0;
                                }
                                //LogManager.d("Surface decoder given buffer " + decoderStatus +
                                //       " (size=" + mBufferInfo.size + ")");
                                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    LogManager.d("Output EOS");
                                    outputDone = true;
                                }

                                boolean doRender = (mBufferInfo.size != 0);
                                mMediaCodec.releaseOutputBuffer(decoderStatus, doRender);

                                // Update the Statis.
                                long timeNowNS = System.nanoTime();
                                if(periodStartTimeNS == 0) {
                                    periodFrameCount = 0;
                                    periodFrameBytes = 0;
                                    periodStartTimeNS = timeNowNS;
                                } else {
                                    ++periodFrameCount;
                                    if(periodFrameCount == NUM_FRAMES) {
                                        float elapsedS = (timeNowNS - periodStartTimeNS) / 1000000000.f;
                                        float fps = NUM_FRAMES / elapsedS;
                                        float kbps = periodFrameBytes / elapsedS * 8.f / 1000;

                                        // Invoke callback
                                        MediaEngine.Callback cb = MediaEngine.getInstance().getCallback();
                                        if (cb != null)
                                            cb.onVideoStreamStatistics(mId, fps, (int)kbps);

                                        // Reset fps counter
                                        periodFrameCount = 0;
                                        periodFrameBytes = 0;
                                        periodStartTimeNS = timeNowNS;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("No source available!");
                }
            }

            private void updateTexture() {
                synchronized (mUpdateSurfaceLock) {
                    if (mUpdateSurface) {
                        mOutputSurfaceTexture.updateTexImage();
                        mUpdateSurface = false;
                    }
                }
            }
        }

        private class VideoScene {
            public static final int OFFSCREEN_TEXTURE_WIDTH = 1920;
            public static final int OFFSCREEN_TEXTURE_HEIGHT = 1080;

            private int mId;

            private Surface mDisplaySurface;
            private WindowSurface mWindowSurface;
            private FullFrameRect mFullScreen;
            private int mDisplaySurfaceWidth;
            private int mDisplaySurfaceHeight;

            // Used for off-screen rendering.
            private OffscreenSurface mOffscreenSurface;
            private int mOffscreenTexture;
            private int mFramebuffer;
            private int mDepthBuffer;

            // Orthographic projection matrix.
            private float[] mDisplayProjectionMatrix = new float[16];

            // Flat shaded program
            private FlatShadedProgram mProgram;

            // RGB Texture program - background, display name and marquee...
            private Texture2dProgram mBgImgTexPgm;
            private int mBgImgTexId;
            private Sprite2d mBgImgRect;

            private Texture2dProgram mDisplayNameTexPgm;
            private int mDisplayNameTexId;
            private Sprite2d mDisplayNameRect;

            private final Drawable2d mTriDrawable = new Drawable2d(Drawable2d.Prefab.TRIANGLE);
            private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);

            // One spinning triangle and one bouncing rectangle
            private Sprite2d mTri;
            private Sprite2d mRect;
            private float mRectVelX, mRectVelY;     // velocity, in viewport units per second

            private final float[] mIdentityMatrix = new float[16];

            private long mPrevTimeNanos;

            private int mBackgroundColor;
            private Bitmap mBackgroundImage;

            private String mDisplayName;
            private int mDisplayNameAlignment;
            private Map<String, String> mDisplayNameStyle;
            private boolean mDisplayNameVisible;
            private boolean mNeedUpdateDisplayNameBitmap;
            private Bitmap mDisplayNameImage;
            private ByteBuffer mDisplayNameImageBuffer;

            private boolean mEnableTestingItems = false;

            private class Stream {
                private WeakReference<VideoStream> mWeakRefVideoStream;

                private RectF mSrcRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
                private RectF mDstRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);

                private float mOpacity;
                private boolean mVisible;
                private int mZIndex;

                private String mDisplayName;
                private int mDisplayNameAlignment;
                private Map<String, String> mDisplayNameStyle;
                private boolean mDisplayNameVisible;

                private Stream(VideoStream videoStream) {
                    mWeakRefVideoStream = new WeakReference<VideoStream>(videoStream);
                }

                private void draw() {
                    VideoStream videoStream = mWeakRefVideoStream.get();
                    if (videoStream != null) {
                        videoStream.draw(mSrcRect, mDstRect);
                    }
                }

                private int getId() {
                    return mWeakRefVideoStream.get().mId;
                }

                private void setSrcRect(String left, String top, String width, String height) {
                    float x = Float.valueOf(left);
                    float y = Float.valueOf(top);
                    float w = Float.valueOf(width);
                    float h = Float.valueOf(height);

                    mSrcRect.set(x, y, x + w, y + h);
                }

                private void setDstRect(String left, String top, String width, String height) {
                    float x = Float.valueOf(left);
                    float y = Float.valueOf(top);
                    float w = Float.valueOf(width);
                    float h = Float.valueOf(height);

                    mDstRect.set(x, y, x + w, y + h);
                }

                private void setZIndex(String zIndex) {
                    mZIndex = Integer.valueOf(zIndex, 10);
                }

                private void setOpacity(String opacity) {
                    mOpacity = Integer.valueOf(opacity, 10);
                }

                private void setVisible(String visible) {
                    mVisible = visible.equals("true");
                }
            }

            private class StreamZIndexComparator implements Comparator<Stream> {
                public int compare(Stream a, Stream b) {
                    if (a == null && b == null) {
                        return 0;
                    }

                    if (a == null) {
                        return -1;
                    }

                    if (b == null) {
                        return 1;
                    }

                    return a.mZIndex - b.mZIndex;
                }
            }

            private class Sink {
                private int mId;

                private WeakReference<VideoSink> mWeakRefVideoSink;
                private WindowSurface mInputWindowSurface;
                private int mWidth;
                private int mHeight;

                public Sink(VideoSink videoSink) {
                    mWeakRefVideoSink = new WeakReference<>(videoSink);

                    mId = videoSink.getId();
                }

                public int getId() {
                    return mId;
                }

                public void updateConfig() {
                    VideoSink videoSink = mWeakRefVideoSink.get();

                    if (videoSink != null) {
                        VideoSinkConfig config = videoSink.getConfig();

                        // Update width and height
                        mWidth = config.width;
                        mHeight = config.height;

                        // Create input window surface
                        Surface inputSurface = videoSink.getInputSurface();
                        if (inputSurface != null) {
                            mInputWindowSurface = new WindowSurface(mEglCore, inputSurface, true);
                        } else {
                            LogManager.e("Video sink input surface is null!");
                        }
                    }
                }

                public boolean isRunning() {
                    VideoSink videoSink = mWeakRefVideoSink.get();
                    if (videoSink != null && videoSink.getStatus() == VideoSink.STATUS_RUNNING) {
                        return true;
                    }

                    return false;
                }

                public void beginRecord() {
                    //LogManager.d("beginRecord: sink #" + mId);
                    VideoSink videoSink = mWeakRefVideoSink.get();
                    if (videoSink != null) {
                        videoSink.frameAvailableSoon();
                        mInputWindowSurface.makeCurrent();

                        GLES20.glViewport(0, 0, mWidth, mHeight);
                    }
                }

                public void finishRecord(long timeStampNanos) {
                    //LogManager.d("finishRecord: sink #" + mId);
                    mInputWindowSurface.setPresentationTime(timeStampNanos);
                    mInputWindowSurface.swapBuffers();
                }
            }

            private Stream[] mStreams = new Stream[MAXIMUM_VIDEO_STREAM_COUNT];
            private Sink[] mSinks = new Sink[MAXIMUM_VIDEO_SINK_COUNT];

            private VideoScene(int id) {
                // TODO:
                mId = id;

                prepareGL();
            }

            private VideoScene(int id, Surface displaySurface) {
                // TODO:
                mId = id;
                mDisplaySurface = displaySurface;

                prepareGL();

                // TEST
                //setBackgroundImage("/storage/emulated/0/Pictures/壁纸/ByFedericoBottos.jpg");
                //setDisplayNameVisible("true");
                //setDisplayName("小学一年级数学——于老师");
                //setDisplayNameAlignment("hcenter", "top");
            }

            private int getId() {
                return mId;
            }

            private void onDisplaySurfaceChanged(int width, int height) {
                // TODO:
                LogManager.d("Video scene #" + mId + " display surface changed "
                        + width + "x" + height);

                mDisplaySurfaceWidth = width;
                mDisplaySurfaceHeight = height;
            }

            private void onSwitchTestingItems(boolean able) {
                mEnableTestingItems = able;
            }

            private void processCommands(String[] commands) {
                // TODO:
                for (String cmd : commands) {
                    for (Pattern pattern : mSceneCommandPatternArray) {
                        Matcher matcher = pattern.matcher(cmd);
                        if (!matcher.matches()) {
                            continue;
                        }

                        String target = matcher.group(1);
                        if (target.equals("scene")) {
                            String property = matcher.group(2);
                            switch (property) {
                                case "background-color" :
                                    setBackgroundColor(matcher.group(3));
                                    break;

                                case "background-image" :
                                    setBackgroundImage(matcher.group(3));
                                    break;

                                case "display-name" :
                                    setDisplayName(matcher.group(3));
                                    break;

                                case "display-name-alignment" :
                                    setDisplayNameAlignment(matcher.group(3), matcher.group(4));
                                    break;

                                case "display-name-style-sheet" :
                                    setDisplayNameStyleSheet(matcher.group(3));
                                    break;

                                case "display-name-visible" :
                                    setDisplayNameVisible(matcher.group(3));
                                    break;

                                default :
                                    LogManager.w("Unimplement feature!");
                                    break;
                            }
                        } else if (target.equals("stream")) {
                            int streamId = Integer.valueOf(matcher.group(2), 10);
                            Stream stream = getStream(streamId);
                            if (stream == null) {
                                LogManager.w("Stream #" + streamId + " not exist!");
                                continue;
                            }

                            String property = matcher.group(3);
                            switch (property) {
                                case "src-rect" :
                                    stream.setSrcRect(matcher.group(4), matcher.group(5),
                                            matcher.group(6), matcher.group(7));
                                    break;

                                case "dst-rect" :
                                    stream.setDstRect(matcher.group(4), matcher.group(5),
                                            matcher.group(6), matcher.group(7));
                                    break;

                                case "z-index" :
                                    stream.setZIndex(matcher.group(4));
                                    // Re-sort streams by z index
                                    Arrays.sort(mStreams, new StreamZIndexComparator());
                                    break;

                                case "opacity" :
                                    stream.setOpacity(matcher.group(4));
                                    break;

                                case "visible" :
                                    stream.setVisible(matcher.group(4));
                                    break;

                                default :
                                    LogManager.w("Unimplement feature!");
                                    break;
                            }
                        }
                    }
                }
            }

            private void addStream(VideoStream videoStream) {
                for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                    if (mStreams[i] == null) {
                        LogManager.i("Added stream #" + videoStream.mId + " to scene #" + mId);
                        mStreams[i] = new Stream(videoStream);

                        // Sort by z-index
                        Arrays.sort(mStreams, new StreamZIndexComparator());
                        return;
                    }
                }

                LogManager.e("Add stream to video scene failed!");
            }

            private void removeStream(int streamId) {
                for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                    if (mStreams[i] != null && mStreams[i].getId() == streamId) {
                        LogManager.i("Removed stream #" + streamId + " from scene #" + mId);
                        mStreams[i] = null;

                        // Sort by z-index
                        Arrays.sort(mStreams, new StreamZIndexComparator());
                        return;
                    }
                }

                LogManager.e("Stream #" + streamId + " not found in video scene #" + mId);
            }

            private void addSink(VideoSink videoSink) {
                for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                    if (mSinks[i] == null) {
                        LogManager.i("Added sink #" + videoSink.mId + " to scene #" + mId);
                        mSinks[i] = new Sink(videoSink);
                        return;
                    }
                }

                LogManager.e("Add sink to video scene failed!");
            }

            private boolean removeSink(int sinkId) {
                for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                    if (mSinks[i] != null && mSinks[i].getId() == sinkId) {
                        LogManager.i("Removed sink #" + sinkId + " from scene #" + mId);
                        mSinks[i] = null;
                        return true;
                    }
                }

                LogManager.i("Sink #" + sinkId + " not found in video scene #" + mId);
                return false;
            }

            private void updateSinkConfig(int sinkId) {
                // Update sink configuration
                for (int i = 0; i < MAXIMUM_VIDEO_SINK_COUNT; ++i) {
                    if (mSinks[i] != null && mSinks[i].getId() == sinkId) {
                        mSinks[i].updateConfig();
                        return;
                    }
                }

                LogManager.e("updateSinkConfig: sink #" + sinkId + " not exists!");
            }

            private void prepareGL() {
                LogManager.d("Prepare Gl for video scene");

                mOffscreenSurface = new OffscreenSurface(mEglCore, OFFSCREEN_TEXTURE_WIDTH,
                        OFFSCREEN_TEXTURE_HEIGHT);
                mOffscreenSurface.makeCurrent();

                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                // Set the background color.
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

                // Disable depth testing -- we're 2D only.
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);

                // Don't need backface culling.
                // (If you're feeling pedantic, you can turn it on to make sure we're
                // defining our shapes correctly.)
                GLES20.glDisable(GLES20.GL_CULL_FACE);

                // Prepare frame buffer for offscreen rendering
                prepareFramebuffer(OFFSCREEN_TEXTURE_WIDTH, OFFSCREEN_TEXTURE_HEIGHT);

                // Prepare texture for background image
                prepareBgImgTexture();
                prepareDisplayNameTexture();

                // Simple orthographic projection, with (0,0) in lower-left corner.
                //Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, OFFSCREEN_TEXTURE_WIDTH,
                //        0, OFFSCREEN_TEXTURE_HEIGHT, -1, 1);
                Matrix.orthoM(mDisplayProjectionMatrix, 0, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f);

                //int smallDim = Math.min(OFFSCREEN_TEXTURE_WIDTH, OFFSCREEN_TEXTURE_HEIGHT);
                float aspect = (float)OFFSCREEN_TEXTURE_WIDTH / (float)OFFSCREEN_TEXTURE_HEIGHT;

                // Set initial shape size / position / velocity based on window size.  Movement
                // has the same "feel" on all devices, but the actual path will vary depending
                // on the screen proportions.  We do it here, rather than defining fixed values
                // and tweaking the projection matrix, so that our squares are square.
                mTri = new Sprite2d(mTriDrawable);
                mTri.setColor(0.1f, 0.9f, 0.1f);
                mTri.setScale(1.0f / 4.0f, aspect / 4.0f);
                mTri.setPosition(0.5f, 0.5f);

                mRect = new Sprite2d(mRectDrawable);
                mRect.setColor(0.9f, 0.1f, 0.1f);
                mRect.setScale(1.0f / 8.0f, aspect / 8.0f);
                mRect.setPosition(0.5f, 0.5f);
                mRectVelX = 0.25f;
                mRectVelY = 0.25f / aspect;

                // Program used for drawing testing items.
                mProgram = new FlatShadedProgram();

                Matrix.setIdentityM(mIdentityMatrix, 0);

                // Used for blitting texture to FBO.
                mFullScreen = new FullFrameRect(
                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));

                if (mDisplaySurface != null) {
                    mWindowSurface = new WindowSurface(mEglCore, mDisplaySurface, false);
                    mWindowSurface.makeCurrent();

                    // Set the background color.
                    //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

                    // Disable depth testing -- we're 2D only.
                    //GLES20.glDisable(GLES20.GL_DEPTH_TEST);

                    // Don't need backface culling.
                    // (If you're feeling pedantic, you can turn it on to make sure we're
                    // defining our shapes correctly.)
                    //GLES20.glDisable(GLES20.GL_CULL_FACE);
                }
            }

            /**
             * Prepares the off-screen framebuffer.
             */
            private void prepareFramebuffer(int width, int height) {
                GlUtil.checkGlError("prepareFramebuffer start");

                int[] values = new int[1];

                // Create a texture object and bind it.  This will be the color buffer.
                GLES20.glGenTextures(1, values, 0);
                GlUtil.checkGlError("glGenTextures");
                mOffscreenTexture = values[0];   // expected > 0
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture);
                GlUtil.checkGlError("glBindTexture " + mOffscreenTexture);

                // Create texture storage.
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                // Set parameters.  We're probably using non-power-of-two dimensions, so
                // some values may not be available for use.
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);
                GlUtil.checkGlError("glTexParameter");

                // Create framebuffer object and bind it.
                GLES20.glGenFramebuffers(1, values, 0);
                GlUtil.checkGlError("glGenFramebuffers");
                mFramebuffer = values[0];    // expected > 0
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
                GlUtil.checkGlError("glBindFramebuffer " + mFramebuffer);

                // Create a depth buffer and bind it.
                GLES20.glGenRenderbuffers(1, values, 0);
                GlUtil.checkGlError("glGenRenderbuffers");
                mDepthBuffer = values[0];    // expected > 0
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer);
                GlUtil.checkGlError("glBindRenderbuffer " + mDepthBuffer);

                // Allocate storage for the depth buffer.
                GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                        width, height);
                GlUtil.checkGlError("glRenderbufferStorage");

                // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                        GLES20.GL_RENDERBUFFER, mDepthBuffer);
                GlUtil.checkGlError("glFramebufferRenderbuffer");
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0);
                GlUtil.checkGlError("glFramebufferTexture2D");

                // See if GLES is happy with all this.
                int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
                if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                    throw new RuntimeException("Framebuffer not complete, status=" + status);
                }

                // Switch back to the default framebuffer.
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

                GlUtil.checkGlError("prepareFramebuffer done");
            }

            private void prepareBgImgTexture() {
                GlUtil.checkGlError("prepareBgImgTexture start");

                int[] values = new int[1];

                mBgImgTexPgm = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);

                // Create a texture object and bind it.
                GLES20.glGenTextures(1, values, 0);
                GlUtil.checkGlError("glGenTextures");
                mBgImgTexId = values[0];   // expected > 0
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBgImgTexId);
                GlUtil.checkGlError("glBindTexture " + mBgImgTexId);

                // Create texture storage.
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                        OFFSCREEN_TEXTURE_WIDTH, OFFSCREEN_TEXTURE_HEIGHT, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                // Set parameters.  We're probably using non-power-of-two dimensions, so
                // some values may not be available for use.
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);
                GlUtil.checkGlError("glTexParameter");

                ScaledDrawable2d rectDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
                mBgImgRect = new Sprite2d(rectDrawable);
                mBgImgRect.setTexture(mBgImgTexId);

                GlUtil.checkGlError("prepareBgImgTexture done");
            }

            private void prepareDisplayNameTexture() {
                GlUtil.checkGlError("prepareDisplayNameTexture start");

                int[] values = new int[1];

                mDisplayNameTexPgm = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);

                // Create a texture object and bind it.
                GLES20.glGenTextures(1, values, 0);
                GlUtil.checkGlError("glGenTextures");
                mDisplayNameTexId = values[0];   // expected > 0
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mDisplayNameTexId);
                GlUtil.checkGlError("glBindTexture " + mDisplayNameTexId);

                // Create texture storage.
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                        OFFSCREEN_TEXTURE_WIDTH, OFFSCREEN_TEXTURE_HEIGHT, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                // Set parameters.  We're probably using non-power-of-two dimensions, so
                // some values may not be available for use.
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);
                GlUtil.checkGlError("glTexParameter");

                ScaledDrawable2d rectDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
                mDisplayNameRect = new Sprite2d(rectDrawable);
                mDisplayNameRect.setTexture(mDisplayNameTexId);

                GlUtil.checkGlError("prepareDisplayNameTexture done");
            }

            private void draw(long timeStampNanos, boolean recordCurrentFrame) {
                // TODO:
                // LogManager.d("Rendering scene #" + mId + "ts:" + timeStampNanos);

                // Render off-screen.
                mOffscreenSurface.makeCurrent();
                GlUtil.checkGlError("draw start");

                // Update viewport.
                GLES20.glViewport(0, 0, OFFSCREEN_TEXTURE_WIDTH, OFFSCREEN_TEXTURE_HEIGHT);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
                GlUtil.checkGlError("glBindFramebuffer");

                // Draw background
                drawBackground();

                // Draw streams
                drawStreams();

                // Draw display name
                drawDisplayName();

                // Draw testing items
                if(mEnableTestingItems)
                    drawTestingItems();

                // Draw something unvisible, fix crash on RK3288
                mTri.setPosition(2.0f, 2.0f);
                mTri.draw(mProgram, mDisplayProjectionMatrix);

                // Blit to display.
                if (mDisplaySurface != null) {
                    mWindowSurface.makeCurrent();

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GlUtil.checkGlError("glBindFramebuffer");

                    // Update viewport
                    GLES20.glViewport(0, 0, mDisplaySurfaceWidth, mDisplaySurfaceHeight);

                    mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
                    //mFullscreenRect.draw(mFullscreenTexPgm, mFullscreenDisplayProjectionMatrix);

                    mWindowSurface.swapBuffers();
                }

                // Blit to encoder surface
                if (recordCurrentFrame) {
                    // long curMS = System.currentTimeMillis();
                    for (int i = 0; i < mSinks.length; ++i) {
                        if (mSinks[i] != null && mSinks[i].isRunning()) {
                            // Prepare for recording
                            mSinks[i].beginRecord();

                            mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);

                            // Finish recording
                            mSinks[i].finishRecord(timeStampNanos);
                        }
                    }
                    // LogManager.d("`Blitting frame to encoder surface` spent " + (System.currentTimeMillis() - curMS) + " ms");
                }
            }

            private void drawBackground() {
                // Clear color
                float red = Color.red(mBackgroundColor) / 255.0f;
                float green = Color.green(mBackgroundColor) / 255.0f;
                float blue = Color.blue(mBackgroundColor) / 255.0f;

                GLES20.glClearColor(red, green, blue, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Draw image
                if (mBackgroundImage != null) {
                    // Translate canvas coordinates to OpenGL coordinates
                    mBgImgRect.setPosition(0.0f, 0.0f);
                    mBgImgRect.setRotation(0.0f);
                    mBgImgRect.setScale(1.0f, 1.0f);

                    mBgImgRect.draw(mBgImgTexPgm, mDisplayProjectionMatrix);
                }
            }

            private void drawStreams() {
                for (Stream stream : mStreams) {
                    if (stream != null && stream.mVisible) {
                        stream.draw();
                    }
                }
            }

            private void drawTestingItems() {
                mTri.setPosition(0.5f, 0.5f);
                mTri.draw(mProgram, mDisplayProjectionMatrix);
                mRect.draw(mProgram, mDisplayProjectionMatrix);
            }

            private void drawDisplayName() {
                if (!mDisplayNameVisible) {
                    return;
                }

                if (mNeedUpdateDisplayNameBitmap) {
                    updateDisplayNameImageBuffer();

                    // Update texture
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mDisplayNameTexId);
                    GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                            mDisplayNameImage.getWidth(), mDisplayNameImage.getHeight(),
                            0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mDisplayNameImageBuffer);
                }

                float widthF = (float)mDisplayNameImage.getWidth() / (float)OFFSCREEN_TEXTURE_WIDTH;
                float heightF = (float)mDisplayNameImage.getHeight() / (float)OFFSCREEN_TEXTURE_HEIGHT;
                float posX = 0.0f;
                float posY = 0.0f;

                switch (mDisplayNameAlignment & 0x000F) {
                    case ALIGNMENT_LEFT :
                        posX = 0.0f;
                        break;
                    case ALIGNMENT_HCENTER :
                        posX = 0.5f - widthF / 2.0f;
                        break;
                    case ALIGNMENT_RIGHT :
                        posX = 1.0f - widthF;
                        break;
                }

                switch (mDisplayNameAlignment & 0x00F0) {
                    case ALIGNMENT_TOP :
                        posY = 1.0f - heightF;
                        break;
                    case ALIGNMENT_VCENTER :
                        posY = 0.5f - heightF / 2.0f;
                        break;
                    case ALIGNMENT_BOTTOM :
                        posY = 0.0f;
                        break;
                }

                mDisplayNameRect.setPosition(posX, posY);
                mDisplayNameRect.setRotation(0.0f);
                mDisplayNameRect.setScale(widthF, heightF);

                mDisplayNameRect.draw(mDisplayNameTexPgm, mDisplayProjectionMatrix);
            }

            private void update(long timeStampNanos) {
                // Compute time from previous frame.
                long intervalNanos;
                if (mPrevTimeNanos == 0) {
                    intervalNanos = 0;
                } else {
                    intervalNanos = timeStampNanos - mPrevTimeNanos;

                    final long ONE_SECOND_NANOS = 1000000000L;
                    if (intervalNanos > ONE_SECOND_NANOS) {
                        // A gap this big should only happen if something paused us.  We can
                        // either cap the delta at one second, or just pretend like this is
                        // the first frame and not advance at all.
                        LogManager.d("Time delta too large: " +
                                (double) intervalNanos / ONE_SECOND_NANOS + " sec");
                        intervalNanos = 0;
                    }
                }
                mPrevTimeNanos = timeStampNanos;

                final float ONE_BILLION_F = 1000000000.0f;
                final float elapsedSeconds = intervalNanos / ONE_BILLION_F;

                // Spin the triangle.  We want one full 360-degree rotation every 3 seconds,
                // or 120 degrees per second.
                final int SECS_PER_SPIN = 3;
                float angleDelta = (360.0f / SECS_PER_SPIN) * elapsedSeconds;
                mTri.setRotation(mTri.getRotation() + angleDelta);

                // Bounce the rect around the screen.  The rect is a 1x1 square scaled up to NxN.
                // We don't do fancy collision detection, so it's possible for the box to slightly
                // overlap the edges.  We draw the edges last, so it's not noticeable.
                float xpos = mRect.getPositionX();
                float ypos = mRect.getPositionY();
                float xscale = mRect.getScaleX();
                float yscale = mRect.getScaleY();

                xpos += mRectVelX * elapsedSeconds;
                ypos += mRectVelY * elapsedSeconds;
                if ((mRectVelX < 0.0f && xpos /*- xscale / 2*/ < 0.0f) ||
                        (mRectVelX > 0.0f && xpos + xscale > 1.0f)) {
                    mRectVelX = -mRectVelX;
                }
                if ((mRectVelY < 0.0f && ypos /*- yscale / 2*/ < 0.0f) ||
                        (mRectVelY > 0.0f && ypos + yscale > 1.0f)) {
                    mRectVelY = -mRectVelY;
                }
                mRect.setPosition(xpos, ypos);
            }

            private void setBackgroundColor(String color) {
                mBackgroundColor = Color.parseColor(color);
            }

            private void setBackgroundImage(String imagePath) {
                // Check is unset
                if (imagePath.equals("none")) {
                    LogManager.i("Disable background image for scene #" + mId);
                    mBackgroundImage = null;
                    return;
                }

                // Load image from file and convert into byte buffer
                LogManager.i("Set background image(" + imagePath + ") for scene #" + mId);
                mBackgroundImage = BitmapFactory.decodeFile(imagePath);
                if (mBackgroundImage != null) {
                    LogManager.d("Loaded background image: width=" + mBackgroundImage.getWidth()
                            + ", height=" + mBackgroundImage.getHeight());
                    ByteBuffer pixels = ByteBuffer.allocate(
                            mBackgroundImage.getWidth() * mBackgroundImage.getHeight() * 4);
                    mBackgroundImage.copyPixelsToBuffer(pixels);
                    pixels.position(0);

                    // Update texture
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBgImgTexId);
                    GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                            mBackgroundImage.getWidth(), mBackgroundImage.getHeight(),
                            0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
                } else {
                    LogManager.e("Loading background image failed!");
                    // TODO: Notify
                }
            }

            private void setDisplayName(String displayName) {
                mDisplayName = displayName;
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void setDisplayNameAlignment(String horizontal, String vertical) {
                int alignment = 0;
                switch (horizontal) {
                    case "left" :
                        alignment |= ALIGNMENT_LEFT;
                        break;

                    case "right" :
                        alignment |= ALIGNMENT_RIGHT;
                        break;

                    case "hcenter" :
                        alignment |= ALIGNMENT_HCENTER;
                        break;

                    default :
                        LogManager.e("Invalid horizontal alignment type!");
                        return;
                }

                switch (vertical) {
                    case "top" :
                        alignment |= ALIGNMENT_TOP;
                        break;

                    case "bottom" :
                        alignment |= ALIGNMENT_BOTTOM;
                        break;

                    case "vcenter" :
                        alignment |= ALIGNMENT_VCENTER;
                        break;

                    default :
                        LogManager.e("Invalid vertical alignment type!");
                        return;
                }

                mDisplayNameAlignment = alignment;
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void setDisplayNameStyleSheet(String styleSheet) {
                // TODO: Parse style sheet to style map
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void setDisplayNameVisible(String visible) {
                mDisplayNameVisible = (visible.equals("true"));
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void updateDisplayNameImageBuffer() {
                LogManager.i("Updateing display name image buffer for scene #" + mId);
                mDisplayNameImage = renderTextAsBitmap(mDisplayName, 48, Color.WHITE);
                mDisplayNameImageBuffer = ByteBuffer.allocate(
                        mDisplayNameImage.getWidth() * mDisplayNameImage.getHeight() * 4);
                mDisplayNameImage.copyPixelsToBuffer(mDisplayNameImageBuffer);
                mDisplayNameImageBuffer.position(0);

                mNeedUpdateDisplayNameBitmap = false;
            }

            @Nullable
            private Stream getStream(int streamId) {
                for (int i = 0; i < MAXIMUM_VIDEO_STREAM_COUNT; ++i) {
                    if (mStreams[i] != null && mStreams[i].getId() == streamId) {
                        return mStreams[i];
                    }
                }

                return null;
            }
        }

        private class VideoSink implements Runnable {
            public static final int STATUS_UNINITIALIZED = 1;
            public static final int STATUS_CONFIGURED = 2;
            public static final int STATUS_RUNNING = 3;

            private int mId;
            private int mSceneId;

            private volatile VideoSinkConfig mConfig;

            private Surface mInputSurface;
            private MediaCodec mMediaCodec;
            private long mEncodedFrames;
            private MediaCodec.BufferInfo mBufferInfo;

            private volatile VideoSinkHandler mHandler;

            private Object mStatusFence = new Object();
            private volatile int mStatus;

            private Object mOutputLock = new Object();
            private List<SinkOutput> mOutputs = new ArrayList<>();

            private long mPeriodStartTimeNS = 0;
            private int mPeriodFrameCount = 0;
            private int mPeriodFrameBytes = 0;

            private long mKeepLogging = 0;

            private AVPacket mCodecSpecificData = null;

            public VideoSink(int id, int sceneId) {
                mId = id;
                mSceneId = sceneId;

                mStatus = STATUS_UNINITIALIZED;
            }

            public int getId() {
                return mId;
            }

            public int getSceneId() {
                return mSceneId;
            }

            public int getStatus() {
                synchronized (mStatusFence) {
                    return mStatus;
                }
            }

            public VideoSinkConfig getConfig() {
                return mConfig;
            }

            public Surface getInputSurface() {
                return mInputSurface;
            }

            public void configure(VideoSinkConfig config) {
                synchronized(mStatusFence) {
                    if (mStatus == STATUS_RUNNING) {
                        LogManager.e("Video sink is running, please stop first!");
                        // TODO: Notify configure video sink failed
                        return;
                    } else if (mStatus == STATUS_CONFIGURED) {
                        LogManager.e("Video sink already configured, please reset first!");
                        // TODO: Notify configure video sink failed
                        return;
                    }

                    if (mStatus == STATUS_UNINITIALIZED) {
                        // Copy configuration
                        mConfig = config;

                        try {
                            // Set encoder media format
                            MediaFormat format = MediaFormat.createVideoFormat(mConfig.mimeType,
                                    mConfig.width, mConfig.height);
                            // Color format
                            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

                            // Bitrate
                            format.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitrate);

                            // Frame rate
                            format.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.frameRate);

                            // I frame interval
                            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
                                    mConfig.keyFrameInterval);

                            // Create a MediaCodec encoder, and configure it.
                            mMediaCodec = MediaCodec.createEncoderByType(mConfig.mimeType);

                            // Config as encoder
                            mMediaCodec.configure(format, null, null,
                                    MediaCodec.CONFIGURE_FLAG_ENCODE);

                            // Create input surface
                            mInputSurface = mMediaCodec.createInputSurface();

                            mBufferInfo = new MediaCodec.BufferInfo();
                        } catch (IOException ioe) {
                            throw new RuntimeException(ioe);
                        }

                        mStatus = STATUS_CONFIGURED;
                    }
                }
            }
            
            public void addOutput(SinkOutput output) {
                synchronized(mOutputLock) {
                    mOutputs.add(output);
                }
            }
            
            public void removeOutput(SinkOutput output) {
                synchronized(mOutputLock) {
                    mOutputs.remove(output);
                }
            }

            public void start() {
                mCodecSpecificData = null;
                synchronized(mStatusFence) {
                    // Check current status
                    if (mStatus == STATUS_RUNNING) {
                        LogManager.w("Video sink already running!");
                        return;
                    } else if (mStatus == STATUS_UNINITIALIZED) {
                        LogManager.e("Video sink is uninitialized, please configure first!");
                        // TODO: Notify configure video sink failed
                        return;
                    }

                    if (mStatus == STATUS_CONFIGURED) {
                        // Start codec
                        mMediaCodec.start();
                        mEncodedFrames = 0;

                        // Start thread
                        new Thread(this, "VideoSink#" + mId).start();

                        // Waiting for thread ready
                        while (mStatus != STATUS_RUNNING) {
                            try {
                                mStatusFence.wait();
                            } catch (InterruptedException ie) {
                                // ignore
                            }
                        }
                    }
                }
            }

            public void stop() {
                synchronized(mStatusFence) {
                    // Check current status
                    if (mStatus != STATUS_RUNNING) {
                        LogManager.w("Video sink not running!");
                        return;
                    } else {
                        // Notify thread to quit
                        mHandler.sendMessage(
                                mHandler.obtainMessage(VideoSinkHandler.MSG_STOP_RECORDING));

                        // Waiting for encoder stopped
                        while (mStatus != STATUS_UNINITIALIZED) {
                            try {
                                mStatusFence.wait();
                            } catch (InterruptedException ie) {
                                // ignore
                            }
                        }

                        LogManager.i("Video sink #" + mId + " stopped! with " + mEncodedFrames + " frames");
                    }
                }
                mCodecSpecificData = null;
            }

            public void release() {
                // Check current status
                if (mStatus == STATUS_RUNNING) {
                    LogManager.e("Video sink is running, please stop first.");
                    // TODO: Notify start video sink failed
                    return;
                }

                // Release media codec
                mMediaCodec.release();
                mMediaCodec = null;
                mInputSurface = null;
                mBufferInfo = null;
                mConfig = null;

                LogManager.i("Video sink #" + mId + " released! with " + mEncodedFrames + " frames");
            }

            /**
             * Tells the video recorder that a new frame is arriving soon.
             * (Call from non-encoder thread.)
             * <p>
             * This function sends a message and returns immediately. This is fine -- the purpose is
             * to wake the encoder thread up to do work so the producer side doesn't block.
             */
            public void frameAvailableSoon() {
                synchronized (mStatusFence) {
                    if (mStatus != STATUS_RUNNING) {
                        return;
                    }
                }

                mHandler.sendMessage(mHandler.obtainMessage(VideoSinkHandler.MSG_FRAME_AVAILABLE));
            }

            @Override
            public void run() {
                // Establish a Looper for this thread, and define a Handler for it.
                Looper.prepare();

                synchronized (mStatusFence) {
                    mHandler = new VideoSinkHandler(this);
                    mStatus = STATUS_RUNNING;
                    mStatusFence.notify();
                }

                // Enter loop
                Looper.loop();

                LogManager.i("Video sink thread exiting");
                synchronized (mStatusFence) {
                    mStatus = STATUS_UNINITIALIZED;
                    mHandler = null;
                    mStatusFence.notify();
                }
            }

            /**
             * Handles notification of an available frame.
             */
            private void handleFrameAvailable() {
                //if (VERBOSE) LogManager.d("handleFrameAvailable");
                drainEncoder(false);
            }

            /**
             * Handles a request to stop encoding.
             */
            private void handleStopRecording() {
                LogManager.d("handleStopRecording");
                drainEncoder(true);
            }

            /**
             * Extracts all pending data from the encoder and forwards it to the muxer.
             * <p>
             * If endOfStream is not set, this returns when there is no more data to drain.  If it
             * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
             * Calling this with endOfStream set should be done once, right before stopping the muxer.
             * <p>
             * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
             * not recording audio.
             */
            public void drainEncoder(boolean endOfStream) {
                final int TIMEOUT_USEC = 10000;
                final int TIMEOUT_MS = 33;

                if (endOfStream) {
                    //if (VERBOSE) LogManager.d("sending EOS to encoder");
                    mMediaCodec.signalEndOfInputStream();
                }

                while (true) {
                    int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (!endOfStream) {
                            break;      // out of while
                        } else {
                            //if (VERBOSE) LogManager.d("no output available, spinning to await EOS");
                        }
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not expected for an encoder
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // should happen before receiving buffers, and should only happen once
                        //if (mMuxerStarted) {
                        //    throw new RuntimeException("format changed twice");
                        //}
                        MediaFormat newFormat = mMediaCodec.getOutputFormat();
                        LogManager.d("encoder output format changed: " + newFormat);

                        // now that we have the Magic Goodies, start the muxer
                        //mTrackIndex = mMuxer.addTrack(newFormat);
                        //mMuxer.start();
                        //mMuxerStarted = true;
                    } else if (encoderStatus < 0) {
                        LogManager.w("unexpected result from encoder.dequeueOutputBuffer: " +
                                encoderStatus);
                        // let's ignore it
                    } else {
                        ByteBuffer encodedData = mMediaCodec.getOutputBuffer(encoderStatus);
                        if (encodedData == null) {
                            throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                    " was null");
                        }

                        // LogManager.d("encoded output size: "+ mBufferInfo.size + " pts:" + mBufferInfo.presentationTimeUs);
                        if(mBufferInfo.presentationTimeUs < 0) {
                            LogManager.e("!!! MediaEngine Sink error: pts from encoder is invalid " + mBufferInfo.presentationTimeUs);
                        } else if (mBufferInfo.size != 0) {
                            //if (!mMuxerStarted) {
                            //    throw new RuntimeException("muxer hasn't started");
                            //}

                            synchronized(mOutputLock) {
                                for(SinkOutput output: mOutputs) {
                                    try {
                                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                                        encodedData.position(mBufferInfo.offset);
                                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                                        AVPacket pkt = output.output_channel.pollIdlePacket(TIMEOUT_MS);
                                        if(pkt != null) {
                                            mKeepLogging = 0;
                                            ByteBuffer payload = pkt.getPayload();
                                            payload.clear();
                                            payload.put(encodedData);
                                            payload.flip();
                                            pkt.setPts(mBufferInfo.presentationTimeUs);
                                            pkt.setDts(mBufferInfo.presentationTimeUs);
                                            pkt.setMediaType(DataType.VIDEO);
                                            pkt.setTrackIndex(output.track_id);
                                            pkt.setCodecFlags(mBufferInfo.flags);
                                            pkt.setIsKeyFrame((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0);
                                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                                pkt.setDataFlag(DataFlag.CODEC_SPECIFIC_DATA);
                                                // pkt.setIsKeyFrame(true);

                                                encodedData.position(mBufferInfo.offset);
                                                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                                                mCodecSpecificData = new AVPacket(payload.remaining());
                                                mCodecSpecificData.setMediaType(pkt.getMediaType());
                                                mCodecSpecificData.setTrackIndex(pkt.getTrackIndex());
                                                mCodecSpecificData.setCodecFlags(pkt.getCodecFlags());
                                                mCodecSpecificData.setIsKeyFrame(pkt.isKeyFrame());
                                                mCodecSpecificData.setDataFlag(pkt.getDataFlag());
                                                ByteBuffer bakData = mCodecSpecificData.getPayload();
                                                bakData.clear();
                                                bakData.put(encodedData);
                                                bakData.flip();
                                            }else if(pkt.isKeyFrame() && mCodecSpecificData != null) {
                                                AVPacket pkt2 = output.output_channel.pollIdlePacket(TIMEOUT_MS);
                                                if(pkt2 != null) {
                                                    ByteBuffer bakData = mCodecSpecificData.getPayload();
                                                    int pos = bakData.position();
                                                    int lit = bakData.limit();

                                                    ByteBuffer payload2 = pkt2.getPayload();
                                                    payload2.clear();
                                                    payload2.put(bakData);
                                                    payload2.flip();
                                                    pkt2.setPts(pkt.getPts());
                                                    pkt2.setDts(pkt.getDts());
                                                    pkt2.setMediaType(mCodecSpecificData.getMediaType());
                                                    pkt2.setTrackIndex(mCodecSpecificData.getTrackIndex());
                                                    pkt2.setCodecFlags(mCodecSpecificData.getCodecFlags());
                                                    pkt2.setIsKeyFrame(mCodecSpecificData.isKeyFrame());
                                                    pkt2.setDataFlag(mCodecSpecificData.getDataFlag());
                                                    output.output_channel.putBusyPacket(pkt2);

                                                    bakData.position(pos);
                                                    bakData.limit(lit);
                                                }
                                            }
                                            output.output_channel.putBusyPacket(pkt);
                                            //mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                                            //if (VERBOSE) {
                                                //LogManager.d("Sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                                //        mBufferInfo.presentationTimeUs);
                                            //}
                                        } else {
                                            ++mKeepLogging;
                                            if(mKeepLogging <= 10) {
                                                LogManager.w("VideoSink get output packet timeout");
                                                if(mKeepLogging == 10)
                                                    LogManager.w("There are too much the same log. Will combine them");
                                            } else if(mKeepLogging % 100 == 0) {
                                                LogManager.w("VideoSink get output packet timeout " + mKeepLogging);
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        LogManager.w("Poll on AVChannel was interrupted!");
                                        return;
                                    }
                                }
                            }
                            ++mEncodedFrames;

                            // Update the Statis.
                            final int NUM_FRAMES = 120;
                            long timeNowNS = System.nanoTime();
                            if(mPeriodStartTimeNS == 0) {
                                mPeriodFrameCount = 0;
                                mPeriodFrameBytes = 0;
                                mPeriodStartTimeNS = timeNowNS;
                            } else {
                                ++mPeriodFrameCount;
                                mPeriodFrameBytes += mBufferInfo.size;
                                if(mPeriodFrameCount == NUM_FRAMES) {
                                    float elapsedS = (timeNowNS - mPeriodStartTimeNS) / 1000000000.f;
                                    float fps = NUM_FRAMES / elapsedS;
                                    float kbps = mPeriodFrameBytes / elapsedS * 8.f / 1000;

                                    // Invoke callback
                                    MediaEngine.Callback cb = MediaEngine.getInstance().getCallback();
                                    if (cb != null)
                                        cb.onVideoSinkStatistics(mId, fps, (int)kbps);

                                    // Reset fps counter
                                    mPeriodFrameCount = 0;
                                    mPeriodFrameBytes = 0;
                                    mPeriodStartTimeNS = timeNowNS;
                                }
                            }
                        }

                        mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (!endOfStream) {
                                LogManager.w("reached end of stream unexpectedly");
                            } else {
                                //if (VERBOSE) LogManager.d("end of stream reached");
                            }
                            break;      // out of while
                        }
                    }
                }
            }

            private class VideoSinkHandler extends Handler {
                private static final int MSG_STOP_RECORDING = 1;
                private static final int MSG_FRAME_AVAILABLE = 2;

                private WeakReference<VideoSink> mWeakRefVideoSink;

                public VideoSinkHandler(VideoSink videoSink) {
                    mWeakRefVideoSink = new WeakReference<>(videoSink);
                }

                @Override
                public void handleMessage(Message inputMessage) {
                    int what = inputMessage.what;
                    Object obj = inputMessage.obj;

                    VideoSink videoSink = mWeakRefVideoSink.get();
                    if (videoSink == null) {
                        LogManager.w("VideoSinkHandler.handleMessage: videoSink is null");
                        return;
                    }

                    switch (what) {
                        case MSG_STOP_RECORDING:
                            videoSink.handleStopRecording();
                            Looper.myLooper().quit();
                            break;
                        case MSG_FRAME_AVAILABLE:
                            videoSink.handleFrameAvailable();
                            break;
                        default:
                            throw new RuntimeException("Unhandled msg what=" + what);
                    }
                }
            }
        }
    }

    private static class VideoEngineHandler extends Handler {
        private static final int MSG_RENDER_FRAME               = 0x00;
        private static final int MSG_DISPLAY_SURFACE_CHANGED    = 0x01;
        private static final int MSG_SWITCH_TESTING_ITEMS       = 0x02;

        private static final int MSG_ADD_VIDEO_STREAM           = 0x10;
        private static final int MSG_REMOVE_VIDEO_STREAM        = 0x11;

        private static final int MSG_ADD_VIDEO_SCENE            = 0x20;
        private static final int MSG_REMOVE_VIDEO_SCENE         = 0x21;
        private static final int MSG_POST_VIDEO_SCENE_COMMANDS  = 0x22;

        private static final int MSG_ADD_VIDEO_SINK             = 0x30;
        private static final int MSG_REMOVE_VIDEO_SINK          = 0x31;
        private static final int MSG_CONFIG_VIDEO_SINK          = 0x32;
        private static final int MSG_START_VIDEO_SINK           = 0x33;
        private static final int MSG_STOP_VIDEO_SINK            = 0x34;
        private static final int MSG_ADD_VIDEO_SINK_OUTPUT      = 0x35;
        private static final int MSG_REMOVE_VIDEO_SINK_OUTPUT   = 0x36;

        private WeakReference<VideoEngine> mWeakRenderThread;

        public VideoEngineHandler(VideoEngine rt) {
            mWeakRenderThread = new WeakReference<>(rt);
        }

        public void handleMessage(Message msg) {
            // Process incoming messages here
            VideoEngine videoEngine = mWeakRenderThread.get();
            if (videoEngine == null) {
                LogManager.w("VideoEngineHandler.handleMessage: weak ref is null");
                return;
            }

            switch (msg.what) {
                case MSG_RENDER_FRAME : {
                    // Render frame
                    long timestamp = (((long)msg.arg1) << 32) | (((long)msg.arg2) & 0xffffffffL);
                    videoEngine.renderFrame(timestamp);
                    break;
                }

                case MSG_DISPLAY_SURFACE_CHANGED : {
                    int sceneId = msg.arg1;
                    int width = (msg.arg2 >> 16) & 0x0000FFFF;
                    int height = msg.arg2 & 0x0000FFFF;
                    videoEngine.notifyDisplaySurfaceChanged(sceneId, width, height);
                    break;
                }

                case MSG_SWITCH_TESTING_ITEMS: {
                    int sceneId = msg.arg1;
                    int able = msg.arg2;
                    videoEngine.switchTestingItems(sceneId, able>0);
                    break;
                }

                case MSG_ADD_VIDEO_STREAM : {
                    // Add video stream
                    int streamId = msg.arg1;
                    if (msg.obj instanceof String) {
                        String url = (String) (msg.obj);
                        videoEngine.addStream(streamId, url);
                    } else if (msg.obj instanceof AVChannel) {
                        AVChannel channel = (AVChannel)(msg.obj);
                        videoEngine.addStream(streamId, channel);
                    } else if (msg.obj instanceof Integer) {
                        int cameraId = (Integer)msg.obj;
                        videoEngine.addStream(streamId, cameraId);
                    } else {
                        LogManager.e("Object not a valid string or channel.");
                        // TODO: Notify adding video stream failed!
                    }
                    break;
                }

                case MSG_REMOVE_VIDEO_STREAM : {
                    // Remove video stream
                    int streamId = msg.arg1;
                    videoEngine.removeStream(streamId);
                    break;
                }

                case MSG_ADD_VIDEO_SCENE : {
                    // Add video scene
                    int sceneId = msg.arg1;
                    int withSurface = msg.arg2;

                    if (withSurface == 0) {
                        videoEngine.addScene(sceneId);
                    } else if (withSurface == 1) {
                        if (msg.obj instanceof Surface) {
                            Surface surface = (Surface) (msg.obj);
                            videoEngine.addScene(sceneId, surface);
                        } else {
                            LogManager.e("Object not a valid surface!");
                        }
                    }
                    break;
                }

                case MSG_REMOVE_VIDEO_SCENE : {
                    // Remove video scene
                    int sceneId = msg.arg1;
                    videoEngine.removeScene(sceneId);
                    break;
                }

                case MSG_POST_VIDEO_SCENE_COMMANDS : {
                    // Post video scene commands
                    if (msg.obj instanceof String[]) {
                        String[] commands = (String[]) (msg.obj);
                        videoEngine.postSceneCommands(msg.arg1, commands);
                    } else {
                        LogManager.e("Object not a valid string array.");
                    }
                    break;
                }

                case MSG_ADD_VIDEO_SINK : {
                    // Add video sink
                    int sceneId = msg.arg1;
                    int sinkId = msg.arg2;
                    videoEngine.addSink(sceneId, sinkId);
                    break;
                }

                case MSG_REMOVE_VIDEO_SINK : {
                    // Remove video sink
                    int sinkId = msg.arg1;
                    videoEngine.removeSink(sinkId);
                    break;
                }

                case MSG_CONFIG_VIDEO_SINK : {
                    // Configure video sink
                    int sinkId = msg.arg1;
                    if (msg.obj instanceof VideoSinkConfig) {
                        VideoSinkConfig config = (VideoSinkConfig)(msg.obj);
                        videoEngine.configureSink(sinkId, config);
                    } else {
                        LogManager.e("Object not a valid VideoSinkConfig object.");
                    }
                    break;
                }

                case MSG_ADD_VIDEO_SINK_OUTPUT : {
                    // Add video sink output
                    int sinkId = msg.arg1;
                    if (msg.obj instanceof SinkOutput) {
                        SinkOutput output = (SinkOutput)(msg.obj);
                        videoEngine.addSinkOutput(sinkId, output);
                    } else {
                        LogManager.e("Object not a valid SinkOutput object.");
                    }
                    break;
                }
                
                case MSG_REMOVE_VIDEO_SINK_OUTPUT : {
                    // Remove video sink output
                    int sinkId = msg.arg1;
                    if (msg.obj instanceof SinkOutput) {
                        SinkOutput output = (SinkOutput)(msg.obj);
                        videoEngine.removeSinkOutput(sinkId, output);
                    } else {
                        LogManager.e("Object not a valid SinkOutput object.");
                    }
                    break;
                }
                
                case MSG_START_VIDEO_SINK : {
                    // Start video sink
                    int sinkId = msg.arg1;
                    videoEngine.startSink(sinkId);
                    break;
                }

                case MSG_STOP_VIDEO_SINK : {
                    // Stop video sink
                    int sinkId = msg.arg1;
                    videoEngine.stopSink(sinkId);
                    break;
                }

                default : {
                    LogManager.e("Unknown message type!");
                }
            }
        }

        private void sendRenderFrame(long frameTimeNanos) {
            sendMessage(obtainMessage(MSG_RENDER_FRAME,
                    (int)(frameTimeNanos >> 32),
                    (int)frameTimeNanos));
        }

        private void sendDisplaySurfaceChanged(int sceneId, int surfaceWidth, int surfaceHeight) {
            int size = surfaceWidth << 16 | surfaceHeight;
            sendMessage(obtainMessage(MSG_DISPLAY_SURFACE_CHANGED, sceneId, size));
        }

        private void sendSwitchTestingItems(int sceneId, boolean able) {
            sendMessage(obtainMessage(MSG_SWITCH_TESTING_ITEMS, sceneId, able?1:0));
        }

        private void sendAddVideoStream(int streamId, String url) {
            sendMessage(obtainMessage(MSG_ADD_VIDEO_STREAM, streamId, 0, url));
        }

        private void sendAddVideoStream(int streamId, AVChannel channel) {
            sendMessage(obtainMessage(MSG_ADD_VIDEO_STREAM, streamId, 0, channel));
        }

        private void sendAddVideoStream(int streamId, int cameraId) {
            sendMessage(obtainMessage(MSG_ADD_VIDEO_STREAM, streamId, 0, cameraId));
        }

        private void sendRemoveVideoStream(int streamId) {
            sendMessage(obtainMessage(MSG_REMOVE_VIDEO_STREAM, streamId, 0));
        }

        private void sendAddVideoScene(int sceneId) {
            int withSurface = 0;
            sendMessage(obtainMessage(MSG_ADD_VIDEO_SCENE, sceneId, withSurface));
        }

        private void sendAddVideoScene(int sceneId, Surface surface) {
            int withSurface = 1;
            sendMessage(obtainMessage(MSG_ADD_VIDEO_SCENE, sceneId, withSurface, surface));
        }

        private void sendRemoveVideoScene(int sceneId) {
            sendMessage(obtainMessage(MSG_REMOVE_VIDEO_SCENE, sceneId, 0));
        }

        private void sendPostSceneCommands(int sceneId, String[] commands) {
            sendMessage(obtainMessage(MSG_POST_VIDEO_SCENE_COMMANDS, sceneId, 0, commands));
        }

        private void sendAddVideoSink(int sceneId, int sinkId) {
            sendMessage(obtainMessage(MSG_ADD_VIDEO_SINK, sceneId, sinkId));
        }

        private void sendRemoveVideoSink(int sinkId) {
            sendMessage(obtainMessage(MSG_REMOVE_VIDEO_SINK, sinkId, 0));
        }

        private void sendConfigVideoSink(int sinkId, VideoSinkConfig config) {
            sendMessage(obtainMessage(MSG_CONFIG_VIDEO_SINK, sinkId, 0, config));
        }

        private void sendAddVideoSinkOutput(int sinkId, SinkOutput output) {
            sendMessage(obtainMessage(MSG_ADD_VIDEO_SINK_OUTPUT, sinkId, 0, output));
        }

        private void sendRemoveVideoSinkOutput(int sinkId, SinkOutput output) {
            sendMessage(obtainMessage(MSG_REMOVE_VIDEO_SINK_OUTPUT, sinkId, 0, output));
        }
        
        private void sendStartVideoSink(int sinkId) {
            sendMessage(obtainMessage(MSG_START_VIDEO_SINK, sinkId, 0));
        }

        private void sendStopVideoSink(int sinkId) {
            sendMessage(obtainMessage(MSG_STOP_VIDEO_SINK, sinkId, 0));
        }
    }

    private static class AudioEngine {
        private boolean mEchoTesting = false;
        private Map<Integer, AudioSink> mSinks = new HashMap<>();
        private Map<Integer, AudioStream> mStreams = new HashMap<>();
        // private AudioTrack mAudioPlayer = null;
        // private Map<Integer, AudioDecoder> mAudioDecoders = new HashMap<>();

        public AudioEngine() {

        }

        public void release() {
            for(AudioSink sink: mSinks.values())
                sink.release();
            mSinks.clear();
            for(AudioStream stream: mStreams.values())
                stream.release();
            mStreams.clear();


        }

        public MediaFormat getSinkBasicFormat(String mime) {
            Tuple<Integer, Integer> params = sinkBasicFormat(mime);
            if(params == null)
                return null;
            return MediaFormat.createAudioFormat(mime, params.first, params.second);
        }

        public void addSink(int sinkId) {
            if(mSinks.size() >= MAXIMUM_AUDIO_SINK_COUNT) {
                LogManager.e("Can not add any more audio sinks!");
                return;
            }
            if(mSinks.containsKey(sinkId)) {
                LogManager.e("This sink had added before: " + sinkId);
                return;
            }
            mSinks.put(sinkId, new AudioSink(sinkId));
        }

        public void removeSink(int sinkId) {
            if(!mSinks.containsKey(sinkId)) {
                LogManager.e("Has no this sink: " + sinkId);
                return;
            }
            mSinks.remove(sinkId).release();
        }

        private void configureSink(int sinkId, AudioSinkConfig config) {
            if(!mSinks.containsKey(sinkId)) {
                LogManager.e("Has no this sink: " + sinkId);
                return;
            }
            mSinks.get(sinkId).configure(config);
        }

        private void addSinkOutput(int sinkId, SinkOutput output) {
            if(!mSinks.containsKey(sinkId)) {
                LogManager.e("Has no this sink: " + sinkId);
                return;
            }
            mSinks.get(sinkId).addOutput(output);
        }

        private void removeSinkOutput(int sinkId, SinkOutput output) {
            if(!mSinks.containsKey(sinkId)) {
                LogManager.e("Has no this sink: " + sinkId);
                return;
            }
            mSinks.get(sinkId).removeOutput(output);
        }

        private void startSink(int sinkId) {
            if(!mSinks.containsKey(sinkId)) {
                LogManager.e("Has no this sink: " + sinkId);
                return;
            }
            mSinks.get(sinkId).start();
        }

        private void stopSink(int sinkId) {
            if(!mSinks.containsKey(sinkId)) {
                LogManager.e("Has no this sink: " + sinkId);
                return;
            }
            mSinks.get(sinkId).stop();
        }

        public void enableEchoTesting(boolean on) {
            if(on) {
                if(AudioCapturer.getInstance().inited())
                    throw new RuntimeException("AudioCapturer is in using, not support to echo test");
                AudioCapturer.getInstance().init();
                muteStreams(true);
                AudioCapturer.getInstance().enableTesting(true, AudioCapturer.Supporting.TESTING_METHOD_PLAYBACK, AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_MEDIA);
            } else {
                AudioCapturer.getInstance().enableTesting(false, AudioCapturer.Supporting.TESTING_METHOD_NONE, AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_NONE);
                muteStreams(false);
                AudioCapturer.getInstance().close();
            }
        }

        public void addStream(int streamId, AVChannel channel) {
            if(mStreams.size() >= MAXIMUM_AUDIO_STREAM_COUNT) {
                LogManager.e("Can not add any more audio streams!");
                return;
            }
            if(mStreams.containsKey(streamId)) {
                LogManager.e("This stream had added before: " + streamId);
                return;
            }
            mStreams.put(streamId, new AudioStream(streamId, channel));
        }

        public void removeStream(int streamId) {
            if(!mStreams.containsKey(streamId)) {
                LogManager.e("Has no this stream: " + streamId);
                return;
            }
            mStreams.remove(streamId).release();
        }

        public void muteStreams(boolean on) {
            // TODO:
        }

        private Tuple<Integer, Integer> sinkBasicFormat(String mime) {
            if(mime == AudioEncoder.Supporting.ENC_NAME_AAC) {
                return new Tuple<>(AudioCapturer.Supporting.HZ_RECOMMENDED, AudioCapturer.Supporting.CHANNEL_RECOMMENDED);
            } else if(mime == AudioEncoder.Supporting.ENC_NAME_G711A || mime == AudioEncoder.Supporting.ENC_NAME_G711U) {
                return new Tuple<>(AudioEncoder.Supporting.ENC_G711_SAMPLERATE, AudioEncoder.Supporting.ENC_G711_CHANNEL_CNT);
            } else if (AudioEncoder.Supporting.ENC_NAMEs.contains(mime)){
                throw new RuntimeException("logical error: not implement");
            } else {
                LogManager.e("non-supported audio codec: " + mime);
                return null;
            }
        }

        private class AudioStream implements AudioDecoder.Callback {
            static private final int THREAD_EXIT_TIMEOUT_MS = 50;
            static private final int PKT_WAIT_TIMEOUT_MS = 30;

            private int mId = -1;
            MediaFormat mMediaFormat = null;
            private AVChannel mAVChannel = null;
            private AudioDecoder mAudioDecoder = null;
            private AudioTrack mAudioTrack = null;

            private boolean mRunning = false;
            private Thread mWorkingThread = null;

            public AudioStream(int id, AVChannel channel) {
                mId = id;
                mAVChannel = channel;
                mMediaFormat = mAVChannel.getMediaFormat();

                mAudioDecoder = new AudioDecoder();
                if(mAudioDecoder.init(mMediaFormat, channel) != 0) {
                    LogManager.e("AudioDecoder init failed");
                    return;
                }
                mAudioDecoder.addCallback(this);
                mAudioDecoder.start();

                initAudioPlay();
            }

            public int getId() {
                return mId;
            }

            public void release() {
                if(mAudioDecoder != null) {
                    mAudioDecoder.removeCallback(this);
                    mAudioDecoder.close();
                    mAudioDecoder = null;
                }
                if(mAudioTrack != null) {
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
                mAVChannel = null;
            }

            private void initAudioPlay() {
                int playMode = AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_MEDIA;
                int sampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channelCnt = mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                int channelMode = channelCnt == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
                int sampleFormat = AudioCapturer.Supporting.DEFAULT_SAMPLE_FORMAT;

                int minPlaySizeInByte = AudioTrack.getMinBufferSize(sampleRate, channelMode, sampleFormat);
                if (minPlaySizeInByte == AudioTrack.ERROR_BAD_VALUE)
                    throw new RuntimeException(String.format("AudioTrack.getMinBufferSize invalid parameter: rate-%d mode-%d format-%d",
                            sampleRate, channelMode, sampleFormat));
                int sampleWidthInBytes = AudioCapturer.SampleFormat2Bytes(sampleFormat);
                int sampleCntOfFrame = sampleRate * AudioCapturer.Supporting.FRAME_DURATION_MS_RECOMMENDED / 1000 * channelCnt;
                int frameSizeInByte = sampleCntOfFrame * sampleWidthInBytes;
                minPlaySizeInByte = frameSizeInByte > minPlaySizeInByte ? frameSizeInByte : minPlaySizeInByte;

                mAudioTrack = new AudioTrack(playMode, sampleRate, channelMode, sampleFormat, minPlaySizeInByte, AudioTrack.MODE_STREAM);
                if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED)
                    throw new RuntimeException("AudioTrack initialize failed");
                mAudioTrack.play();
            }

            @Override
            public void onData(ByteBuffer data, long ptsUs) {
                int wsize = data.remaining();
                int ret = mAudioTrack.write(data, wsize, AudioTrack.WRITE_NON_BLOCKING);
                if(ret < 0) {
                    LogManager.e("AudioTrack write error: " + ret);
                } else if(ret != wsize) {
                    LogManager.w(String.format("AudioTrack write %d(%d)", ret, wsize));
                }
            }
        }

        private class AudioSink implements AudioEncoder.Callback {

            static private final int TIMEOUT_MS = 40;
            public int Id = -1;
            private AudioEncoder mAudioEncoder = null;
            private AudioCapturer mAudioCapturer = null;
            private List<SinkOutput> mOutputs = new ArrayList<>();
            private long mKeepLogging = 0;

            public AudioSink(int id) {
                Id = id;
                mAudioEncoder = new AudioEncoder();
                mAudioEncoder.addCallback(this);
            }

            public void configure(AudioSinkConfig config) {
                if(AudioCapturer.getInstance().inited())
                    throw new RuntimeException("AudioCapturer is in using, not support to add new audio sink");
                Tuple<Integer/*rate*/, Integer/*ch*/> params = sinkBasicFormat(config.encFormat.CodecName);
                mAudioCapturer = AudioCapturer.getInstance();
                mAudioCapturer.init(
                        AudioCapturer.Supporting.DEFAULT_AUDIO_SOURCE,
                        params.first, params.second,
                        AudioCapturer.Supporting.DEFAULT_SAMPLE_FORMAT,
                        AudioCapturer.Supporting.FRAME_DURATION_MS_RECOMMENDED);
                mAudioEncoder.init(mAudioCapturer.PCMFormat, config.encFormat);
            }

            public void release() {
                if(mAudioCapturer != null) {
                    mAudioCapturer.close();
                    mAudioCapturer = null;
                }
                synchronized(mOutputs) {
                    mOutputs.clear();
                }
                mAudioEncoder.close();
                mAudioEncoder = null;
                LogManager.i("Audio sink #" + Id + " released!");
            }

            public void addOutput(SinkOutput output) {
                synchronized(mOutputs) {
                    mOutputs.add(output);
                }
            }

            public void removeOutput(SinkOutput output) {
                synchronized(mOutputs) {
                    mOutputs.remove(output);
                }
            }

            public void start() {
                mAudioEncoder.start();
                mAudioCapturer.addCallback(mAudioEncoder);
                LogManager.i("Audio sink #" + Id + " started!");
            }

            public void stop() {
                mAudioCapturer.removeCallback(mAudioEncoder);
                mAudioEncoder.stop();
                LogManager.i("Audio sink #" + Id + " stopped!");
            }

            // Implementation of AudioEncoder.Callback
            public void onData(ByteBuffer data, long ptsUs, int flags) {
                int pos = data.position();
                synchronized(mOutputs) {
                    for (SinkOutput output : mOutputs) {
                        try {
                            AVPacket pkt = output.output_channel.pollIdlePacket(TIMEOUT_MS);
                            if(pkt != null) {
                                mKeepLogging = 0;
                                ByteBuffer payload = pkt.getPayload();
                                payload.clear();
                                payload.put(data);
                                payload.flip();
                                data.position(pos);
                                pkt.setPts(ptsUs);
                                pkt.setDts(ptsUs);
                                pkt.setMediaType(DataType.AUDIO);
                                pkt.setTrackIndex(output.track_id);
                                pkt.setCodecFlags(flags);
                                if ((flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    pkt.setDataFlag(DataFlag.CODEC_SPECIFIC_DATA);
                                }
                                output.output_channel.putBusyPacket(pkt);
                            } else {
                                ++mKeepLogging;
                                if(mKeepLogging <= 10) {
                                    LogManager.w("AudioSink get output packet timeout");
                                    if(mKeepLogging == 10)
                                        LogManager.w("There are too much the same log. Will combine them");
                                } else if(mKeepLogging % 100 == 0) {
                                    LogManager.w("AudioSink get output packet timeout " + mKeepLogging);
                                }
                            }
                        } catch (InterruptedException e) {
                            LogManager.w("Poll on AVChannel was interrupted!");
                            return;
                        }
                    }
                }
            }

        }
    }

    static public class Tester implements SimpleTesting.Tester {
        public void start(Object obj) {
            CameraManager cameraMgr = (CameraManager)((Context)obj).getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] cameraIds = cameraMgr.getCameraIdList();
                LogManager.i("Got camera id list length = " + cameraIds.length);
                for (int i = 0; i < cameraIds.length; ++i) {
                    LogManager.i("Camera Id: " + cameraIds[i]);
                    CameraCharacteristics camCaps = cameraMgr.getCameraCharacteristics(cameraIds[i]);
                }
            } catch (CameraAccessException e) {
                LogManager.e(e.toString());
            }

            MediaEngine me = MediaEngine.getInstance();
            me.addVideoStream(1);
            String[] cmds = {
                    "set stream 2 visible true",
                    "set stream 2 dst-rect 0.2:0.2:0.6:0.6",
                    "set stream 2 z-index 999"
            };
            me.postVideoSceneCommands(1, cmds);
        }
        public void next() {
        }
    }
}
