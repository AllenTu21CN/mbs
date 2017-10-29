package sanp.mpx.mc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.Tuple;
import sanp.avalon.libs.base.utils.Tuple3;
import sanp.avalon.libs.SimpleTesting;
import sanp.avalon.libs.media.audio.AudioEncoder;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVDefines.DataType;


public class MediaController implements MediaEngine.Callback, IOEngine.IOSession.Observer, Runnable {
    private static final String TAG = "MediaController";

    public static final String SAVE_PATH = System.getenv("EXTERNAL_STORAGE") + "/MPX";
    public static final String BACKGROUND_DEFAULT_COLOR = "#121212";
    public static final String BACKGROUND_DEFAULT_IMAGE = "none";

    public static final int ALIGNMENT_UNDEFINED = -1;
    public static final int HORIZONTAL_LEFT     = 0;
    public static final int HORIZONTAL_RIGHT    = 1;
    public static final int HORIZONTAL_HCENTER  = 2;
    public static final int VERTICAL_TOP        = 3;
    public static final int VERTICAL_BOTTOM     = 4;
    public static final int VERTICAL_VCENTER    = 5;

    public static final int REOPEN_CNT_FOREVER     = -1;
    public static final int REOPEN_CNT_FORBIDDEN   = 0;
    public static final int RECOMMENDED_REOPEN_CNT = 20;

    public static final int SCENE_IDX_LOCAL_DISPLAY = 0;
    public static final int SCENE_IDX_SEND_TO_REMOTE = 1;

    private static final float PAPER_WIDTH = 1920.0f;
    private static final float PAPER_HEIGHT = 1080.0f;
    private static final float ERROR_PRECISION = 0.0073f;

    private static final long THREAD_EXIT_TIMEOUT_MS = 1000;
    private static final long RETRY_MIN_INTERVAL_MS = 1000;
    private static final long RETRY_MAX_INTERVAL_MS = 7000;
    private static final long RETRY_STEP_INTERVAL_MS = 1000;
    private static final long TASK_DELAY_MS = 0;

    public static MediaController allocateInstance(Context context) {
        if (gMediaController == null) {
            synchronized (MediaController.class) {
                if (gMediaController == null) {
                    gMediaController = new MediaController(context);
                }
            }
        }
        return gMediaController;
    }

    public static MediaController getInstance() {
        return gMediaController;
    }

    public static int getWidthFromFormat(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_WIDTH);
    }

    public static int getHeightFromFormat(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_HEIGHT);
    }

    public static int getFramerateFromFormat(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_FRAME_RATE);
    }

    public static int getBitrateFromFormat(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_BIT_RATE);
    }

    public interface Observer {
        void onSourceAdded(int id, String url, int result);

        void onSourceLost(int id, String url, int result);

        void onSourceResolutionChanged(int id, int width, int height);

        void onSourceStatistics(int id, float fps, int kbps);

        void onOutputAdded(int id, String url, int result);

        void onOutputLost(int id, String url, int result);

        void onOutputStatistics(int id, float fps, int kbps);

        void onVideoRendererStatistics(float fps, long droppedFrame);
    };

    public class Position {
        public float X = 0;
        public float Y = 0;
        public float Width = 0;
        public float Height = 0;

        public Position() {
        }

        public Position(float x, float y, float w, float h) {
            update(x, y, w, h);
        }

        public Position(String position) {
            update(position);
        }

        public void update(float x, float y, float w, float h) {
            X = x;
            Y = y;
            Width = w;
            Height = h;
        }

        public void update(String position) {
            String[] ps = position.split(":");
            if (ps.length != 4)
                throw new RuntimeException("invalid position string:" + position);
            X = Float.parseFloat(ps[0]);
            Y = Float.parseFloat(ps[1]);
            Width = Float.parseFloat(ps[2]);
            Height = Float.parseFloat(ps[3]);
        }

        public void update(Position position) {
            X = position.X;
            Y = position.Y;
            Width = position.Width;
            Height = position.Height;
        }

        public boolean isEqual(Position other) {
            return (X == other.X &&
                    Y == other.Y &&
                    Width == other.Width &&
                    Height == other.Height);
        }

        public boolean isValid() {
            return X >= 0 && Y >= 0 && Width > 0 && Height > 0;
        }

        public String toString() {
            return String.format("%.4f:%.4f:%.4f:%.4f", X, Y, Width, Height);
        }
    };

    public class Region extends Position {
        public Region() {
            super();
        }

        public Region(float x, float y, float w, float h) {
            super(x, y, w, h);
        }

        public Region(String position) {
            super(position);
        }
    }

    private static Map<Integer, String> sAlignmentNames = new HashMap<Integer, String>() {{
        put(HORIZONTAL_LEFT, "left");
        put(HORIZONTAL_RIGHT, "right");
        put(HORIZONTAL_HCENTER, "hcenter");
        put(VERTICAL_TOP, "top");
        put(VERTICAL_BOTTOM, "bottom");
        put(VERTICAL_VCENTER, "vcenter");
    }};

    private class Alignment {
        public int Horizontal = ALIGNMENT_UNDEFINED;
        public int Vertical = ALIGNMENT_UNDEFINED;

        public Alignment() {
        }

        public Alignment(int horizontal, int vertical) {
            update(horizontal, vertical);
        }

        public void update(int horizontal, int vertical) {
            if (horizontal < HORIZONTAL_LEFT ||
                    horizontal > HORIZONTAL_HCENTER ||
                    vertical < VERTICAL_TOP ||
                    vertical > VERTICAL_VCENTER)
                throw new RuntimeException(String.format("invalid Alignment value(%d:%d)", horizontal, vertical));
            Horizontal = horizontal;
            Vertical = vertical;
        }

        public void update(Alignment other) {
            Horizontal = other.Horizontal;
            Vertical = other.Vertical;
        }

        public String toString() {
            return String.format("%s:%s", sAlignmentNames.get(Horizontal), sAlignmentNames.get(Vertical));
        }
    }

    private class DisplayName {

        public String Content = "!!!Not Set!!!";
        public Alignment Align = new Alignment();
        public String Style = "";

        DisplayName() {
        }

        DisplayName(String content, Alignment align, String style) {
            update(content, align, style);
        }

        public void update(String content, Alignment align, String style) {
            Content = content;
            Align.update(align);
            Style = style;
        }

        public void update(DisplayName other) {
            Content = other.Content;
            Align.update(other.Align);
            Style = other.Style;
        }

        public List<String> toCmds(String prefix, boolean visible) {
            List<String> cmds = new ArrayList<>();
            if (visible) {
                cmds.add(String.format("%s display-name-visible true", prefix));
                cmds.add(String.format("%s display-name %s", prefix, Content));
                cmds.add(String.format("%s display-name-alignment %s", prefix, Align.toString()));
                if (!Style.equals(""))
                    cmds.add(String.format("%s display-name-style-sheet %s", prefix, Style));
            } else {
                cmds.add(String.format("%s display-name-visible false", prefix));
            }
            return cmds;
        }

        public boolean isEqual(DisplayName other) {
            return (
                    Content == other.Content &&
                            Align.Horizontal == other.Align.Horizontal &&
                            Align.Vertical == other.Align.Vertical &&
                            Style == other.Style
            );
        }
    };

    private static class MediaCapture {
        static public final String MEDIA_CAPTURE_NAME_DEFAULT = "default";
        static public final String AUDIO_CAPTURE_NAME_MIC = "mic";
        static public final String VIDEO_CAPTURE_NAME_INTEGRATED_CAMERA = "integrated_camera";
        static public final String VIDEO_CAPTURE_NAME_USB_CAMERA = "usb_camera";
        static public final String VIDEO_CAPTURE_NAME_HDMI_IN = "hdmi_in";

        static public boolean isCapture(String url) {
            return url.startsWith("capture://");
        }

        public int ID = -1;
        public int DeviceId = -1;
        public String DeviceName = MEDIA_CAPTURE_NAME_DEFAULT;
        public DataType Type = DataType.UNKNOWN;
        public String Description = null;
        public String Path = null;
        public List<String> Others = new ArrayList<>();
        private Pattern mUrlPattern = Pattern.compile("^capture://([^?]+)(\\?(.+))?$", Pattern.CASE_INSENSITIVE);

        public MediaCapture(int id, String url) {
            ID = id;
            Matcher m = mUrlPattern.matcher(url);
            if (!m.matches())
                throw new RuntimeException("invalid url for MediaCapture: " + url);
            Path = m.group(1);
            if (m.group(2) != null) {
                String[] params = m.group(3).split("&");
                for (String param : params) {
                    if (param.startsWith("type=")) {
                        String type = param.split("=")[1].trim();
                        if(type.equals("video"))
                            Type = DataType.VIDEO;
                        else if(type.equals("audio"))
                            Type = DataType.AUDIO;
                    } else if (param.startsWith("id=")) {
                        DeviceId = Integer.valueOf(param.split("=")[1].trim());
                    } else if (param.startsWith("name=")) {
                        DeviceName = param.split("=")[1].trim();
                    } else if (param.startsWith("description=")) {
                        Description = param.split("=")[1].trim();
                    } else {
                        Others.add(param);
                    }
                }
            }
        }
    };

    private class MediaSink<Config> {
        public int id = -1;
        public Config config = null;
        public List<Integer> outputs = new ArrayList<>();

        public MediaSink(int id, Config config) {
            this.id = id;
            this.config = config;
        }
    }

    private class OutputSession {
        public int id = -1;
        public int sceneIdx = -1;
        int videoTrackId = -1;
        int audioTrackId = -1;
        int videoSinkId = -1;
        int audioSinkId = -1;
        MediaEngine.SinkOutput videoOutput = null;
        MediaEngine.SinkOutput audioOutput = null;

        OutputSession(int id, int sceneIdx) {
            this.id = id;
            this.sceneIdx = sceneIdx;
        }

        void set(boolean isVideo, int sinkId, MediaEngine.SinkOutput output) {
            if (isVideo) {
                videoSinkId = sinkId;
                videoOutput = output;
            } else {
                audioSinkId = sinkId;
                audioOutput = output;
            }
        }
    }

    private class AudioStream {
        public String Url;
        public int ID = -1;
        public int TrackID = -1;
        private MediaEngine mMediaEngine = null;

        private List<Scene> mReferences = new ArrayList<>();

        public AudioStream(int trackID, MediaEngine mediaEngine, String url, AVChannel channel) {
            Url = url;
            TrackID = trackID;
            mMediaEngine = mediaEngine;
            ID = mMediaEngine.addAudioStream(channel);
            if (ID < 0)
                throw new RuntimeException("Create addAudioStream failed: " + ID);
        }

        public void release() {
            if(mReferences.size() > 0) {
                throw new RuntimeException("logical error: release all AudioStream first");
            }
            mMediaEngine.removeAudioStream(ID);
            mMediaEngine = null;
            TrackID = -1;
            Url = null;
            ID = -1;
        }

        public void addRef(Scene scene) {
            synchronized (mReferences) {
                if (!mReferences.contains(scene))
                    mReferences.add(scene);
            }
        }

        public void removeRef(Scene scene) {
            synchronized (mReferences) {
                if (mReferences.contains(scene))
                    mReferences.remove(scene);
            }
        }

    };

    private class VideoStream {
        public String Url;
        public int ID = -1;
        public int TrackID = -1;
        private MediaEngine mMediaEngine = null;
        private MediaCapture mMediaCapture = null;

        public int mStatisWidth = 0;
        public int mStatisHeight = 0;
        private int mStatisFps = 0;
        private int mStatisBitrateKbps = 0;

        private List<Scene> mReferences = new ArrayList<>();

        public VideoStream(MediaEngine mediaEngine, String url, MediaCapture capture) {
            Url = url;
            mMediaEngine = mediaEngine;
            mMediaCapture = capture;
            ID = mMediaEngine.addVideoStream(capture.DeviceId);
            if (ID < 0)
                throw new RuntimeException("Create VideoStream failed: " + ID);
        }

        public VideoStream(int trackID, MediaEngine mediaEngine, String url, AVChannel channel) {
            Url = url;
            TrackID = trackID;
            mMediaEngine = mediaEngine;
            ID = mMediaEngine.addVideoStream(channel);
            if (ID < 0)
                throw new RuntimeException("Create VideoStream failed: " + ID);
        }

        public void release() {
            if(mReferences.size() > 0) {
                throw new RuntimeException("logical error: release all VideoStream first");
            }
            mMediaEngine.removeVideoStream(ID);
            mMediaEngine = null;
            TrackID = -1;
            Url = null;
            ID = -1;
        }

        public boolean isCapture() {
            return mMediaCapture != null;
        }

        public void display(boolean visible, boolean immediate) {
            synchronized (mReferences) {
                for(Scene scene: mReferences)
                    scene.displayStream(this, visible, immediate);
            }
        }

        public void setDisplayName(String name, int horizontal, int vertical, String styleSheet, boolean immediate) {
            synchronized (mReferences) {
                for(Scene scene: mReferences)
                    scene.setStreamDisplayName(this, name, horizontal, vertical, styleSheet, immediate);
            }
        }

        public void displayName(boolean visible, boolean immediate) {
            synchronized (mReferences) {
                for(Scene scene: mReferences)
                    scene.displayStreamName(this, visible, immediate);
            }
        }

        public MediaFormat getStatistics() {
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mStatisWidth, mStatisHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mStatisFps);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mStatisBitrateKbps);
            return format;
        }

        public void addRef(Scene scene) {
            synchronized (mReferences) {
                if (!mReferences.contains(scene))
                    mReferences.add(scene);
            }
        }

        public void removeRef(Scene scene) {
            synchronized (mReferences) {
                if (mReferences.contains(scene))
                    mReferences.remove(scene);
            }
        }

        public void onResolutionChanged(int newWidth, int newHeight) {
            mStatisWidth = newWidth;
            mStatisHeight = newWidth;
            synchronized (mReferences) {
                for(Scene scene: mReferences)
                    scene.onStreamResolutionChanged(this, newWidth, newHeight);
            }
        }

        public void onStatistics(int fps, int bitrate) {
            mStatisFps = fps;
            mStatisBitrateKbps = bitrate;
        }
    };

    private class Scene {
        static private final String filePrefix = "file://";
        static private final String colorPrefix = "#";

        public int ID = -1;
        private String mBackgroundColor = BACKGROUND_DEFAULT_COLOR;
        private String mBackgroundImage = BACKGROUND_DEFAULT_IMAGE;
        private DisplayName mName = new DisplayName();
        private boolean mNameVisible = false;
        private boolean mFrozen = false;

        private boolean mBackgroundColorChanged = true;
        private boolean mBackgroundImageChanged = false;
        private boolean mNameChanged = false;
        private boolean mFrozenChanged = false;

        private MediaEngine mMediaEngine = null;
        private Map<Integer, VideoStreamRef> mVideoStreams = new HashMap<>();

        private Map<Integer, MediaSink<MediaEngine.VideoSinkConfig>> mVideoSinks = new HashMap<>(MediaEngine.MAXIMUM_VIDEO_SINK_COUNT);
        private Map<Integer, MediaSink<MediaEngine.AudioSinkConfig>> mAudioSinks = new HashMap<>(MediaEngine.MAXIMUM_AUDIO_SINK_COUNT);

        private ScreenLayout.LayoutMode mCurrentMode = ScreenLayout.LayoutMode.UNSPECIFIED;
        private int mCurrentSubScreenCnt = -1;

        private class VideoStreamRef {
            private int mStreamID = -1;

            private boolean mVisible = false;
            private Position mDstPosition = new Position(0, 0, 1, 1);
            private Position mSrcPosition = new Position(0, 0, 1, 1);
            private DisplayName mName = new DisplayName();
            private boolean mNameVisible = false;
            private int mZIndex = -1;
            private float mOpacity = -1;

            private boolean mVisibleChanged = false;
            private boolean mDstPositionChanged = true;
            private boolean mSrcPositionChanged = false;
            private boolean mNameChanged = false;
            private boolean mZIndexChanged = false;
            private boolean mOpacityChanged = false;

            private int mWidth = 1920;
            private int mHeight = 1080;
            private Position mExpectedDstPos = new Position();
            private ScreenLayout.FillPattern mPattern = ScreenLayout.FillPattern.FILL_PATTERN_NONE;

            public VideoStream mVideoStream = null;

            public VideoStreamRef(VideoStream stream) {
                mStreamID = stream.ID;
                mVideoStream = stream;
                if(stream.mStatisWidth > 0)
                    mWidth = stream.mStatisWidth;
                if(stream.mStatisHeight > 0)
                    mHeight = stream.mStatisHeight;
            }

            public void display(boolean visible, boolean immediate) {
                if (visible == mVisible)
                    return;

                mVisible = visible;
                if (immediate) {
                    String[] cmd = {toVisibleCmd()};
                    postVideoSceneCmds(cmd);
                    mVisibleChanged = false;
                } else {
                    mVisibleChanged = true;
                }
            }

            public void setDisplayName(String name, int horizontal, int vertical, String styleSheet, boolean immediate) {
                DisplayName newName = new DisplayName(name, new Alignment(horizontal, vertical), styleSheet);
                if (mName.isEqual(newName))
                    return;

                mName.update(newName);
                flushDisplayName(immediate);
            }

            public void displayName(boolean visible, boolean immediate) {
                if (visible == mNameVisible)
                    return;

                mNameVisible = visible;
                flushDisplayName(immediate);
            }

            public void setZIndex(int index, boolean immediate) {
                if (index == mZIndex)
                    return;

                mZIndex = index;
                if (immediate) {
                    String[] cmd = {toZIndexCmd()};
                    postVideoSceneCmds(cmd);
                    mZIndexChanged = false;
                } else {
                    mZIndexChanged = true;
                }
            }

            public void setOpacity(float opacity, boolean immediate) {
                if (opacity == mOpacity)
                    return;

                mOpacity = opacity;
                if (immediate) {
                    String[] cmd = {toOpacityCmd()};
                    postVideoSceneCmds(cmd);
                    mOpacityChanged = false;
                } else {
                    mOpacityChanged = true;
                }
            }

            public void setPosition(ScreenLayout.FillPattern newPattern, Position pos) {
                if (mPattern != newPattern || !mExpectedDstPos.isEqual(pos)) {
                    mPattern = newPattern;
                    mExpectedDstPos.update(pos);
                    flushPosition();
                } else {
                    display(true, true);
                }
            }

            public void cutRegion(Region region) {
                setSrcPosition(region, false);
                display(true, false);
                flush();
            }

            public void flush() {
                List<String> cmds = new ArrayList<>();

                if (mVisible) {

                    if (mVisibleChanged) {
                        cmds.add(toVisibleCmd());
                        mVisibleChanged = false;
                    }

                    if (mDstPositionChanged) {
                        cmds.addAll(toDstPositionCmds());
                        mDstPositionChanged = false;
                    }

                    if (mSrcPositionChanged) {
                        cmds.add(toSrcPositionCmd());
                        mSrcPositionChanged = false;
                    }

                    if (mNameChanged) {
                        cmds.addAll(toNameCmds());
                        mNameChanged = false;
                    }

                    if (mZIndexChanged) {
                        cmds.add(toZIndexCmd());
                        mZIndexChanged = false;
                    }

                    if (mOpacityChanged) {
                        cmds.add(toOpacityCmd());
                        mOpacityChanged = false;
                    }

                } else {
                    if (!mVisibleChanged)
                        return;
                    cmds.add(String.format("set stream %d visible false", mStreamID));
                    mVisibleChanged = false;
                }

                if (cmds.size() > 0) {
                    postVideoSceneCmds(cmds.toArray(new String[cmds.size()]));
                }
            }

            public void onResolutionChanged(int newWidth, int newHeight) {
                if (!mExpectedDstPos.isValid() ||
                        mPattern == ScreenLayout.FillPattern.FILL_PATTERN_NONE ||
                        isSimilarRatio(newWidth, newHeight, mWidth, mHeight)) {
                    mWidth = newWidth;
                    mHeight = newHeight;
                    return;
                }
                mWidth = newWidth;
                mHeight = newHeight;
                flushPosition();
            }

            // 拉伸/充满
            private int stretch() {
                return stretch(mExpectedDstPos);
            }

            // 拉伸/充满
            private int stretch(Position pos) {
                setSrcPosition(new Position(0.0f, 0.0f, 1.0f, 1.0f), false);
                setDstPosition(pos, false);
                display(true, false);
                flush();
                return 0;
            }

            // 裁剪
            private int crop() {
                float dst_width = mExpectedDstPos.Width * PAPER_WIDTH;
                float dst_height = mExpectedDstPos.Height * PAPER_HEIGHT;
                if (isSimilarRatio(mWidth, mHeight, dst_width, dst_height))
                    return stretch();

                Region using_region = maxRegionInRatio(mWidth, mHeight, dst_width, dst_height);
                setSrcPosition(using_region, false);
                setDstPosition(mExpectedDstPos, false);
                display(true, false);
                flush();
                return 0;
            }

            // 适应/完整
            private int adapte() {
                float dst_width = mExpectedDstPos.Width * PAPER_WIDTH;
                float dst_height = mExpectedDstPos.Height * PAPER_HEIGHT;
                if (isSimilarRatio(mWidth, mHeight, dst_width, dst_height))
                    return stretch();

                Region dst_region = maxRegionInRatio(dst_width, dst_height, mWidth, mHeight);
                float new_x = (dst_region.X * mExpectedDstPos.Width) + mExpectedDstPos.X;
                float new_y = (dst_region.Y * mExpectedDstPos.Height) + mExpectedDstPos.Y;
                float new_w = dst_region.Width * mExpectedDstPos.Width;
                float new_h = dst_region.Height * mExpectedDstPos.Height;
                return stretch(new Position(new_x, new_y, new_w, new_h));

                /*
                float fx = pos.X;
                float fy = pos.Y;
                float dst_width_per_16 = pos.Width;
                float dst_height_per_9 = pos.Height;
                if (dst_width_per_16 < dst_height_per_9) {
                    fy = (dst_height_per_9 - dst_width_per_16) / 2 + fy;
                    dst_height_per_9 = dst_width_per_16;
                } else {
                    fx = (dst_width_per_16 - dst_height_per_9) / 2 + fx;
                    dst_width_per_16 = dst_height_per_9;
                }
                return setSourcePatternStretched(stream, new Position(fx, fy, dst_width_per_16, dst_height_per_9));
                */
            }

            private void setDstPosition(Position pos, boolean immediate) {
                if (mDstPosition.isEqual(pos))
                    return;

                mDstPosition.update(pos);
                if (immediate) {
                    postVideoSceneCmds((String[]) (toDstPositionCmds().toArray()));
                    mDstPositionChanged = false;
                } else {
                    mDstPositionChanged = true;
                }
            }

            private void setSrcPosition(Position pos, boolean immediate) {
                if (mSrcPosition.isEqual(pos))
                    return;

                mSrcPosition.update(pos);
                if (immediate) {
                    String[] cmd = {toSrcPositionCmd()};
                    postVideoSceneCmds(cmd);
                    mSrcPositionChanged = false;
                } else {
                    mSrcPositionChanged = true;
                }
            }

            private void flushPosition() {
                if (mPattern == ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING)
                    adapte();
                else if (mPattern == ScreenLayout.FillPattern.FILL_PATTERN_STRETCHED)
                    stretch();
                else if (mPattern == ScreenLayout.FillPattern.FILL_PATTERN_CROPPING)
                    crop();
                else
                    throw new RuntimeException("Unknow pattern: " + mPattern);
            }

            private boolean isSimilarRatio(float src_width, float src_height, float dst_width, float dst_height) {
                float mid_ratio = src_width / src_height;
                float min_ratio = (1 - ERROR_PRECISION) * mid_ratio;
                float max_ratio = (1 + ERROR_PRECISION) * mid_ratio;
                float dst_ratio = dst_width / dst_height;
                return dst_ratio >= min_ratio && dst_ratio <= max_ratio;
            }

            private Region maxRegionInRatio(float src_width, float src_height, float dst_width, float dst_height) {
                float width_using_ratio = dst_width / src_width;
                float height_using_ratio = dst_height / src_height;
                float max_using_ratio = width_using_ratio > height_using_ratio ? width_using_ratio : height_using_ratio;
                float max_sub_stream_width = dst_width / max_using_ratio;
                float max_sub_stream_height = dst_height / max_using_ratio;
                max_sub_stream_width = max_sub_stream_width > src_width ? src_width : max_sub_stream_width;
                max_sub_stream_height = max_sub_stream_height > src_height ? src_height : max_sub_stream_height;
                float x = (src_width - max_sub_stream_width) / 2;
                float y = (src_height - max_sub_stream_height) / 2;
                return new Region(x / src_width, y / src_height,
                        max_sub_stream_width / src_width, max_sub_stream_height / src_height);
            }

            private void flushDisplayName(boolean immediate) {
                if (immediate) {
                    List<String> cmds = toNameCmds();
                    postVideoSceneCmds(cmds.toArray(new String[cmds.size()]));
                    mNameChanged = false;
                } else {
                    mNameChanged = true;
                }
            }

            private String toVisibleCmd() {
                return String.format("set stream %d visible %s", mStreamID, mVisible ? "true" : "false");
            }

            private List<String> toDstPositionCmds() {
                List<String> cmds = new ArrayList<>();
                cmds.add(String.format("set stream %d dst-rect %s", mStreamID, mDstPosition.toString()));
                if (mCurrentMode == ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING) {
                    String zero_string = ScreenLayout.getSubScreenPosition(mCurrentMode, mCurrentSubScreenCnt, 0);
                    Position zero_pos = new Position(zero_string);
                    cmds.add(String.format("set stream %d z-index %d", mStreamID, zero_pos.isEqual(mExpectedDstPos) ? 0 : 1));
                }
                return cmds;
            }

            private String toSrcPositionCmd() {
                return String.format("set stream %d src-rect %s", mStreamID, mSrcPosition.toString());
            }

            private List<String> toNameCmds() {
                return mName.toCmds(String.format("set stream %d", mStreamID), mNameVisible);
            }

            private String toZIndexCmd() {
                return String.format("set stream %d z-index %d", mStreamID, mZIndex);
            }

            private String toOpacityCmd() {
                return String.format("set stream %d opacity %.4f", mStreamID, mOpacity);
            }
        };

        public Scene(MediaEngine mediaEngine, Surface surface) {
            mMediaEngine = mediaEngine;
            if(surface == null)
                ID = mMediaEngine.addVideoScene();
            else
                ID = mMediaEngine.addVideoScene(surface);
            if (ID < 0)
                throw new RuntimeException("Create Scene failed: " + ID);
        }

        public void release() {
            if(mVideoSinks.size() > 0 || mAudioSinks.size() > 0) {
                throw new RuntimeException("logical error: can't release this scene, remove all outputs in it");
            }
            cleanAllVideoStream();

            mMediaEngine.removeVideoScene(ID);
            mMediaEngine = null;
            ID = -1;
        }

        public void autoShow(ScreenLayout.LayoutMode mode, ScreenLayout.FillPattern pattern) {
            int streamCnt = mVideoStreams.size();
            if (streamCnt > 0) {
                hideAllStreams();

                Integer[] ids = mVideoStreams.keySet().toArray(new Integer[streamCnt]);
                List<Integer> idss = java.util.Arrays.asList(ids);
                Collections.sort(idss);
                Integer[] streamIds = (Integer[]) idss.toArray();

                int subScreenCnt = ScreenLayout.getSubScreenCnt(mode, streamCnt);
                streamCnt = subScreenCnt > streamCnt ? streamCnt : subScreenCnt;
                String[] XYs = ScreenLayout.getLayouts(mode, subScreenCnt);

                setLayout(mode, subScreenCnt);
                for (int i = 0; i < streamCnt; ++i) {
                    mVideoStreams.get(streamIds[i]).setPosition(pattern, new Position(XYs[i]));
                }
            }
        }

        public void setLayout(ScreenLayout.LayoutMode mode, int subScreenCnt) {
            if (mode == ScreenLayout.LayoutMode.UNSPECIFIED)
                throw new RuntimeException("non-supported layout mode: " + mode);
            if (mCurrentMode == mode && mCurrentSubScreenCnt == subScreenCnt)
                return;

            mCurrentMode = mode;
            mCurrentSubScreenCnt = subScreenCnt;
            String bgName = ScreenLayout.getBackground(mCurrentMode, mCurrentSubScreenCnt);
            if (bgName != null)
                setBackgroundImage("file:///" + SAVE_PATH + "/" + bgName, true);
            else
                clearBackgroundImage(true);
        }

        public void setDisplayName(String name, int horizontal, int vertical, String styleSheet, boolean immediate) {
            DisplayName newName = new DisplayName(name, new Alignment(horizontal, vertical), styleSheet);
            if (mName.isEqual(newName))
                return;

            mName.update(newName);
            flushDisplayName(immediate);
        }

        public void displayName(boolean visible, boolean immediate) {
            if (visible == mNameVisible)
                return;

            mNameVisible = visible;
            flushDisplayName(immediate);
        }

        public void setBackgroundColor(String rgbCode, boolean immediate) {
            if (!rgbCode.startsWith(colorPrefix))
                throw new RuntimeException(String.format("Invalid RGB color code(%s) format. Must startwith '%s'", rgbCode, colorPrefix));
            if (mBackgroundColor == rgbCode)
                return;

            mBackgroundColor = rgbCode;
            if (immediate) {
                String[] cmd = {toBackgroundColorCmd()};
                postVideoSceneCmds(cmd);
                mBackgroundColorChanged = false;
            } else {
                mBackgroundColorChanged = true;
            }
        }

        public void setBackgroundImage(String url, boolean immediate) {
            if (!url.startsWith(filePrefix) && url != BACKGROUND_DEFAULT_IMAGE)
                throw new RuntimeException(String.format("Invalid file url(%s) format. Must startwith '%s'", url, filePrefix));
            if (mBackgroundImage == url)
                return;

            mBackgroundImage = url;
            if (immediate) {
                String[] cmd = {toBackgroundImageCmd()};
                postVideoSceneCmds(cmd);
                mBackgroundImageChanged = false;
            } else {
                mBackgroundImageChanged = true;
            }
        }

        public void clearBackgroundImage(boolean immediate) {
            setBackgroundImage(BACKGROUND_DEFAULT_IMAGE, immediate);
        }

        public void enableDisplayTestingItems(boolean able) {
            mMediaEngine.switchTestingItems(ID, able);
        }

        public void cleanVideoStream(int streamId) {
            VideoStreamRef streamRef = mVideoStreams.remove(streamId);
            if(streamRef != null) {
                streamRef.display(false, true);
                streamRef.mVideoStream.removeRef(this);
            }
        }

        public void cleanAllVideoStream() {
            for(VideoStreamRef streamRef: mVideoStreams.values()) {
                streamRef.display(false, true);
                streamRef.mVideoStream.removeRef(this);
            }
            mVideoStreams.clear();
        }

        public void hideAllStreams() {
            for (VideoStreamRef stream : mVideoStreams.values())
                stream.display(false, true);
        }

        public void displayStream(VideoStream stream, boolean visible, boolean immediate) {
            getStreamRef(stream).display(visible, immediate);
        }

        public void setStreamDisplayName(VideoStream stream, String name, int horizontal, int vertical, String styleSheet, boolean immediate) {
            getStreamRef(stream).setDisplayName(name, horizontal, vertical, styleSheet, immediate);
        }

        public void displayStreamName(VideoStream stream, boolean visible, boolean immediate) {
            getStreamRef(stream).displayName(visible, immediate);
        }

        public void setStreamPosition(VideoStream stream, ScreenLayout.FillPattern pattern, Position pos) {
            getStreamRef(stream).setPosition(pattern, pos);
        }

        public void cutStreamRegion(VideoStream stream, Region region) {
            getStreamRef(stream).cutRegion(region);
        }

        public void onStreamResolutionChanged(VideoStream stream, int newWidth, int newHeight) {
            getStreamRef(stream).onResolutionChanged(newWidth, newHeight);
        }

        public int getVideoSink(MediaEngine.VideoSinkConfig videoSinkConf) {
            int sinkId = -1;
            for (MediaSink<MediaEngine.VideoSinkConfig> sink : mVideoSinks.values()) {
                if (sink.config.isEqual(videoSinkConf)) {
                    sinkId = sink.id;
                    break;
                }
            }
            if (sinkId == -1) {
                sinkId = mMediaEngine.addVideoSink(ID);
                if (sinkId == -1)
                    throw new RuntimeException("MediaEngine.addVideoSink failed");
                mMediaEngine.configureVideoSink(sinkId, videoSinkConf);
                mVideoSinks.put(sinkId, new MediaSink<>(sinkId, videoSinkConf));
                sinkId = 0 - sinkId;
            }
            return sinkId;
        }

        public int getAudioSink(MediaEngine.AudioSinkConfig audioSinkConf) {
            int sinkId = -1;
            for (MediaSink<MediaEngine.AudioSinkConfig> sink : mAudioSinks.values()) {
                if (sink.config.isEqual(audioSinkConf)) {
                    sinkId = sink.id;
                    break;
                }
            }
            if (sinkId == -1) {
                sinkId = mMediaEngine.addAudioSink();
                if (sinkId == -1)
                    throw new RuntimeException("MediaEngine.addAudioSink failed");
                mMediaEngine.configureAudioSink(sinkId, audioSinkConf);
                mAudioSinks.put(sinkId, new MediaSink<>(sinkId, audioSinkConf));
                sinkId = 0 - sinkId;
            }
            return sinkId;
        }

        public void startVideoSink(int sinkId) {
            if (mVideoSinks.containsKey(sinkId)) {
                mMediaEngine.startVideoSink(sinkId);
            }
        }

        public void startAudioSink(int sinkId) {
            if (mAudioSinks.containsKey(sinkId)) {
                mMediaEngine.startAudioSink(sinkId);
            }
        }

        public void removeVideoSink(int sinkId) {
            if (mVideoSinks.containsKey(sinkId)) {
                mMediaEngine.stopVideoSink(sinkId);
                mMediaEngine.removeVideoSink(sinkId);
                mVideoSinks.remove(sinkId);
            }
        }

        public void removeAudioSink(int sinkId) {
            if (mAudioSinks.containsKey(sinkId)) {
                mMediaEngine.stopAudioSink(sinkId);
                mMediaEngine.removeAudioSink(sinkId);
                mAudioSinks.remove(sinkId);
            }
        }

        public void addVideoSinkOutput(int sinkId, int outputId, MediaEngine.SinkOutput output) {
            mMediaEngine.addVideoSinkOutput(sinkId, output);
            mVideoSinks.get(sinkId).outputs.add(outputId);
        }

        public void addAudioSinkOutput(int sinkId, int outputId, MediaEngine.SinkOutput output) {
            mMediaEngine.addAudioSinkOutput(sinkId, output);
            mAudioSinks.get(sinkId).outputs.add(outputId);
        }

        public void removeVideoSinkOutput(int sinkId, int outputId, MediaEngine.SinkOutput output) {
            if (sinkId == -1 || output == null)
                return;
            mMediaEngine.removeVideoSinkOutput(sinkId, output);
            MediaSink<MediaEngine.VideoSinkConfig> sink = mVideoSinks.get(sinkId);
            sink.outputs.remove(new Integer(outputId));
            if (sink.outputs.size() == 0)
                removeVideoSink(sink.id);
        }

        public void removeAudioSinkOutput(int sinkId, int outputId, MediaEngine.SinkOutput output) {
            if (sinkId == -1 || output == null)
                return;
            mMediaEngine.removeAudioSinkOutput(sinkId, output);
            MediaSink<MediaEngine.AudioSinkConfig> sink = mAudioSinks.get(sinkId);
            sink.outputs.remove(new Integer(outputId));
            if (sink.outputs.size() == 0)
                removeAudioSink(sink.id);
        }

        public void flush() {
            List<String> cmds = new ArrayList<>();

            if (mBackgroundColorChanged) {
                cmds.add(toBackgroundColorCmd());
                mBackgroundColorChanged = false;
            }

            if (mBackgroundImageChanged) {
                cmds.add(toBackgroundImageCmd());
                mBackgroundImageChanged = false;
            }

            if (mNameChanged) {
                cmds.addAll(toNameCmds());
                mNameChanged = false;
            }

            if (mFrozenChanged) {
                cmds.add(toFrozenCmd());
                mFrozenChanged = false;
            }

            if (cmds.size() > 0) {
                postVideoSceneCmds(cmds.toArray(new String[cmds.size()]));
            }
        }

        private VideoStreamRef getStreamRef(VideoStream stream) {
            VideoStreamRef streamRef = mVideoStreams.get(stream.ID);
            if(streamRef == null) {
                streamRef = new VideoStreamRef(stream);
                mVideoStreams.put(stream.ID, streamRef);
                stream.addRef(this);
            }
            return streamRef;
        }

        private void flushDisplayName(boolean immediate) {
            if (immediate) {
                List<String> cmds = toNameCmds();
                postVideoSceneCmds(cmds.toArray(new String[cmds.size()]));
                mNameChanged = false;
            } else {
                mNameChanged = true;
            }
        }

        private String toBackgroundColorCmd() {
            return String.format("set scene background-color %s", mBackgroundColor);
        }

        private String toBackgroundImageCmd() {
            String bg = mBackgroundImage;
            if (bg.startsWith(filePrefix))
                bg = bg.substring(bg.indexOf(filePrefix) + filePrefix.length());
            return String.format("set scene background-image %s", bg);
        }

        private List<String> toNameCmds() {
            return mName.toCmds("set scene", mNameVisible);
        }

        private String toFrozenCmd() {
            return String.format("set scene frozen %s", mFrozen ? "true" : "false");
        }

        private void postVideoSceneCmds(String[] cmds) {
            mMediaEngine.postVideoSceneCommands(ID, cmds);
            LogManager.d("postVideoSceneCmds(" + ID + "): [" + TextUtils.join(", ", cmds) + "]");
        }
    };

    private static MediaController gMediaController = null;

    private MediaEngine mMediaEngine = null;
    private IOEngine mIOEngine = new IOEngine();

    private List<Observer> mObservers = new ArrayList<>();

    private Scene[] mScenes = {null, null};
    private Map<Integer, Tuple<AudioStream, VideoStream>> mSource2Streams = new HashMap<>(MediaEngine.MAXIMUM_VIDEO_STREAM_COUNT);

    private Map<Integer, OutputSession> mOutputs = new HashMap<>();

    private Thread mThread = null;
    private boolean mRunning = false;
    private TaskHandler mTaskHandler = null;
    private Object mRunningLock = new Object();

    private Context mContext;

    private MediaController(Context context) {
        if (context != null) {
            mContext = context;
        }
        mMediaEngine = MediaEngine.allocateInstance(mContext);
        mMediaEngine.setCallback(this);
    }

    public int init(SurfaceHolder holder) {
        Scene scene = new Scene(mMediaEngine, holder.getSurface());
        scene.setBackgroundColor(BACKGROUND_DEFAULT_COLOR, false);
        scene.flush();
        mScenes[SCENE_IDX_LOCAL_DISPLAY] = scene;

        if (mThread == null) {
            mThread = new Thread(this, "MediaController");
            mThread.start();
            waitUntilReady();
        }
        return 0;
    }

    public int release() {

        clean();

        Scene scene = mScenes[SCENE_IDX_LOCAL_DISPLAY];
        if(scene != null) {
            scene.release();
            mScenes[SCENE_IDX_LOCAL_DISPLAY] = null;
        }

        if (mMediaEngine != null) {
            mMediaEngine.setCallback(null);
            mMediaEngine = null;
        }

        if (mThread != null) {
            mTaskHandler.sendStopLoop(0);
            waitUntilOver();
            try {
                mThread.join(THREAD_EXIT_TIMEOUT_MS);
            } catch (InterruptedException e) {
                LogManager.e(e);
            }
            mThread = null;
        }

        mIOEngine = null;
        return 0;
    }

    public void cleanSources() {
        for (int id : mSource2Streams.keySet().toArray(new Integer[mSource2Streams.size()]))
            removeSource(id);
    }

    public void cleanOutputs() {
        for (int id : mOutputs.keySet().toArray(new Integer[mOutputs.size()]))
            removeOutput(id);
    }

    public void clean() {
        //TODO: release other scene
        releaseExtraScene(SCENE_IDX_SEND_TO_REMOTE);

        cleanOutputs();

        // clean the sources in these scenes bofore cleanSources
        for(int i = 0 ; i < mScenes.length ; ++i) {
            Scene scene = mScenes[i];
            if(scene != null) {
                scene.cleanAllVideoStream();
            }
        }
        cleanSources();
    }

    public void addObserver(Observer ob) {
        synchronized (mObservers) {
            if (!mObservers.contains(ob))
                mObservers.add(ob);
        }
    }

    public void removeObserver(Observer ob) {
        synchronized (mObservers) {
            if (mObservers.contains(ob))
                mObservers.remove(ob);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////// Methods of Source
    ///////////////////////////////////////////////////////////////////////////

    /** Add a source.
     *  For LocalFile(MP4), url looks like: "http://10.1.0.75/test-3.mp4?loop=true"
     *  For IPCamera(RTSP), url looks like: "rtsp://10.3.0.114:5000/main.h264"
     *  For MediaCapture, url looks like: "capture://<path>?type=<data-type>&id=<device-id-in-android>&name=<device-name>&description=<display-content>"
     */
    public int addSource(String url, int reopenCnt) {
        return addSource(url, (List<DataType>)null, reopenCnt);
    }
    public int addSource(String url, List<DataType> types, int reopenCnt) {
        return addSource(url, types, null, reopenCnt);
    }
    public int addSource(String url, Map<Integer/*trackId*/, Tuple3<DataType, MediaFormat, AVChannel>> tracks, int reopenCnt) {
        return addSource(url, null, tracks, reopenCnt);
    }
    private int addSource(String url, List<DataType> types, Map<Integer/*trackId*/, Tuple3<DataType, MediaFormat, AVChannel>> tracks, int reopenCnt) {
        LogManager.i("addSource: " + url);
        try {
            int sourceId;
            if (MediaCapture.isCapture(url)) {
                sourceId = mIOEngine.addDummySession();
            } else {
                sourceId = mIOEngine.addSession(url, IOEngine.IOFlags.IO_RD, this, tracks);
            }

            if(!mTaskHandler.sendDoAddSource(new AddingSourceParams(url, types, sourceId, reopenCnt, TASK_DELAY_MS))) {
                mIOEngine.removeSession(sourceId);
                return -1;
            }
            return sourceId;
        } catch (RuntimeException e) {
            LogManager.e(String.format("IOEngine create session[url=%s] error: %s", url, e.getMessage()));
            e.printStackTrace();
            return -1;
        }
    }

    public int removeSource(int sourceId) {
        for(int i = 0 ; i < mScenes.length ; ++i) {
            if(mScenes[i] != null) {
                cleanSceneSource(i, sourceId);
            }
        }
        // Remove the source and stream
        mIOEngine.removeSession(sourceId);
        synchronized (mSource2Streams) {
            if (mSource2Streams.containsKey(sourceId)) {
                Tuple<AudioStream, VideoStream> streams = mSource2Streams.remove(sourceId);
                if(streams.first != null)
                    streams.first.release();
                if(streams.second != null)
                    streams.second.release();
            }
        }
        return 0;
    }

    public MediaFormat getSourceVideoFormat(int sourceId) {
        return getSourceFormat(sourceId, DataType.VIDEO);
    }
    public MediaFormat getSourceAudioFormat(int sourceId) {
        return getSourceFormat(sourceId, DataType.AUDIO);
    }
    public MediaFormat getSourceFormat(int sourceId, DataType type) {
        synchronized (mSource2Streams) {
            Tuple<AudioStream, VideoStream> streams = mSource2Streams.get(sourceId);
            if (streams == null) {
                LogManager.w(String.format("the source[%d] had not been added successfully", sourceId));
                return null;
            }
            if(type == DataType.VIDEO) {
                VideoStream stream = streams.second;
                if (stream == null) {
                    LogManager.w(String.format("the source[%d]-video had not been added successfully", sourceId));
                    return null;
                }
                if(stream.isCapture()) {
                    LogManager.w(String.format("TODO: get format from capture[%d]", sourceId));
                    return null;
                }

                IOEngine.TrackInfo[] trackInfos = mIOEngine.getTrackInfos(sourceId);
                for (IOEngine.TrackInfo info : trackInfos) {
                    if (info.type == type)
                        return info.format;
                }
                return null;
            } else if(type == DataType.AUDIO) {
                AudioStream stream = streams.first;
                if (stream == null) {
                    LogManager.w(String.format("the source[%d]-audio had not been added successfully", sourceId));
                    return null;
                }
                /*
                if(stream.isCapture()) {
                    LogManager.w(String.format("TODO: get format from capture[%d]", sourceId));
                    return null;
                }
                */

                IOEngine.TrackInfo[] trackInfos = mIOEngine.getTrackInfos(sourceId);
                for (IOEngine.TrackInfo info : trackInfos) {
                    if (info.type == type)
                        return info.format;
                }
                return null;
            }
            return null;
        }
    }

    public MediaFormat getSourceVideoStatistics(int sourceId) {
        return getSourceStatistics(sourceId, DataType.VIDEO);
    }
    public MediaFormat getSourceAudioStatistics(int sourceId) {
        return getSourceStatistics(sourceId, DataType.AUDIO);
    }
    public MediaFormat getSourceStatistics(int sourceId, DataType type) {
        synchronized (mSource2Streams) {
            Tuple<AudioStream, VideoStream> streams = mSource2Streams.get(sourceId);
            if (streams == null) {
                LogManager.w(String.format("the source[%d] had not been added successfully", sourceId));
                return null;
            }
            if(type == DataType.VIDEO) {
                VideoStream stream = streams.second;
                if (stream == null) {
                    LogManager.w(String.format("the source[%d]-video had not been added successfully", sourceId));
                    return null;
                }

                if(stream.isCapture()) {
                    LogManager.w(String.format("TODO: get format from capture[%d]", sourceId));
                    return null;
                }
                return stream.getStatistics();
            } else if(type == DataType.AUDIO) {
                AudioStream stream = streams.first;
                if (stream == null) {
                    LogManager.w(String.format("the source[%d]-audio had not been added successfully", sourceId));
                    return null;
                }
                LogManager.w(String.format("TODO: get format from audio source[%d]", sourceId));
                return null;
            }
            return null;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////// Methods of Scene
    ///////////////////////////////////////////////////////////////////////////

    /* 创建额外的画面,如发送给远程的 */
    public void createExtraScene(int sceneIdx) {
        LogManager.e("!!!!Tuyj Debug here");
//        Scene scene = mScenes[sceneIdx];
//        if(scene == null) {
//            scene = new Scene(mMediaEngine, null);
//            scene.setBackgroundColor(BACKGROUND_DEFAULT_COLOR, false);
//            scene.flush();
//            mScenes[sceneIdx] = scene;
//        }
    }
    public void releaseExtraScene(int sceneIdx) {
        LogManager.e("!!!!Tuyj Debug here");
//        if(sceneIdx == SCENE_IDX_LOCAL_DISPLAY) {
//            throw new RuntimeException("logical error: can't release local display scene");
//        }
//        Scene scene = mScenes[sceneIdx];
//        if(scene != null) {
//            scene.release();
//            mScenes[sceneIdx] = null;
//        }
    }

    /* 根据分屏模式和当前源数量自动展示默认(本地)画面 */
    public void autoShow(ScreenLayout.LayoutMode mode, ScreenLayout.FillPattern pattern) {
        autoShow(SCENE_IDX_LOCAL_DISPLAY, mode, pattern);
    }
    /* 根据分屏模式和当前源数量自动展示指定画面 */
    public void autoShow(int sceneIdx, ScreenLayout.LayoutMode mode, ScreenLayout.FillPattern pattern) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.autoShow(mode, pattern);
    }

    /** 设置布局模式
     *  subScreenCnt -- 对于画中画,为小画面位置编号(1-4);其他情况为子分屏数
     * */
    /* 设置默认(本地)画面上的布局 */
    public void setLayout(ScreenLayout.LayoutMode mode, int subScreenCnt) {
        setLayout(SCENE_IDX_LOCAL_DISPLAY, mode, subScreenCnt);
    }
    /* 设置指定画面上的布局 */
    public void setLayout(int sceneIdx, ScreenLayout.LayoutMode mode, int subScreenCnt) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.setLayout(mode, subScreenCnt);
    }

    /* 设置默认(本地)画面上的字幕 */
    public void setDisplayName(String name, int horizontal, int vertical, String styleSheet) {
        setDisplayName(SCENE_IDX_LOCAL_DISPLAY, name, horizontal, vertical, styleSheet);
    }
    /* 设置指定画面上的字幕 */
    public void setDisplayName(int sceneIdx, String name, int horizontal, int vertical, String styleSheet) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.setDisplayName(name, horizontal, vertical, styleSheet, false);
        scene.displayName(true, true);
    }

    /* 在默认(本地)画面上(不)显示字幕 */
    public void displayName(boolean visible) {
        displayName(SCENE_IDX_LOCAL_DISPLAY, visible);
    }
    /* 在指定画面上(不)显示字幕 */
    public void displayName(int sceneIdx, boolean visible) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.displayName(visible, true);
    }

    /* 设置默认(本地)画面上的背景图 */
    public void setBackgroundImage(String url) {
        setBackgroundImage(SCENE_IDX_LOCAL_DISPLAY, url);
    }
    /* 设置指定画面上的背景图 */
    public void setBackgroundImage(int sceneIdx, String url) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.setBackgroundImage(url, true);
    }

    /* 清除默认(本地)画面上的背景图 */
    public void clearBackgroundImage() {
        clearBackgroundImage(SCENE_IDX_LOCAL_DISPLAY);
    }
    /* 清除指定画面上的背景图 */
    public void clearBackgroundImage(int sceneIdx) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.clearBackgroundImage(true);
    }

    /* 在默认(本地)画面上(不)显示测试动画 */
    public void enableDisplayTestingItems(boolean able) {
        enableDisplayTestingItems(SCENE_IDX_LOCAL_DISPLAY, able);
    }
    /* 在指定画面上(不)显示测试动画 */
    public void enableDisplayTestingItems(int sceneIdx, boolean able) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.enableDisplayTestingItems(able);
    }

    /* 添加源到默认(本地)画面上 */
    public int addSource2Scene(int sourceId) {
        return addSource2Scene(SCENE_IDX_LOCAL_DISPLAY, sourceId);
    }
    /* 添加源指定画面上 */
    public int addSource2Scene(int sceneIdx, int sourceId) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.getStreamRef(stream);
            return 0;
        }
    }

    /* 清除默认(本地)画面上的所有源 */
    public void cleanSceneSources() {
        cleanSceneSources(SCENE_IDX_LOCAL_DISPLAY);
    }
    /* 清除指定画面上的所有源 */
    public void cleanSceneSources(int sceneIdx) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.cleanAllVideoStream();
    }

    /* 在默认(本地)画面上清除指定源 */
    public int cleanSceneSource(int sourceId) {
        return cleanSceneSource(SCENE_IDX_LOCAL_DISPLAY, sourceId);
    }
    /* 在指定画面上清除指定源 */
    public int cleanSceneSource(int sceneIdx, int sourceId) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.cleanVideoStream(stream.ID);
            return 0;
        }
    }

    /* 隐藏默认(本地)画面上的所有源 */
    public void hideSceneSources() {
        hideSceneSources(SCENE_IDX_LOCAL_DISPLAY);
    }
    /* 隐藏指定画面上的所有源 */
    public void hideSceneSources(int sceneIdx) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return;
        }
        scene.hideAllStreams();
    }

    /* 在默认(本地)画面上(不)显示指定源 */
    public int displaySource(int sourceId, boolean visible) {
        return displaySource(SCENE_IDX_LOCAL_DISPLAY, sourceId, visible);
    }
    /* 在指定画面上(不)显示指定源 */
    public int displaySource(int sceneIdx, int sourceId, boolean visible) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.displayStream(stream, visible, true);
            return 0;
        }
    }
    /* 在所有画面上(不)显示指定源 */
    public int displaySourceAnyWhere(int sourceId, boolean visible) {
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            stream.display(visible, true);
            return 0;
        }
    }

    /* 修改默认(本地)画面上源的显示名 */
    public int setSourceDisplayName(int sourceId, String name, int horizontal, int vertical, String styleSheet) {
        return setSourceDisplayName(SCENE_IDX_LOCAL_DISPLAY, sourceId, name, horizontal, vertical, styleSheet);
    }
    /* 修改指定画面上源的显示名 */
    public int setSourceDisplayName(int sceneIdx, int sourceId, String name, int horizontal, int vertical, String styleSheet) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.setStreamDisplayName(stream, name, horizontal, vertical, styleSheet, false);
            scene.displayStreamName(stream, true, true);
            return 0;
        }
    }
    /* 修改所有画面上该源的显示名 */
    public int setSourceDisplayNameAnyWhere(int sourceId, String name, int horizontal, int vertical, String styleSheet) {
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            stream.setDisplayName(name, horizontal, vertical, styleSheet, false);
            stream.displayName(true, true);
            return 0;
        }
    }

    /* (不)显示默认(本地)画面上源的显示名 */
    public int displaySourceName(int sourceId, boolean visible) {
        return displaySourceName(SCENE_IDX_LOCAL_DISPLAY, sourceId, visible);
    }
    /* (不)显示指定画面上源的显示名 */
    public int displaySourceName(int sceneIdx, int sourceId, boolean visible) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.displayStreamName(stream, visible, true);
            return 0;
        }
    }
    /* (不)显示所有画面上源的显示名 */
    public int displaySourceNameAnyWhere(int sourceId, boolean visible) {
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            stream.displayName(visible, true);
            return 0;
        }
    }

    /* 在默认(本地)画面上 以默认(适应/充满)模式设置源位置 */
    public int setSourcePosition(int sourceId, String pos) {
        return setSourcePosition(sourceId, ScreenLayout.FillPattern.FILL_PATTERN_STRETCHED, new Position(pos));
    }
    /* 在默认(本地)画面上 以默认(适应/充满)模式设置源位置 */
    public int setSourcePosition(int sourceId, Position pos) {
        return setSourcePosition(sourceId, ScreenLayout.FillPattern.FILL_PATTERN_STRETCHED, pos);
    }
    /* 在默认(本地)画面上 设置源位置 */
    public int setSourcePosition(int sourceId, ScreenLayout.FillPattern pattern, String pos) {
        return setSourcePosition(sourceId, pattern, new Position(pos));
    }
    /* 在默认(本地)画面上 设置源位置 */
    public int setSourcePosition(int sourceId, ScreenLayout.FillPattern pattern, Position pos) {
        return setSourcePosition(SCENE_IDX_LOCAL_DISPLAY, sourceId, pattern, pos);
    }
    /* 在指定画面上 以适应/充满模式设置源位置 */
    public int setSourcePosition(int sceneIdx, int sourceId, String pos) {
        return setSourcePosition(sceneIdx, sourceId, ScreenLayout.FillPattern.FILL_PATTERN_STRETCHED, new Position(pos));
    }
    /* 在指定画面上 以适应/充满模式设置源位置 */
    public int setSourcePosition(int sceneIdx, int sourceId, Position pos) {
        return setSourcePosition(sceneIdx, sourceId, ScreenLayout.FillPattern.FILL_PATTERN_STRETCHED, pos);
    }
    /* 在指定画面上 设置源位置 */
    public int setSourcePosition(int sceneIdx, int sourceId, ScreenLayout.FillPattern pattern, String pos) {
        return setSourcePosition(sceneIdx, sourceId, pattern, new Position(pos));
    }
    /* 在指定画面上 设置源位置 */
    public int setSourcePosition(int sceneIdx, int sourceId, ScreenLayout.FillPattern pattern, Position pos) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.setStreamPosition(stream, pattern, pos);
            return 0;
        }
    }

    /* 裁剪默认(本地)画面上的源 */
    public int cutSourceRegion(int sourceId, String region) {
        return cutSourceRegion(sourceId, new Region(region));
    }
    /* 裁剪默认(本地)画面上的源 */
    public int cutSourceRegion(int sourceId, Region region) {
        return cutSourceRegion(SCENE_IDX_LOCAL_DISPLAY, sourceId, region);
    }
    /* 裁剪指定画面上的源 */
    public int cutSourceRegion(int sceneIdx, int sourceId, String region) {
        return cutSourceRegion(sceneIdx, sourceId, new Region(region));
    }
    /* 裁剪指定画面上的源 */
    public int cutSourceRegion(int sceneIdx, int sourceId, Region region) {
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        synchronized (mSource2Streams) {
            VideoStream stream = mSource2Streams.get(sourceId).second;
            if (stream == null) {
                LogManager.w("MediaController has no this source: " + sourceId);
                return -1;
            }
            scene.cutStreamRegion(stream, region);
            return 0;
        }
    }

    /* 在默认(本地)画面上 添加输出 */
    public int addOutput(String url, MediaEngine.VideoSinkConfig videoSinkConf, int reopenCnt) {
        return addOutput(url, videoSinkConf, null, reopenCnt);
    }
    /* 在默认(本地)画面上 添加输出 */
    public int addOutput(String url, MediaEngine.AudioSinkConfig audioSinkConf, int reopenCnt) {
        return addOutput(url, null, audioSinkConf, reopenCnt);
    }
    /* 在默认(本地)画面上 添加输出 */
    public int addOutput(String url, MediaEngine.VideoSinkConfig videoSinkConf, MediaEngine.AudioSinkConfig audioSinkConf, int reopenCnt) {
        return addOutput(SCENE_IDX_LOCAL_DISPLAY, url, videoSinkConf, audioSinkConf, reopenCnt);
    }
    /* 在指定画面上 添加输出 */
    public int addOutput(int sceneIdx, String url, MediaEngine.VideoSinkConfig videoSinkConf, int reopenCnt) {
        return addOutput(sceneIdx, url, videoSinkConf, null, reopenCnt);
    }
    /* 在指定画面上 添加输出 */
    public int addOutput(int sceneIdx, String url, MediaEngine.AudioSinkConfig audioSinkConf, int reopenCnt) {
        return addOutput(sceneIdx, url, null, audioSinkConf, reopenCnt);
    }
    /* 在指定画面上 添加输出 */
    public int addOutput(int sceneIdx, String url, MediaEngine.VideoSinkConfig videoSinkConf, MediaEngine.AudioSinkConfig audioSinkConf, int reopenCnt) {
        return addOutput(sceneIdx, url, null, videoSinkConf, audioSinkConf, reopenCnt);
    }
    /* 在指定画面上 添加输出 */
    public int addOutput(int sceneIdx, String url,
                         Map<Integer/*trackId*/, Pair<DataType, AVChannel>> tracks,
                         MediaEngine.VideoSinkConfig videoSinkConf,
                         MediaEngine.AudioSinkConfig audioSinkConf,
                         int reopenCnt) {
        LogManager.i("addOutput: " + url + " on scene-idx-" + sceneIdx);
        Scene scene = mScenes[sceneIdx];
        if(scene == null) {
            LogManager.w("logical error: can't find the scene-idx-" + sceneIdx);
            return -1;
        }
        if (videoSinkConf != null && !videoSinkConf.isValid()) {
            LogManager.e("videoSinkConf is invalid");
            return -1;
        }
        if (audioSinkConf != null && !audioSinkConf.isValid()) {
            LogManager.e("audioSinkConf is invalid");
            return -1;
        }

        try {
            int outputId = mIOEngine.addSession(url, IOEngine.IOFlags.IO_WR, this, tracks);
            MediaEngine.VideoSinkConfig vcfg = null;
            MediaEngine.AudioSinkConfig acfg = null;
            if (videoSinkConf != null) {
                vcfg = new MediaEngine.VideoSinkConfig();
                vcfg.from(videoSinkConf);
            }
            if (audioSinkConf != null) {
                acfg = new MediaEngine.AudioSinkConfig();
                acfg.from(audioSinkConf);
            }
            if(!mTaskHandler.sendDoAddOutput(new AddingOutputParams(sceneIdx, url, outputId, vcfg, acfg, reopenCnt, TASK_DELAY_MS))) {
                mIOEngine.removeSession(outputId);
                return -1;
            }
            return outputId;
        } catch (RuntimeException e) {
            LogManager.e(String.format("IOEngine create session[url=%s] error:", url));
            e.printStackTrace();
            return -1;
        }
    }

    /* 从对应画面上移除指定输出 */
    public int removeOutput(int outputId) {
        OutputSession output = mOutputs.remove(outputId);
        if (output != null) {
            Scene scene = mScenes[output.sceneIdx];
            if(scene != null) {
                scene.removeVideoSinkOutput(output.videoSinkId, output.id, output.videoOutput);
                scene.removeAudioSinkOutput(output.audioSinkId, output.id, output.audioOutput);
            }
        }
        mIOEngine.removeSession(outputId);
        return 0;
    }

    public void changeSurface(SurfaceHolder holder, int format, int width, int height) {
        LogManager.d("surfaceChanged fmt=" + format + " size=" + width + "x" + height + " surface=" + holder);
        mMediaEngine.notifyDisplaySurfaceChanged(mScenes[SCENE_IDX_LOCAL_DISPLAY].ID, format, width, height);
    }

    private class AddingSourceParams {
        AddingSourceParams(String url, List<DataType> types, int sourceId, int reopenCnt, long delayMs) {
            this(url, types, sourceId, reopenCnt, delayMs, null, null);
        }
        AddingSourceParams(String url, List<DataType> types, int sourceId, int reopenCnt, long delayMs, Object obj1, Object obj2) {
            this.url = url; this.types = types; this.sourceId = sourceId;
            this.reopenCnt = reopenCnt; this.delayMs = delayMs;
            this.obj1 = obj1; this.obj2 = obj2;
        }
        public String url;
        public List<DataType> types;
        public int sourceId;
        public int reopenCnt;
        public long delayMs;
        public Object obj1;
        public Object obj2;
    };

    private void doAddSource(AddingSourceParams params) {
        int sourceId = params.sourceId;
        String url = params.url;

        if(MediaCapture.isCapture(url)) {
            doAddCapture(params);
            return;
        }

        try {
            /** async open source -> onOpening -> onOpenSourceSuccess -> ... -> async start source
                                               -> onOpenSourceFailed -> send `AddSource` task again  OR   onAddSourceFailed
            * */
            LogManager.i(String.format("try to async open source(%d) :%s", sourceId, url));
            mIOEngine.asyncOpenSession(sourceId, new IOEngine.IOSession.asyncActionCallback(params) {
                @Override
                public void onOpening(int err) {
                    AddingSourceParams params = (AddingSourceParams) getExtParams();
                    if (err == 0) {
                        mTaskHandler.sendOnOpenSourceSuccess(params);
                    } else {
                        mTaskHandler.sendOnOpenSourceFailed(params, err);
                    }
                }
            });
        } catch(RuntimeException e) {
            e.printStackTrace();
            onAddSourceFailed(sourceId, url, -1, String.format("Open source[id=%d url=%s] failed: %s", sourceId, url, e.getMessage()));
        }
    }

    private void doAddCapture(AddingSourceParams params) {
        int sourceId = params.sourceId;
        String url = params.url;
        List<DataType> types = params.types;

        LogManager.i(String.format("Do add capture(%d) :%s", sourceId, url));
        try {
            MediaCapture capture = new MediaCapture(sourceId, url);
            if(types != null && !types.contains(capture.Type))
                throw new RuntimeException("there is no expected data in the capture");
            if(capture.Type != DataType.VIDEO)
                throw new RuntimeException("TODO: just support add video capture");
            VideoStream stream = new VideoStream(mMediaEngine, url, capture);
            synchronized (mSource2Streams) {
                mSource2Streams.put(sourceId, new Tuple<AudioStream, VideoStream>(null, stream));
            }
            onAddSourceSuccess(sourceId, url);
        } catch(RuntimeException e) {
            e.printStackTrace();
            onAddSourceFailed(sourceId, url, -3, String.format("Add capture[id=%d url=%s] failed: %s", sourceId, url, e.getMessage()));
        }
    }

    private void onOpenSourceSuccess(AddingSourceParams params) {
        int sourceId = params.sourceId;
        String url = params.url;
        List<DataType> types = params.types;
        LogManager.i(String.format("open source(%d) successfully: %s", sourceId, url));

        // find expected track
        int video_track = -1;
        int audio_track = -1;
        IOEngine.TrackInfo[] trackInfos = mIOEngine.getTrackInfos(sourceId);
        for (IOEngine.TrackInfo info : trackInfos) {
            if(types != null && !types.contains(info.type))
                continue;
            if(info.type == DataType.VIDEO || info.type == DataType.VIDEO_EXT) {
                video_track = info.trackId;
            } else if(info.type == DataType.AUDIO) {
                audio_track = info.trackId;
            } else {
                LogManager.w("non-supported media type:" + info.type.toString());
            }
        }
        if(video_track == -1 && audio_track == -1) {
            onAddSourceFailed(sourceId, url, -1, String.format("Invalid source[id=%d url=%s] has no audio or video stream", sourceId, url));
            return;
        }

        // create stream
        AudioStream audio = null;
        VideoStream video = null;
        if(audio_track >= 0) {
            IOEngine.IOChannel ioChannel = mIOEngine.getTrackIO(sourceId, audio_track);
            audio = new AudioStream(audio_track, mMediaEngine, url, ioChannel.rChannel);
        }
        if(video_track >= 0) {
            IOEngine.IOChannel ioChannel = mIOEngine.getTrackIO(sourceId, video_track);
            video = new VideoStream(video_track, mMediaEngine, url, ioChannel.rChannel);
        }

        // start IOSession
        try {
            /*  async start source -> onStartSourceSuccess -> keep stream and notify observers
                                   -> onStartSourceFailed -> onAddSourceFailed
            * */
            LogManager.i(String.format("try to async start source(%d) :%s", sourceId, url));
            params.obj1 = new Tuple<>(audio, video);
            mIOEngine.asyncStartSession(sourceId, new IOEngine.IOSession.asyncActionCallback(params) {
                @Override
                public void onStarting(int err) {
                    AddingSourceParams params = (AddingSourceParams) getExtParams();
                    if (err == 0) {
                        mTaskHandler.sendOnStartSourceSuccess(params);
                    } else {
                        mTaskHandler.sendOnStartSourceFailed(params, err);
                    }
                }
            });
        } catch(RuntimeException e) {
            audio.release();
            video.release();
            onAddSourceFailed(sourceId, url, -1, String.format("Start source[id=%d url=%s] failed: %s", sourceId, url, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void onOpenSourceFailed(AddingSourceParams params, int err) {
        int sourceId = params.sourceId;
        String url = params.url;

        if(params.reopenCnt == REOPEN_CNT_FORBIDDEN || !mIOEngine.retryForOpenFailed(sourceId)) {
            onAddSourceFailed(sourceId, url, err, String.format("Open source[id=%d url=%s] failed", sourceId, url));
        } else {
            mIOEngine.closeSession(sourceId);
            if(params.reopenCnt != REOPEN_CNT_FOREVER)
                --params.reopenCnt;
            params.delayMs += RETRY_STEP_INTERVAL_MS;
            if(params.delayMs > RETRY_MAX_INTERVAL_MS) params.delayMs = RETRY_MAX_INTERVAL_MS;
            if(!mTaskHandler.sendDoAddSource(params)) {
                onAddSourceFailed(sourceId, url, err, String.format("Open source[id=%d url=%s] failed, fail to try again", sourceId, url));
            } else {
                onAddSourceFailed(sourceId, url, 1, String.format("Open source[id=%d url=%s] failed, try again", sourceId, url));
            }
        }
        return;
    }

    private void onStartSourceSuccess(AddingSourceParams params) {
        LogManager.i(String.format("start source(%d) successfully: %s", params.sourceId, params.url));
        Tuple<AudioStream, VideoStream> streams = (Tuple<AudioStream, VideoStream>) params.obj1;
        synchronized(mSource2Streams) {
            mSource2Streams.put(params.sourceId, streams);
        }
        onAddSourceSuccess(params.sourceId, params.url);
    }

    private void onStartSourceFailed(AddingSourceParams params, int err) {
        Tuple<AudioStream, VideoStream> streams = (Tuple<AudioStream, VideoStream>) params.obj1;
        if(streams.first != null)
            streams.first.release();
        if(streams.second != null)
            streams.second.release();
        onAddSourceFailed(params.sourceId, params.url, err, String.format("Start source[id=%d url=%s] failed: %d", params.sourceId, params.url, err));
    }

    private void doReOpenSource(AddingSourceParams params, int err) {
        int sourceId = params.sourceId;
        String url = params.url;

        if(params.reopenCnt == REOPEN_CNT_FORBIDDEN || !mIOEngine.retryForOpenFailed(sourceId)) {
            onSourceLost(sourceId, url, err, String.format("Source(id-%d url-%s) stream lost: %s", sourceId, url, err));
            return;
        }

        try {
            /*  async reopen source -> onOpening -> onReOpenSourceSuccess
                                                 -> onReOpenSourceFailed -> send `DoSourceReOpen` task again  OR  onSourceLost
            * */
            LogManager.i(String.format("try to async reopen source(%d) :%s", sourceId, url));
            mIOEngine.asyncReOpenSession(sourceId, new IOEngine.IOSession.asyncActionCallback(params) {
                @Override
                public void onOpening(int err) {
                    AddingSourceParams params = (AddingSourceParams) getExtParams();
                    if (err == 0) {
                        mTaskHandler.sendOnReOpenSourceSuccess(params);
                    } else {
                        mTaskHandler.sendOnReOpenSourceFailed(params, err);
                    }
                }
            });
        } catch(RuntimeException e) {
            e.printStackTrace();
            onSourceLost(sourceId, url, err, String.format("Source(id-%d url-%s) stream lost: %s, fail to try again", sourceId, url, err));
        }
    }

    private void onReOpenSourceSuccess(AddingSourceParams params) {
        LogManager.i(String.format("reopen source(%d) successfully: %s", params.sourceId, params.url));
    }

    private void onReOpenSourceFailed(AddingSourceParams params, int err) {
        int sourceId = params.sourceId;
        String url = params.url;

        if(params.reopenCnt != REOPEN_CNT_FOREVER)
            --params.reopenCnt;
        if(params.reopenCnt == REOPEN_CNT_FORBIDDEN || !mIOEngine.retryForOpenFailed(sourceId)) {
            onSourceLost(sourceId, url, err, String.format("Source(id-%d url-%s) stream lost: %s", sourceId, url, err));
            return;
        }

        params.delayMs += RETRY_STEP_INTERVAL_MS;
        if(params.delayMs > RETRY_MAX_INTERVAL_MS) params.delayMs = RETRY_MAX_INTERVAL_MS;
        if(!mTaskHandler.sendDoSourceReOpen(params, err)) {
            onSourceLost(sourceId, url, err, String.format("Source(id-%d url-%s) stream lost: %s, fail to try again", sourceId, url, err));
        } else {
            LogManager.e(String.format("Source(id-%d url-%s) lost. Try to reopen", sourceId, url));
        }
    }

    private void onAddSourceSuccess(int sourceId, String url) {
        synchronized(mObservers) {
            for(Observer ob: mObservers)
                ob.onSourceAdded(sourceId, url, 0);
        }
    }

    private void onAddSourceFailed(int sourceId, String url, int err, String msg) {
        LogManager.e(msg);
        synchronized (mObservers) {
            for (Observer ob : mObservers)
                ob.onSourceAdded(sourceId, url, err);
        }
    }

    private void onSourceLost(int sourceId, String url, int err, String msg) {
        LogManager.e(msg);
        synchronized (mObservers) {
            for (Observer ob : mObservers)
                ob.onSourceLost(sourceId, url, err);
        }
    }

    private class AddingOutputParams {
        AddingOutputParams(int sceneIdx, String url, int outputId, MediaEngine.VideoSinkConfig videoSinkConf, MediaEngine.AudioSinkConfig audioSinkConf, int reopenCnt, long delayMs) {
            this(sceneIdx, url, outputId, videoSinkConf, audioSinkConf, reopenCnt, delayMs, null, null);
        }
        AddingOutputParams(int sceneIdx, String url, int outputId, MediaEngine.VideoSinkConfig videoSinkConf, MediaEngine.AudioSinkConfig audioSinkConf, int reopenCnt, long delayMs, Object obj1, Object obj2) {
            this.sceneIdx = sceneIdx; this.url = url; this.outputId = outputId;
            this.videoSinkConf = videoSinkConf; this.audioSinkConf = audioSinkConf;
            this.reopenCnt = reopenCnt; this.delayMs = delayMs;
            this.obj1 = obj1; this.obj2 = obj2;
        }
        public int sceneIdx;
        public String url;
        public int outputId;
        public MediaEngine.VideoSinkConfig videoSinkConf;
        public MediaEngine.AudioSinkConfig audioSinkConf;
        public int reopenCnt;
        public long delayMs;
        public Object obj1;
        public Object obj2;
    }

    private void doAddOutput(AddingOutputParams params) {
        int outputId = params.outputId;
        String url = params.url;

        // config output format
        OutputSession output = new OutputSession(outputId, params.sceneIdx);
        cfgVideoOutput(output, params.videoSinkConf);
        cfgAudioOutput(output, params.audioSinkConf);

        try {
            /*  async open output -> onOpening -> onOpenOutputSuccess -> ... -> async start output
                                               -> onOpenOutputFailed -> send `AddOutput` task again  OR  onAddOutputFailed
            * */
            LogManager.i(String.format("try to async open output(%d) :%s", outputId, url));
            params.obj1 = output;
            mIOEngine.asyncOpenSession(outputId, new IOEngine.IOSession.asyncActionCallback(params) {
                @Override
                public void onOpening(int err) {
                    AddingOutputParams params = (AddingOutputParams) getExtParams();
                    if (err == 0) {
                        mTaskHandler.sendOnOpenOutputSuccess(params);
                    } else {
                        mTaskHandler.sendOnOpenOutputFailed(params, err);
                    }
                }
            });
        } catch(RuntimeException e) {
            e.printStackTrace();
            onAddOutputFailed(outputId, url, -1, String.format("Open output[id=%d url=%s] failed: %s", outputId, url, e.getMessage()));
        }
    }

    private class SinkStruct {
        public SinkStruct(int videoSinkId, boolean newVideoSink, int audioSinkId, boolean newAudioSink) {
            this.videoSinkId    = videoSinkId ; this.audioSinkId    = audioSinkId ;
            this.newVideoSink   = newVideoSink; this.newAudioSink   = newAudioSink;
        }
        public int videoSinkId = -1;
        public int audioSinkId = -1;
        public boolean newVideoSink = false;
        public boolean newAudioSink = false;
    };

    private void onOpenOutputSuccess(AddingOutputParams params) {
        int outputId = params.outputId;
        String url = params.url;
        MediaEngine.VideoSinkConfig videoSinkConf = params.videoSinkConf;
        MediaEngine.AudioSinkConfig audioSinkConf = params.audioSinkConf;
        OutputSession output = (OutputSession) params.obj1;
        Scene scene = mScenes[params.sceneIdx]; //TODO: check scene
        LogManager.i(String.format("open output(%d) successfully: %s", outputId, url));

        // link output with sink
        int videoSinkId = -1;
        int audioSinkId = -1;
        boolean newVideoSink = false;
        boolean newAudioSink = false;
        if (videoSinkConf != null) {
            videoSinkId = scene.getVideoSink(videoSinkConf);
            if (videoSinkId < 0) {
                videoSinkId = 0 - videoSinkId;
                newVideoSink = true;
            }
            linkVideoOutput(scene, output, videoSinkId);
        }
        if (audioSinkConf != null) {
            audioSinkId = scene.getAudioSink(audioSinkConf);
            if (audioSinkId < 0) {
                audioSinkId = 0 - audioSinkId;
                newAudioSink = true;
            }
            linkAudioOutput(scene, output, audioSinkId);
        }

        // start IOSession
        try{
            /*  async start output -> onStartOutputSuccess -> keep output and notify observers
                                   -> onStartOutputFailed -> onAddOutputFailed
            * */
            LogManager.i(String.format("try to async start output(%d) :%s", outputId, url));
            params.obj2 = new SinkStruct(videoSinkId, newVideoSink, audioSinkId, newAudioSink);
            mIOEngine.asyncStartSession(outputId, new IOEngine.IOSession.asyncActionCallback(params) {
                @Override
                public void onStarting(int err) {
                    AddingOutputParams params = (AddingOutputParams) getExtParams();
                    if (err == 0) {
                        mTaskHandler.sendOnStartOutputSuccess(params);
                    } else {
                        mTaskHandler.sendOnStartOutputFailed(params, err);
                    }
                }
            });
        } catch(RuntimeException e) {
            e.printStackTrace();
            scene.removeVideoSinkOutput(output.videoSinkId, output.id, output.videoOutput);
            scene.removeAudioSinkOutput(output.audioSinkId, output.id, output.audioOutput);
            onAddOutputFailed(outputId, url, -1, String.format("Start output[id=%d url=%s] failed: %s", outputId, url, e.getMessage()));
            return;
        }
    }

    private void onOpenOutputFailed(AddingOutputParams params, int err) {
        int outputId = params.outputId;
        String url = params.url;

        if(params.reopenCnt == REOPEN_CNT_FORBIDDEN || !mIOEngine.retryForOpenFailed(outputId)) {
            onAddOutputFailed(outputId, url, err, String.format("Open output[id=%d url=%s] failed", outputId, url));
        } else {
            mIOEngine.closeSession(outputId);
            if(params.reopenCnt != REOPEN_CNT_FOREVER)
                --params.reopenCnt;
            params.delayMs += RETRY_STEP_INTERVAL_MS;
            if(params.delayMs > RETRY_MAX_INTERVAL_MS) params.delayMs = RETRY_MAX_INTERVAL_MS;
            if(!mTaskHandler.sendDoAddOutput(params)) {
                onAddOutputFailed(outputId, url, err, String.format("Open output[id=%d url=%s] failed, fail to try again", outputId, url));
            } else {
                onAddOutputFailed(outputId, url, 1, String.format("Open output[id=%d url=%s] failed, try again", outputId, url));
            }
        }
    }

    private void onStartOutputSuccess(AddingOutputParams params) {
        int outputId = params.outputId;
        String url = params.url;
        Scene scene = mScenes[params.sceneIdx]; //TODO: check scene
        OutputSession output = (OutputSession) params.obj1;
        SinkStruct struct = (SinkStruct)params.obj2;
        LogManager.i(String.format("start output(%d) successfully: %s", outputId, url));

        mOutputs.put(outputId, output);
        if(struct.newVideoSink)
            scene.startVideoSink(struct.videoSinkId);
        if(struct.newAudioSink)
            scene.startAudioSink(struct.audioSinkId);
        synchronized(mObservers) {
            for (Observer ob : mObservers)
                ob.onOutputAdded(outputId, url, 0);
        }
    }

    private void onStartOutputFailed(AddingOutputParams params, int err) {
        int outputId = params.outputId;
        String url = params.url;
        OutputSession output = (OutputSession) params.obj1;
        Scene scene = mScenes[params.sceneIdx]; //TODO: check scene

        scene.removeVideoSinkOutput(output.videoSinkId, output.id, output.videoOutput);
        scene.removeAudioSinkOutput(output.audioSinkId, output.id, output.audioOutput);
        onAddOutputFailed(outputId, url, err, String.format("Start output[id=%d url=%s] failed: %d", outputId, url, err));
    }

    private void doReOpenOutput(AddingOutputParams params, int err) {
        int outputId = params.outputId;
        String url = params.url;

        if(params.reopenCnt == REOPEN_CNT_FORBIDDEN || !mIOEngine.retryForOpenFailed(outputId)) {
            onOutputLost(outputId, url, err, String.format("Output(id-%d url-%s) stream lost: %s", outputId, url, err));
            return;
        }

        try {
            /*  async reopen output -> onOpening -> onReOpenOutputSuccess
                                                 -> onReOpenOutputFailed -> send `DoOutputReOpen` task again  OR  onOutputLost
            * */
            LogManager.i(String.format("try to async reopen output(%d) :%s", outputId, url));
            mIOEngine.asyncReOpenSession(outputId, new IOEngine.IOSession.asyncActionCallback(params) {
                @Override
                public void onOpening(int err) {
                    AddingOutputParams params = (AddingOutputParams) getExtParams();
                    if (err == 0) {
                        mTaskHandler.sendOnReOpenOutputSuccess(params);
                    } else {
                        mTaskHandler.sendOnReOpenOutputFailed(params, err);
                    }
                }
            });
        } catch(RuntimeException e) {
            e.printStackTrace();
            onOutputLost(outputId, url, err, String.format("Output(id-%d url-%s) stream lost: %s, fail to try again", outputId, url, err));
        }
    }

    private void onReOpenOutputSuccess(AddingOutputParams params) {
        LogManager.i(String.format("reopen output(%d) successfully: %s", params.outputId, params.url));
    }

    private void onReOpenOutputFailed(AddingOutputParams params, int err) {
        int outputId = params.outputId;
        String url = params.url;

        if(params.reopenCnt != REOPEN_CNT_FOREVER)
            --params.reopenCnt;
        if(params.reopenCnt == REOPEN_CNT_FORBIDDEN || !mIOEngine.retryForOpenFailed(outputId)) {
            onOutputLost(outputId, url, err, String.format("Output(id-%d url-%s) stream lost: %s", outputId, url, err));
            return;
        }

        params.delayMs += RETRY_STEP_INTERVAL_MS;
        if(params.delayMs > RETRY_MAX_INTERVAL_MS) params.delayMs = RETRY_MAX_INTERVAL_MS;
        if(!mTaskHandler.sendDoOutputReOpen(params, err)) {
            onOutputLost(outputId, url, err, String.format("Output(id-%d url-%s) stream lost: %s, fail to try again", outputId, url, err));
        } else {
            LogManager.e(String.format("Output(id-%d url-%s) lost. Try to reopen", outputId, url));
        }
    }

    private void onAddOutputFailed(int outputId, String url, int err, String msg) {
        LogManager.e(msg);
        synchronized (mObservers) {
            for (Observer ob : mObservers)
                ob.onOutputAdded(outputId, url, err);
        }
    }

    private void onOutputLost(int outputId, String url, int err, String msg) {
        LogManager.e(msg);
        synchronized(mObservers) {
            for (Observer ob : mObservers)
                ob.onOutputLost(outputId, url, err);
        }
    }

    private void cfgVideoOutput(OutputSession session, MediaEngine.VideoSinkConfig videoSinkConf) {
        if(videoSinkConf == null)
            return;
        MediaFormat outFormat = MediaFormat.createVideoFormat(videoSinkConf.mimeType, videoSinkConf.width, videoSinkConf.height);
        outFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoSinkConf.frameRate);
        outFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoSinkConf.bitrate);
        session.videoTrackId = mIOEngine.addTrack(session.id, DataType.VIDEO, outFormat);
    }

    private void linkVideoOutput(Scene scene, OutputSession session, int sinkId) {
        int trackId = session.videoTrackId;
        AVChannel ch = mIOEngine.getTrackIO(session.id, trackId).wChannel;
        MediaEngine.SinkOutput output = new MediaEngine.SinkOutput(trackId, ch);
        scene.addVideoSinkOutput(sinkId, session.id, output);
        session.set(true, sinkId, output);
    }

    private void cfgAudioOutput(OutputSession session, MediaEngine.AudioSinkConfig audioSinkConf) {
        if(audioSinkConf == null)
            return;
        MediaFormat outFormat = mMediaEngine.getAudioSinkBasicFormat(audioSinkConf.encFormat.CodecName);
        if (audioSinkConf.encFormat.CodecName == AudioEncoder.Supporting.ENC_NAME_AAC) {
            outFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioSinkConf.encFormat.Bitrate);
            outFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, audioSinkConf.encFormat.Profile);
        }
        session.audioTrackId = mIOEngine.addTrack(session.id, DataType.AUDIO, outFormat);
    }

    private void linkAudioOutput(Scene scene, OutputSession session, int sinkId) {
        int trackId = session.audioTrackId;
        AVChannel ch = mIOEngine.getTrackIO(session.id, trackId).wChannel;
        MediaEngine.SinkOutput output = new MediaEngine.SinkOutput(trackId, ch);
        scene.addAudioSinkOutput(sinkId, session.id, output);
        session.set(false, sinkId, output);
    }

    private void waitUntilReady() {
        synchronized (mRunningLock) {
            while (!mRunning) {
                try {
                    mRunningLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void notifyReady() {
        synchronized (mRunningLock) {
            mRunning = true;
            mRunningLock.notify();
        }
    }

    private void waitUntilOver() {
        synchronized (mRunningLock) {
            while (mRunning) {
                try {
                    mRunningLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void notifyOver() {
        synchronized (mRunningLock) {
            mRunning = false;
            mRunningLock.notify();
        }
    }

    @Override
    public void run() {
        LogManager.i("MediaController thread started!");
        Looper.prepare();
        mTaskHandler = new TaskHandler(this);
        notifyReady();
        Looper.loop();
        notifyOver();
        LogManager.i("MediaController thread exit...!");
    }

    class TaskHandler extends Handler {
        private static final int MSG_START                = 0x0000;

        private static final int MSG_SOURCE_ADD            = MSG_START + 1;
        private static final int MSG_SOURCE_ADD_DO         = MSG_SOURCE_ADD + 1;
        private static final int MSG_SOURCE_ADD_OPEN_SUCC  = MSG_SOURCE_ADD + 2;
        private static final int MSG_SOURCE_ADD_OPEN_FAIL  = MSG_SOURCE_ADD + 3;
        private static final int MSG_SOURCE_ADD_START_SUCC = MSG_SOURCE_ADD + 4;
        private static final int MSG_SOURCE_ADD_START_FAIL = MSG_SOURCE_ADD + 5;
        private static final int MSG_SOURCE_ADD_DO_REOPEN  = MSG_SOURCE_ADD + 6;
        private static final int MSG_SOURCE_ADD_REOPEN_SUCC= MSG_SOURCE_ADD + 7;
        private static final int MSG_SOURCE_ADD_REOPEN_FAIL= MSG_SOURCE_ADD + 8;

        private static final int MSG_OUTPUT_ADD            = MSG_SOURCE_ADD + 50;
        private static final int MSG_OUTPUT_ADD_DO         = MSG_OUTPUT_ADD + 1;
        private static final int MSG_OUTPUT_ADD_OPEN_SUCC  = MSG_OUTPUT_ADD + 2;
        private static final int MSG_OUTPUT_ADD_OPEN_FAIL  = MSG_OUTPUT_ADD + 3;
        private static final int MSG_OUTPUT_ADD_START_SUCC = MSG_OUTPUT_ADD + 4;
        private static final int MSG_OUTPUT_ADD_START_FAIL = MSG_OUTPUT_ADD + 5;
        private static final int MSG_OUTPUT_ADD_DO_REOPEN  = MSG_OUTPUT_ADD + 6;
        private static final int MSG_OUTPUT_ADD_REOPEN_SUCC= MSG_OUTPUT_ADD + 7;
        private static final int MSG_OUTPUT_ADD_REOPEN_FAIL= MSG_OUTPUT_ADD + 8;

        private static final int MSG_STOP_LOOP             = MSG_OUTPUT_ADD + 50;

        private WeakReference<MediaController> mWeakTaskThread;

        public TaskHandler(MediaController mc) {
            mWeakTaskThread = new WeakReference<>(mc);
        }

        public void handleMessage(Message msg) {
            // Process incoming messages here
            MediaController mc = mWeakTaskThread.get();
            if (mc == null) {
                LogManager.w("TaskHandler.handleMessage: weak ref is null");
                return;
            }

            switch (msg.what) {
                /*Add source*/
                case MSG_SOURCE_ADD_DO: {
                    mc.doAddSource((AddingSourceParams)msg.obj);
                    break;
                }
                case MSG_SOURCE_ADD_OPEN_SUCC: {
                    mc.onOpenSourceSuccess((AddingSourceParams)msg.obj);
                    break;
                }
                case MSG_SOURCE_ADD_OPEN_FAIL: {
                    mc.onOpenSourceFailed((AddingSourceParams)msg.obj, msg.arg1);
                    break;
                }
                case MSG_SOURCE_ADD_START_SUCC: {
                    mc.onStartSourceSuccess((AddingSourceParams)msg.obj);
                    break;
                }
                case MSG_SOURCE_ADD_START_FAIL: {
                    mc.onStartSourceFailed((AddingSourceParams)msg.obj, msg.arg1);
                    break;
                }
                case MSG_SOURCE_ADD_DO_REOPEN: {
                    mc.doReOpenSource((AddingSourceParams)msg.obj, msg.arg1);
                    break;
                }
                case MSG_SOURCE_ADD_REOPEN_SUCC: {
                    mc.onReOpenSourceSuccess((AddingSourceParams)msg.obj);
                    break;
                }
                case MSG_SOURCE_ADD_REOPEN_FAIL: {
                    mc.onReOpenSourceFailed((AddingSourceParams)msg.obj, msg.arg1);
                    break;
                }

                /*Add output*/
                case MSG_OUTPUT_ADD_DO: {
                    mc.doAddOutput((AddingOutputParams)msg.obj);
                    break;
                }
                case MSG_OUTPUT_ADD_OPEN_SUCC: {
                    mc.onOpenOutputSuccess((AddingOutputParams)msg.obj);
                    break;
                }
                case MSG_OUTPUT_ADD_OPEN_FAIL: {
                    mc.onOpenOutputFailed((AddingOutputParams)msg.obj, msg.arg1);
                    break;
                }
                case MSG_OUTPUT_ADD_START_SUCC : {
                    mc.onStartOutputSuccess((AddingOutputParams)msg.obj);
                    break;
                }
                case MSG_OUTPUT_ADD_START_FAIL : {
                    mc.onStartOutputFailed((AddingOutputParams)msg.obj, msg.arg1);
                    break;
                }
                case MSG_OUTPUT_ADD_DO_REOPEN  : {
                    mc.doReOpenOutput((AddingOutputParams)msg.obj, msg.arg1);
                    break;
                }
                case MSG_OUTPUT_ADD_REOPEN_SUCC: {
                    mc.onReOpenOutputSuccess((AddingOutputParams)msg.obj);
                    break;
                }
                case MSG_OUTPUT_ADD_REOPEN_FAIL: {
                    mc.onReOpenOutputFailed((AddingOutputParams)msg.obj, msg.arg1);
                    break;
                }

                /*Others*/
                case MSG_STOP_LOOP: {
                    Looper.myLooper().quit();
                    break;
                }
                default: {
                    LogManager.e("Unknown message type: " + msg.what);
                }
            };
        }

        private boolean sendDoAddSource(AddingSourceParams params) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_DO, params), params.delayMs);
        }
        private boolean sendOnOpenSourceSuccess(AddingSourceParams params) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_OPEN_SUCC, params), 0);
        }
        private boolean sendOnOpenSourceFailed(AddingSourceParams params, int err) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_OPEN_FAIL, err, 0, params), 0);
        }
        private boolean sendOnStartSourceSuccess(AddingSourceParams params) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_START_SUCC, params), 0);
        }
        private boolean sendOnStartSourceFailed(AddingSourceParams params, int err) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_START_FAIL, err, 0, params), 0);
        }
        private boolean sendDoSourceReOpen(AddingSourceParams params, int err) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_DO_REOPEN, err, 0, params), params.delayMs);
        }
        private boolean sendOnReOpenSourceSuccess(AddingSourceParams params) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_REOPEN_SUCC, params), 0);
        }
        private boolean sendOnReOpenSourceFailed(AddingSourceParams params, int err) {
            return sendMsg(obtainMessage(MSG_SOURCE_ADD_REOPEN_FAIL, err, 0, params), 0);
        }

        private boolean sendDoAddOutput(AddingOutputParams params) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_DO, params), params.delayMs);
        }
        private boolean sendOnOpenOutputSuccess(AddingOutputParams params) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_OPEN_SUCC, params), 0);
        }
        private boolean sendOnOpenOutputFailed(AddingOutputParams params, int err) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_OPEN_FAIL, err, 0, params), 0);
        }
        private boolean sendOnStartOutputSuccess(AddingOutputParams params) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_START_SUCC, params), 0);
        }
        private boolean sendOnStartOutputFailed(AddingOutputParams params, int err) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_START_FAIL, err, 0, params), 0);
        }
        private boolean sendDoOutputReOpen(AddingOutputParams params, int err) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_DO_REOPEN, err, 0, params), params.delayMs);
        }
        private boolean sendOnReOpenOutputSuccess(AddingOutputParams params) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_REOPEN_SUCC, params), 0);
        }
        private boolean sendOnReOpenOutputFailed(AddingOutputParams params, int err) {
            return sendMsg(obtainMessage(MSG_OUTPUT_ADD_REOPEN_FAIL, err, 0, params), 0);
        }

        private boolean sendStopLoop(long delayMs) {
            return sendMsg(obtainMessage(MSG_STOP_LOOP), delayMs);
        }

        private boolean sendMsg(Message msg, long delayMs) {
            if(delayMs > 0)
                return sendMessageDelayed(msg, delayMs);
            else
                return sendMessage(msg);
        }
    }

    ///////////////////////// implementation of MediaEngine.Callback
    public void onVideoStreamStarted(int streamId) {
    }

    public void onVideoStreamFinished(int streamId) {
    }

    public void onVideoStreamError(int streamId, int errorCode) {
    }

    public void onVideoStreamResolutionChanged(int streamId, int width, int height,
                                               int previousWidth, int previousHeight) {
        int sourceId = -1;
        synchronized (mSource2Streams) {
            for (int id : mSource2Streams.keySet()) {
                VideoStream stream = mSource2Streams.get(id).second;
                if (stream != null && stream.ID == streamId) {
                    LogManager.w(String.format("Source[%s] get size(%dx%d) from decoder", stream.Url, width, height));
                    stream.onResolutionChanged(width, height);
                    sourceId = id;
                    break;
                }
            }
        }
        if (sourceId >= 0) {
            synchronized (mObservers) {
                for (Observer ob : mObservers)
                    ob.onSourceResolutionChanged(sourceId, width, height);
            }
        }
    }

    public void onVideoStreamForamtChanged(int streamId, int format, int previousFormat) {
    }

    public void onVideoStreamStatistics(int streamId, float fps, int kbps) {
        int sourceId = -1;
        synchronized (mSource2Streams) {
            for (int id : mSource2Streams.keySet()) {
                VideoStream stream = mSource2Streams.get(id).second;
                if (stream != null && stream.ID == streamId) {
                    stream.onStatistics((int) fps, kbps);
                    sourceId = id;
                    break;
                }
            }
        }
        if (sourceId >= 0) {
            synchronized (mObservers) {
                for (Observer ob : mObservers)
                    ob.onSourceStatistics(sourceId, fps, kbps);
            }
        }
    }

    public void onVideoSinkStarted(int sinkId) {
    }

    public void onVideoSinkFinished(int sinkId) {
    }

    public void onVideoSinkError(int sinkId, int errorCode) {
    }

    public void onVideoSinkStatistics(int sinkId, float fps, int kbps) {
        int outputId = -1;
        for (OutputSession output : mOutputs.values()) {
            if (output.videoSinkId == sinkId) {
                outputId = output.id;
                break;
            }
        }
        if (outputId >= 0) {
            synchronized (mObservers) {
                for (Observer ob : mObservers)
                    ob.onOutputStatistics(outputId, fps, kbps);
            }
        }
    }

    public void onVideoRendererFpsUpdated(float fps, long droppedFrame) {
        synchronized (mObservers) {
            for (Observer ob : mObservers)
                ob.onVideoRendererStatistics(fps, droppedFrame);
        }
    }

    // Operation done
    public void onAddVideoStreamDone(boolean success, String errorMessage) {
    }

    public void onRemoveVideoStreamDone(boolean success, String errorMessage) {
    }

    public void onAddVideoSceneDone(boolean success, String errorMessage) {
    }

    public void onRemoveVideoSceneDone(boolean success, String errorMessage) {
    }

    public void onPostVideoSceneCommandsDone(boolean success, String errorMessage) {
    }

    public void onAddVideoSinkDone(boolean success, String errorMessage) {
    }

    public void onRemoveVideoSinkDone(boolean success, String errorMessage) {
    }

    public void onConfigureVideoSinkDone(boolean success, String errorMessage) {
    }

    public void onStartVideoSinkDone(boolean success, String errorMessage) {
    }

    public void onStopVideoSinkDone(boolean success, String errorMessage) {
    }

    ///////////////////////// implementation of IOEngine.IOSession.Observer
    @Override
    public void onBroken(IOEngine.IOFlags flags, int id, String url, int err) {
        if(flags == IOEngine.IOFlags.IO_RD) {
            LogManager.w(String.format("Source(id-%d url-%s) onBroken with err-%d. Try to reopen.", id, url, err));
            mTaskHandler.sendDoSourceReOpen(new AddingSourceParams(url, null, id, RECOMMENDED_REOPEN_CNT, RETRY_MIN_INTERVAL_MS), err);
        } else if(flags == IOEngine.IOFlags.IO_WR) {
            LogManager.w(String.format("Output(id-%d url-%s) onBroken with err-%d. Try to reopen.", id, url, err));
            mTaskHandler.sendDoOutputReOpen(new AddingOutputParams(-1, url, id, null, null, RECOMMENDED_REOPEN_CNT, RETRY_MIN_INTERVAL_MS), err);
        } else {
            LogManager.i("Unknow IOSession: " + id + "(" + url + ")");
        }
    }

    static public class Tester extends CameraManager.AvailabilityCallback implements SimpleTesting.Tester, Observer {

        static private boolean mTestFromFile = false;
        static private boolean mOutFromFile = false;
        static private boolean mSourceFromFile = false;
        static private MediaEngine.VideoSinkConfig mVideoSinkConfs[] = {
                /*0*/new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1920, 1088, 25, 2 * 1024 * 1024, 10),
                /*1*/new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1280, 720, 25, 1 * 1024 * 1024, 10),
                /*2*/new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 856, 480, 25, 512 * 1024, 10),
                /*3*/new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1920, 1088, 10, 1 * 1024 * 1024, 10),
                /*4*/new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1280, 720, 10, 1 * 1024 * 1024, 10),
        };
        static private MediaEngine.AudioSinkConfig mAudioSinkConf = new MediaEngine.AudioSinkConfig(AudioEncoder.Supporting.ENC_NAME_AAC, 64000, AudioEncoder.Supporting.ENC_AAC_PROFILE_LC);

        private MediaController mMediaController = null;
        private List<Integer> mOutputIds = new ArrayList<>();
        private List<Integer> mSourceIds = new ArrayList<>();
        private int mTestingStep = 5;
        private CameraManager mCameraMgr = null;

        public void start(Object obj) {
            mMediaController = MediaController.getInstance();
            mMediaController.addObserver(this);
            mMediaController.setDisplayName(
                    "测试",
                    MediaController.HORIZONTAL_HCENTER,
                    MediaController.VERTICAL_TOP,
                    "");
            mMediaController.enableDisplayTestingItems(true);
            /*
             mMediaController.displayName(false);
             mMediaController.setBackgroundImage("file:///" + MyApplication.SAVE_PATH + "/" + MyApplication.VIDEO_BG);
             mMediaController.clearBackgroundImage();
            //*/
            // testCameraManager(obj);
            if (mTestFromFile)
                startFromFile();
            else
                startFromCode();
            --mTestingStep;
        }

        public void startFromFile() {
            // "capture-0,capture-0;decoding-cnt;encoding-0,encoding-1;-"
            final String[] gCaptureUrls = {
                    /*0*/"capture://none?type=video&id=0",
                    /*1*/"capture://none?type=video&id=1",
            };
            final String[] gDecodingUrls = {
                    "rtsp://10.1.83.200:5000/main.h264",
                    "rtsp://10.1.83.201:5000/main.h264",
//                    "rtsp://10.1.83.200:5000/main.h264",
//                    "rtsp://10.1.83.201:5000/main.h264",
//                    "rtsp://10.1.83.200:5000/main.h264",
//                    "rtsp://10.1.83.201:5000/main.h264",
//                    "rtsp://10.1.83.200:5000/main.h264",
//                    "rtsp://10.1.83.201:5000/main.h264",
//                    Environment.getExternalStorageDirectory() + "/MPX/test_1080p_24fps_2mbps_32secs.mp4?loop=true",
//                    Environment.getExternalStorageDirectory() + "/MPX/test_1080p_24fps_2mbps_32secs.mp4?loop=true",
//                    Environment.getExternalStorageDirectory() + "/MPX/test_1080p_24fps_2mbps_32secs.mp4?loop=true",
//                    Environment.getExternalStorageDirectory() + "/MPX/test_1080p_24fps_2mbps_32secs.mp4?loop=true",
                    Environment.getExternalStorageDirectory() + "/MPX/test_720p_24fps_1mbps_17secs.mp4?loop=true",
                    Environment.getExternalStorageDirectory() + "/MPX/test_720p_24fps_1mbps_17secs.mp4?loop=true",
//                    Environment.getExternalStorageDirectory() + "/MPX/test_720p_24fps_1mbps_17secs.mp4?loop=true",
//                    Environment.getExternalStorageDirectory() + "/MPX/test_720p_24fps_1mbps_17secs.mp4?loop=true",
            };
            final String[] gEncodingUrls = {
                    /*0*/Environment.getExternalStorageDirectory() + "/test1080p",
                    /*1*/Environment.getExternalStorageDirectory() + "/test720p",
                    /*2*/Environment.getExternalStorageDirectory() + "/test480p",
                    /*3*/Environment.getExternalStorageDirectory() + "/test1080p10",
                    /*4*/Environment.getExternalStorageDirectory() + "/test720p10",
            };
            final boolean audioOutput = true;

            List<String> captureIndexs = new ArrayList<>();
            int decodingCnt = 0;
            List<String> encodingIndexs = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Environment.getExternalStorageDirectory() + "/test.txt"))));
                String cfg = br.readLine();
                br.close();
                String cfgs[] = cfg.split(";");
                if (cfgs.length < 3) {
                    LogManager.e("!!!!invalid test.txt: " + cfg);
                    return;
                }
                LogManager.w("read config from test.txt: " + cfg);
                Collections.addAll(captureIndexs, cfgs[0].split(","));
                decodingCnt = Integer.valueOf(cfgs[1]);
                Collections.addAll(encodingIndexs, cfgs[2].split(","));
            } catch (FileNotFoundException e) {
                LogManager.e(e);
            } catch (IOException e) {
                LogManager.e(e);
            }

            for (String index : captureIndexs) {
                index = index.trim();
                if (index.equals("") || index.equals("-"))
                    continue;
                int id = mMediaController.addSource(gCaptureUrls[Integer.valueOf(index)], MediaController.RECOMMENDED_REOPEN_CNT);
                mSourceIds.add(id);
            }
            for(int i = 0 ; i < decodingCnt ; ++i) {
                int id = mMediaController.addSource(gDecodingUrls[i], MediaController.RECOMMENDED_REOPEN_CNT);
                mSourceIds.add(id);
            }

            MediaEngine.AudioSinkConfig audioSinkConf = null;
            if (audioOutput)
                audioSinkConf = mAudioSinkConf;
            for (String i : encodingIndexs) {
                i = i.trim();
                if (i.equals("") || i.equals("-"))
                    continue;
                int index = Integer.valueOf(i);
                String url = gEncodingUrls[index];
                MediaEngine.VideoSinkConfig cfg = mVideoSinkConfs[index];
                url = url + "_" + cfg.bitrate + ".mp4";
                int id = mMediaController.addOutput(url, cfg, audioSinkConf, RECOMMENDED_REOPEN_CNT);
                mOutputIds.add(id);
                cfg.bitrate += 10;
            }
        }

        public void startFromCode() {
            final String[] sourceUrls = {
                "capture://none?type=video&id=0",
//                "capture://none?type=video&id=1",
//                Environment.getExternalStorageDirectory() + "/MPX/test_1080p.mp4?loop=true",
//                Environment.getExternalStorageDirectory() + "/sample/test-2.mp4?loop=true",
//                Environment.getExternalStorageDirectory() + "/sample/test-3.mp4?loop=true",
//                "rtsp://10.1.83.200:5000/main.h264",
//                "rtsp://10.1.83.201:5000/main.h264",
//                "rtsp://10.1.83.200:5000/main.h264",
//                "rtsp://10.1.83.200:5000/main.h264",
            };
            final Map<String, Integer> outputUrls = new HashMap<String, Integer>() {{
                put(Environment.getExternalStorageDirectory() + "/test0.mp4", 0);
//                put(Environment.getExternalStorageDirectory() + "/test1.mp4", 1);
//                put("rtmp://10.1.36.4:1935/live/cd834c28-91bc-4293-8856-00eaa110a5d8?s=tuyj", 0);
            }};
            final boolean audioOutput = true;

            if (mSourceFromFile) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Environment.getExternalStorageDirectory() + "/in.txt"))));
                    String in_url = br.readLine();
                    while(in_url != null) {
                        in_url = in_url.trim();
                        if(!in_url.equals("")) {
                            int id = mMediaController.addSource(in_url, MediaController.RECOMMENDED_REOPEN_CNT);
                            mSourceIds.add(id);
                        }
                        in_url = br.readLine();
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    LogManager.e(e);
                } catch (IOException e) {
                    LogManager.e(e);
                }
            } else {
                for (int i = 0; i < sourceUrls.length; ++i) {
                    int id = mMediaController.addSource(sourceUrls[i], MediaController.RECOMMENDED_REOPEN_CNT);
                    mSourceIds.add(id);
                }
            }

            MediaEngine.AudioSinkConfig audioSinkConf = null;
            if (audioOutput)
                audioSinkConf = mAudioSinkConf;
            if (mOutFromFile) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Environment.getExternalStorageDirectory() + "/out.txt"))));
                    String out_url = br.readLine();
                    while(out_url != null) {
                        out_url = out_url.trim();
                        if(!out_url.equals("")) {
                            out_url = Environment.getExternalStorageDirectory() + "/" + out_url;
                            int id = mMediaController.addOutput(out_url, mVideoSinkConfs[0], audioSinkConf, RECOMMENDED_REOPEN_CNT);
                            mOutputIds.add(id);
                        }
                        out_url = br.readLine();
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    LogManager.e(e);
                } catch (IOException e) {
                    LogManager.e(e);
                }
            } else {
                for (String url : outputUrls.keySet()) {
                    int index = outputUrls.get(url);
                    MediaEngine.VideoSinkConfig cfg = mVideoSinkConfs[index];
                    int id = mMediaController.addOutput(url, cfg, audioSinkConf, RECOMMENDED_REOPEN_CNT);
                    mOutputIds.add(id);
                    cfg.bitrate += 10;
                }
            }
        }

        public void next() {
            if (mTestingStep == 0) {
                if (mMediaController != null) {
                    mOutputIds.clear();
                    mMediaController.cleanOutputs();
                    mMediaController.release();
                    mMediaController = null;
                }
                return;
            }
            if (false) {
                if (mTestingStep == 4) {
                    for (int id : mSourceIds) {
                        if (id < 0) continue;
                        // 16:9 -> 70% 16:9
                        mMediaController.cutSourceRegion(id, "0.0:0.0:1.0:1.0");
                        mMediaController.setSourcePosition(id, "0.15:0.15:0.7:0.7");
                    }
                } else if (mTestingStep == 3) {
                    for (int id : mSourceIds) {
                        if (id < 0) continue;
                        // 16:9 -> 裁剪 4:3     48:27->36:27 "0.125:0.0:0.75:1.0"
                        mMediaController.cutSourceRegion(id, "0.125:0.0:0.75:1.0");
                        mMediaController.setSourcePosition(id, "0.25:0.0:0.375:0.5");
                    }
                } else if (mTestingStep == 2) {
                    for (int id : mSourceIds) {
                        if (id < 0) continue;
                        // 16:9 -> 充满 4:3     48:27->36:27 "0.125:0.0:0.75:1.0"
                        mMediaController.cutSourceRegion(id, "0.0:0.0:1.0:1.0");
                        mMediaController.setSourcePosition(id, "0.125:0.0:0.75:1.0");
                    }
                } else if (mTestingStep == 1) {
                    for (int id : mSourceIds) {
                        if (id < 0) continue;
                        // 16:9 -> 完整 4:3     16:9->4:3->16:9 "0.125:0.0:0.75:1.0" + "0.0:0.125:0.75:0.75"
                        mMediaController.cutSourceRegion(id, "0.0:0.0:1.0:1.0");
                        mMediaController.setSourcePosition(id, "0.125:0.125:0.75:0.75");
                    }
                }
            } else {
                if (true) {
                    if (mTestingStep == 4) {
                    } else if (mTestingStep == 3) {
                    } else if (mTestingStep == 2) {
                    } else if (mTestingStep == 1) {
                    }
                } else {
                    if (mTestingStep == 4) {
                        mMediaController.autoShow(ScreenLayout.LayoutMode.ASYMMETRY_FIXED, ScreenLayout.FillPattern.FILL_PATTERN_STRETCHED);
                    } else if (mTestingStep == 3) {
                        mMediaController.autoShow(ScreenLayout.LayoutMode.ASYMMETRY_FIXED, ScreenLayout.FillPattern.FILL_PATTERN_CROPPING);
                    } else if (mTestingStep == 2) {
                        mMediaController.autoShow(ScreenLayout.LayoutMode.ASYMMETRY_FIXED, ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING);
                    } else if (mTestingStep == 1) {
                        int idx = 0;
                        mMediaController.setLayout(ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 2);
                        String poss[] = ScreenLayout.getLayouts(ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 2);
                        for (int id : mSourceIds) {
                            if (id < 0) continue;
                            mMediaController.setSourcePosition(id, ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING, poss[idx++]);
                            if (idx == 2)
                                break;
                        }
                    }
                }
            }
            --mTestingStep;
        }

        private void testCameraManager(Object obj) {
            mCameraMgr = (CameraManager) (((Context) obj).getSystemService(Context.CAMERA_SERVICE));
            try {
                mCameraMgr.registerAvailabilityCallback(this, null);

                String[] cameraIds = mCameraMgr.getCameraIdList();
                for (String cameraId : cameraIds) {
                    LogManager.w("Camera Id: " + cameraId);
                    CameraCharacteristics camCaps = mCameraMgr.getCameraCharacteristics(cameraId);

                    StreamConfigurationMap configurationMap = camCaps.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (configurationMap == null) continue;
                    LogManager.w("    StreamConfigurationMap: " + configurationMap.toString());

                    /* 获取图片输出的尺寸 */
                    Size outSizes[] = configurationMap.getOutputSizes(ImageFormat.JPEG);
                    String sizes = "";
                    for (Size outSize : outSizes)
                        sizes += outSize + ",";
                    LogManager.w("    Output size: " + sizes);

                    /* 获取预览画面输出的尺寸，因为我使用TextureView作为预览 */
                    outSizes = configurationMap.getOutputSizes(SurfaceTexture.class);
                    sizes = "";
                    for (Size outSize : outSizes)
                        sizes += outSize + ",";
                    LogManager.w("    Preview size: " + sizes);

                    /* 闪光灯支持 */
                    Boolean available = camCaps.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    available = available == null ? false : available;
                    LogManager.w(String.format("    %s flash", available ? "Enable" : "Disable"));

                    /* 是否为前置摄像头
                    * LENS_FACING_FRONT: 0x00   LENS_FACING_BACK: 0x01  LENS_FACING_EXTERNAL: 0x02
                    * */
                    Integer facing = camCaps.get(CameraCharacteristics.LENS_FACING);
                    LogManager.w(String.format("    Lens type: %d", facing));

                    /* 全像素阵列的尺寸，可能包括黑色校准像素  */
                    Size pixSize = camCaps.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    LogManager.w("    Sensor pixel array size: " + pixSize);
                }
            } catch (CameraAccessException e) {
                LogManager.e(e);
            }
        }

        @Override
        public void onSourceAdded(int id, String url, int result) {
            if (result == 0) {
                mMediaController.addSource2Scene(id);
                mMediaController.autoShow(ScreenLayout.LayoutMode.SYMMETRICAL, ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING);
            }
        }

        @Override
        public void onSourceLost(int id, String url, int result) {
            mMediaController.cleanSceneSource(id);
            mMediaController.autoShow(ScreenLayout.LayoutMode.SYMMETRICAL, ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING);
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
            LogManager.i(String.format("Add output(id-%d url-%s) with %d", id, url, result));
        }

        @Override
        public void onOutputLost(int id, String url, int result) {
            LogManager.w("output-" + id + " lost: " + url + " with result " + result);
        }

        @Override
        public void onOutputStatistics(int id, float fps, int kbps) {
            LogManager.i(String.format("output-%d fps-%.2f kbps-%d", id, fps, kbps));
        }

        @Override
        public void onVideoRendererStatistics(float fps, long droppedFrame) {
            LogManager.i(String.format("render fps: %.2f dropped: %d", fps, droppedFrame));
        }

        @Override
        public void onCameraAvailable(String cameraId) {
            LogManager.e("!!!!!可用源:" + cameraId);
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            LogManager.e("#####不可用源:" + cameraId);
        }
    }
}

