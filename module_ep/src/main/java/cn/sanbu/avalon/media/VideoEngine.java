package cn.sanbu.avalon.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.Size;
import android.view.Choreographer;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sanbu.board.Rockchip;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.sanbu.avalon.endpoint3.structures.jni.AVCLevel;
import cn.sanbu.avalon.media.gles.Drawable2d;
import cn.sanbu.avalon.media.gles.EglCore;
import cn.sanbu.avalon.media.gles.FullFrameRect;
import cn.sanbu.avalon.media.gles.FlatShadedProgram;
import cn.sanbu.avalon.media.gles.SharedTextureManager;
import cn.sanbu.avalon.media.gles.Sprite2d;
import cn.sanbu.avalon.media.gles.Texture2dProgram;
import cn.sanbu.avalon.media.gles.GlUtil;
import cn.sanbu.avalon.media.gles.ScaledDrawable2d;
import cn.sanbu.avalon.media.gles.OffscreenSurface;
import cn.sanbu.avalon.media.gles.TransitionProgram;
import cn.sanbu.avalon.media.gles.WindowSurface;

public class VideoEngine {

    private static final String TAG = "avalon_" + VideoEngine.class.getSimpleName();

    public static volatile boolean gRenderFrozen = false;

    // Limits
    public static final int MAXIMUM_WIDTH                   = 3840;
    public static final int MAXIMUM_HEIGHT                  = 2160;
    public static final int MAXIMUM_FPS                     = 60;
    public static final int MAXIMUM_SOURCE_COUNT            = 16;
    public static final int MAXIMUM_SCENE_COUNT             = 16;
    public static final int MAXIMUM_SINK_COUNT              = 8;
    public static final int MAXIMUM_DISPLAY_SURFACE_COUNT   = 16;

    // Source type enum
    public static final int SOURCE_TYPE_DECODER            = 0x00;
    public static final int SOURCE_TYPE_CAPTURE            = 0x10;

    // Styles
    private static final float[] DISPLAY_NAME_PADDINGS = new float[] { 0.008f, 0.008f, 0.008f, 0.008f };

    // Thresholds
    private static final int NO_SIGNAL_THRESHOLD            = 3000; // 3000ms

    private static final long CAMERA_REOPEN_MIN_THRESHOLD   = 1 * 60;
    private static final long CAMERA_REOPEN_MAX_THRESHOLD   = 5 * 60;

    private static VideoEngine mInstance = null;

    private static String mNoSignalHint = "text://No Signal";
    private static String mLoadingHint = "text://Loading";

    private Surface[] mDisplaySurfaces      = new Surface[MAXIMUM_DISPLAY_SURFACE_COUNT];
    private int[] mDisplaySurfaceFormats    = new int[MAXIMUM_DISPLAY_SURFACE_COUNT];
    private Size[] mDisplaySurfaceSizes     = new Size[MAXIMUM_DISPLAY_SURFACE_COUNT];
    private Map<Integer, Integer> mSurfaceToSource = new TreeMap<>();
    private Map<Integer, Integer> mSurfaceToScene = new TreeMap<>();

    private Map<Integer, Source> mSources   = new TreeMap<>();
    private Map<Integer, Scene> mScenes     = new TreeMap<>();
    private Map<Integer, Sink> mSinks       = new TreeMap<>();
    private Map<String, Source> mCameras    = new TreeMap<>();

    private MixerThread mMixerThread;
    private MixerHandler mMixerHandler;

    private final boolean mEnableMultiThreadRender = false;
    private final boolean mUpdateTextureWhenDraw   = false;
    private final boolean mUseBlitFramebuffer      = true;
    private final int mTextureFilteringMethod      = GLES20.GL_LINEAR;

    //private Callback mCallback = null;
    private Context mContext = null;

    private CameraManager mCameraMgr;
    private volatile Map<String, CameraReopenFlag> mCameraReopeningFlags;
    private final Object mCameraReopeningLock = new Object();

    private Point mDisplaySize = new Point();
    private float mDisplayRefreshRate;

    private final boolean mShowNoSignal;
    private Bitmap mNoSignalImage;
    private Bitmap mLoadingImage;

    private CameraHelper mCameraHelper = CameraHelper.getInstance();

    private final boolean mEnableNDK;

    /**
     * Video source object.
     */
    private class Source implements Runnable {

        //private static final int CAMERA_DEFAULT_WIDTH       = 1920;
        //private static final int CAMERA_DEFAULT_HEIGHT      = 1080;
        //private static final int CAMERA_STATE_UNINIT        = 0;
        //private static final int CAMERA_STATE_CLOSED        = 1;
        //private static final int CAMERA_STATE_OPENING       = 2;
        //private static final int CAMERA_STATE_OPENED        = 3;
        //private static final int CAMERA_STATE_RELEASED      = 4;

        private int mId;
        private int mType;
        private String mConfig;

        private String mCodecName = null;
        private int mDecodeWidth = -1;
        private int mDecodeHeight = -1;
        private MediaCodec mMediaCodec = null;
        private LinkedBlockingQueue<byte[]> mPacketQueue = null;

        private String mCameraId = null;
        private boolean mCaptureSizeAutoFit = true;
        private int mCaptureWidth = -1;
        private int mCaptureHeight = -1;
        private volatile boolean mCameraHasSignal = false;

        private Semaphore mCameraOpenCloseLock = new Semaphore(1);
        private CameraDevice mCameraDevice;
        private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                // This method is called when the camera is opened.
                LogUtil.i(TAG, "CameraDevice onOpened!");
                mCameraOpenCloseLock.release();
                mCameraDevice = cameraDevice;
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                LogUtil.e(TAG, "CameraDevice onDisconnected!");
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraHelper.flush(mCameraId);
                mCameraDevice = null;
                mCameraReopeningFlags.get(mCameraId).trying = false;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                LogUtil.e(TAG, "CameraDevice onError!");
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraHelper.flush(mCameraId);
                mCameraDevice = null;
                mCameraReopeningFlags.get(mCameraId).trying = false;
            }
        };

        //private volatile int mCameraState = CAMERA_STATE_CLOSED;
        private CaptureRequest.Builder mCameraPreviewRequestBuilder;
        private CaptureRequest mCameraPreviewRequest;
        private CameraCaptureSession mCameraCaptureSession;
        private final Object mCameraCaptureSessionLock = new Object();
        private volatile boolean mCameraCaptureSessionCreating;
        private volatile boolean mCameraCaptureSessionConfigured;

        private HandlerThread mCameraBackgroundThread;
        private Handler mCameraBackgroundHandler;

        private Texture2dProgram mTextureProgram;
        private ScaledDrawable2d mRectDrawable;
        private Sprite2d mRect;
        private FloatBuffer mTexCoordArray;

        private final Object mDrawFence = new Object();

        // Orthographic projection matrix.
        private float[] mDisplayProjectionMatrix = new float[16];

        private SurfaceTexture mOutputSurfaceTexture;
        private Object mUpdateSurfaceLock = new Object();
        private boolean mUpdateSurface;

        private LinkedList<Surface> mAttachedDisplaySurfaces = new LinkedList<>();
        private LinkedList<Sink> mAttachedSinks = new LinkedList<>();

        private Thread mDecodeThread = null;
        private final Object mStopFence = new Object();
        private volatile boolean mStopFlag;

        private RectF mCropRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        private volatile boolean mCropRectChanged = true;

        private BitrateStatist mBitrateStatist = new BitrateStatist();
        private FrameRateStatist mFrameRateStatist = new FrameRateStatist();
        private final Object mStatFence = new Object();
        private volatile boolean mDecoderHasOutput = false;

        private class Stats {
            private String  type;
            private String  device_id;
            private String  codec;
            private long    bitrate;
            private float   frame_rate;
            private String  resolution;
            private String  config;

            private Stats() {
                if (mType == SOURCE_TYPE_CAPTURE) {
                    type        = "capture";
                    device_id   = mCameraId;
                    codec       = "";
                    bitrate     = -1;
                    frame_rate  = mFrameRateStatist.averageFrameRate();
                    resolution  = mCaptureWidth + "x" + mCaptureHeight;
                    config      = mConfig;
                } else if (mType == SOURCE_TYPE_DECODER) {
                    type        = "decoder";
                    device_id   = "-1";
                    codec       = mCodecName;
                    bitrate     = mBitrateStatist.averageBitrate();
                    frame_rate  = mFrameRateStatist.averageFrameRate();
                    resolution  = mDecodeWidth + "x" + mDecodeHeight;
                    config      = mConfig;
                }
            }
        } // End of class Stats

        /**
         * Constructor
         * @param type decoder or capture
         * @param config
         *      For decoder : { "codec": "H264", "max_width": 1920, "max_height": 1080 }
         *      For capture : { "device_id": 0, "capture_width": 1920, "capture_height": 1080 }
         */
        public Source(int id, int type, String config) {
            LogUtil.i(TAG, "Initializing video source id=" + id
                    + ", type=" + type + ", config=" + config);

            mId = id;
            mType = type;
            mConfig = config;
        }

        protected void finalize() throws Throwable {
            try {
                LogUtil.i(TAG, "Finalizing VideoEngine::Source object. id=" + mId);
            } finally {
                super.finalize();
            }
        }

        public boolean init() {
            // Parse config to json object
            JsonObject configObject = (JsonObject)new JsonParser().parse(mConfig);

            // Create surface for video frame output
            createOutputSurface();

            // Initialize different source
            switch (mType) {
                case SOURCE_TYPE_DECODER :
                    // Get codec name
                    mCodecName = configObject.get("codec").getAsString();
                    mDecodeWidth = configObject.get("max_width").getAsInt();
                    mDecodeHeight = configObject.get("max_height").getAsInt();

                    // Allocate queue for incoming packet
                    mPacketQueue = new LinkedBlockingQueue<>();

                    // Allocate and start decoder thread
                    mDecodeThread = new Thread(this, "VideoEngine::Source#" + mId);
                    mDecodeThread.start();

                    break;

                case SOURCE_TYPE_CAPTURE:
                    // Get device id and capture parameters
                    final String DEVICE_ID_KEY = "device_id";
                    final String CAPTURE_WIDTH_KEY = "capture_width";
                    final String CAPTURE_HEIGHT_KEY = "capture_height";

                    if (configObject.has(DEVICE_ID_KEY)) {
                        mCameraId = configObject.get(DEVICE_ID_KEY).getAsString();
                    } else {
                        LogUtil.e(TAG, "invalid source config, has no device_id: " + mConfig);
                        return false;
                    }

                    if (configObject.has(CAPTURE_WIDTH_KEY)
                            && configObject.has(CAPTURE_HEIGHT_KEY)) {
                        try {
                            mCaptureSizeAutoFit = false;
                            mCaptureWidth = configObject.get(CAPTURE_WIDTH_KEY).getAsInt();
                            mCaptureHeight = configObject.get(CAPTURE_HEIGHT_KEY).getAsInt();
                        } catch (ClassCastException e) {
                            LogUtil.w(TAG, "Parse capture size parameters failed!");
                        }
                    }

                    // Open camera
                    //openCamera();

                    break;

                default :
                    LogUtil.e(TAG, "Unsupported video source type " + mType);
                    return false;
            }

            return true;
        }

        public int getId() {
            return mId;
        }

        public int getType() {
            return mType;
        }

        public String getConfig() {
            return mConfig;
        }

        private boolean enqueuePacket(byte[] packet) {
            if (mType != SOURCE_TYPE_DECODER) {
                LogUtil.e(TAG, "Current source type doesn't support enqueuePacket");
                return false;
            }

            // Stats bitrate
            synchronized (mStatFence) {
                mBitrateStatist.incomingPacket(packet.length);
            }

            try {
                mPacketQueue.put(packet);
                return true;
            } catch (InterruptedException ie) {
                LogUtil.e(TAG, ie.toString());
                return false;
            }
        }

        private String query() {
            synchronized (mStatFence) {
                Stats stats = new Stats();
                Gson gson = new Gson();

                return gson.toJson(stats);
            } // End of synchronized (mStatFence)
        }

        private boolean attachDisplaySurface(Surface surface) {
            if (SOURCE_TYPE_CAPTURE != mType) {
                LogUtil.e(TAG, "Decoder type video source on Android platform does not support this operation.");
                return false;
            }

            mAttachedDisplaySurfaces.add(surface);
            recreateCameraPreviewSession();

            return true;
        }

        private boolean detachDisplaySurface(Surface surface) {
            if (SOURCE_TYPE_CAPTURE != mType) {
                LogUtil.e(TAG, "Decoder type video source on Android platform does not support this operation.");
                return false;
            }

            mAttachedDisplaySurfaces.remove(surface);
            recreateCameraPreviewSession();

            return true;
        }

        private boolean addSink(Sink sink) {
            if (SOURCE_TYPE_CAPTURE != mType) {
                LogUtil.e(TAG, "Decoder type video source on Android platform does not support this operation.");
                return false;
            }

            mAttachedSinks.add(sink);
            recreateCameraPreviewSession();

            return true;
        }

        private boolean deleteSink(Sink sink) {
            if (SOURCE_TYPE_CAPTURE != mType) {
                LogUtil.e(TAG, "Decoder type video source on Android platform does not support this operation.");
                return false;
            }

            mAttachedSinks.remove(sink);
            recreateCameraPreviewSession();

            return true;
        }

        private void openCamera() {
            // Should not block render thread
            try {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }

                // Prepare background thread and handler for repeating capture request.
                if (mCameraBackgroundThread != null) {
                    LogUtil.e(TAG, "Camera background thread already started!");
                    return;
                }

                mCameraBackgroundThread = new HandlerThread("CameraBackground");
                mCameraBackgroundThread.start();
                mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());

                // Open camera
                mCameraMgr.openCamera(mCameraId,
                        mCameraStateCallback, mCameraBackgroundHandler);

                // Waiting for camera ready
                synchronized (mCameraCaptureSessionLock) {
                    if (!mCameraCaptureSessionConfigured) {
                        mCameraCaptureSessionLock.wait();
                    }
                }

            } catch (CameraAccessException e) {
                // Open failed, release lock
                mCameraOpenCloseLock.release();
                LogUtil.e(TAG, "Got CameraAccessException when opening camera. " + e.toString());
            } catch (InterruptedException e) {
                LogUtil.e(TAG, "Interrupted while trying to lock camera opening.");
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            } catch (SecurityException e) {
                LogUtil.e(TAG, "Got SecurityException when opening camera. " + e.toString());
                throw new RuntimeException("Got SecurityException when opening camera", e);
            } catch (Exception e) {
                LogUtil.e(TAG, "Got unknown exception when opening camera. " + e.toString());
                throw new RuntimeException("Got unknown exception when opening camera", e);
            }
        }

        private void closeCamera() {
            try {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera closing.");
                }

                // Destroy preview session
                destroyCameraPreviewSession();

                // Close device
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                    mCameraHelper.flush(mCameraId);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
            } finally {
                mCameraOpenCloseLock.release();
            }

            // Stop camera background thread
            if (mCameraBackgroundThread != null) {
                mCameraBackgroundThread.quitSafely();
                try {
                    mCameraBackgroundThread.join();
                    mCameraBackgroundThread = null;
                    mCameraBackgroundHandler = null;
                } catch (InterruptedException e) {
                    LogUtil.e(TAG, e.toString());
                }
            }
        }

        private void reopenCamera() {
            if (mCameraId == null)
                return;

            CameraReopenFlag flag = mCameraReopeningFlags.get(mCameraId);

            synchronized (mCameraReopeningLock) {
                ++flag.attempts;

                if (flag.attempts < flag.threshold)
                    return; // to avoid reopen camera too frequently

                for (CameraReopenFlag f: mCameraReopeningFlags.values()) {
                    if (f.trying) {
                        //LogUtil.v(TAG, "Camera id=" + i + " is reopening, wait for next turn.");
                        return;
                    }
                }

                flag.trying = true;
                flag.attempts = 0;
            }

            if (!checkCamera()) {
                flag.trying = false;
                flag.threshold += 100;
                if (flag.threshold > CAMERA_REOPEN_MAX_THRESHOLD)
                    flag.threshold = CAMERA_REOPEN_MAX_THRESHOLD;
                return;
            }
            flag.threshold = CAMERA_REOPEN_MIN_THRESHOLD;

            LogUtil.d(TAG, "Reopen camera#" + mCameraId + " with " + mCaptureWidth + "x" + mCaptureHeight);

            closeCamera();

            openCamera();
        }

        private boolean checkCamera() {
            if (!mCameraHelper.isConnected(mCameraId)) {
                LogUtil.d(TAG, "Camera#" + mCameraId + " is not connected");
                // trigger to check camera status once
                mCameraHelper.flush(mCameraId);
                return false;
            }

            if (!mCameraHelper.isAvailable(mCameraId)) {
                LogUtil.d(TAG, "Camera#" + mCameraId + " is not available");
                return false;
            }

            // If auto fit capture size, use current resolution
            if (mCaptureSizeAutoFit) {
                Size size = mCameraHelper.getDefaultSize(mCameraId);
                if (size == null) {
                    LogUtil.d(TAG, "Can not get default size for Camera#" + mCameraId);
                    return false;
                }

                LogUtil.d(TAG, "Camera#" + mCameraId + " using default size=" + size.getWidth() + "x" + size.getHeight());
                mCaptureWidth = size.getWidth();
                mCaptureHeight = size.getHeight();
            }

            return true;
        }

        private void createCameraPreviewSession() {
            LogUtil.i(TAG, "Creating camera preview session.");
            if (mCameraCaptureSessionCreating || mCameraCaptureSessionConfigured) {
                LogUtil.e(TAG, "Invalid camera session status: creating="
                        + mCameraCaptureSessionCreating + ", configured="
                        + mCameraCaptureSessionConfigured);
                return;
            }

            // Set capture size
            if (mCaptureWidth <= 0 || mCaptureHeight <= 0) {
                // TODO: Query input resolution from hardware
                LogUtil.i(TAG, "No capture size specified, using 1920x1080");
                mCaptureWidth = 1920;
                mCaptureHeight = 1080;
            }

            LogUtil.i(TAG, "Set device#" + mCameraId + " capture size to " + mCaptureWidth + "x" + mCaptureHeight);
            mOutputSurfaceTexture.setDefaultBufferSize(mCaptureWidth, mCaptureHeight);

            mCameraCaptureSessionCreating = true;
            try {
                mCameraPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // Add output surfaces
                List<Surface> output_surfaces = new LinkedList<>();

                // Mixer input surface
                Surface mixer_surface = new Surface(mOutputSurfaceTexture);

                mCameraPreviewRequestBuilder.addTarget(mixer_surface);
                output_surfaces.add(mixer_surface);

                // Add attahed display surfaces
                for (Surface s : mAttachedDisplaySurfaces) {
                    mCameraPreviewRequestBuilder.addTarget(s);
                    output_surfaces.add(s);
                }

                // Add attached sink input surface
                for (Sink s : mAttachedSinks) {
                    mCameraPreviewRequestBuilder.addTarget(s.getInputSurface());
                    output_surfaces.add(s.getInputSurface());
                }

                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice.createCaptureSession(
                        output_surfaces,
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                LogUtil.d(TAG, "CameraCaptureSession onConfigured!");
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    LogUtil.e(TAG, "The camera is already closed");
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                mCameraCaptureSession = cameraCaptureSession;
                                try {
                                    if (!mCameraHelper.isNormative(mCameraId)) {
                                        // Disable 3A settings for un-normative camera
                                        mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE,
                                                CaptureRequest.CONTROL_MODE_OFF);
                                    } else {
                                        // For other devices which has built-in camera
                                        mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                                        mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                                CaptureRequest.CONTROL_AE_MODE_ON);
                                        mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                                                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ);
                                    }

                                    // Finally, we start displaying the camera preview.
                                    mCameraPreviewRequest = mCameraPreviewRequestBuilder.build();
                                    mCameraCaptureSession.setRepeatingRequest(
                                            mCameraPreviewRequest, new CameraCaptureSession.CaptureCallback() {
                                                @Override
                                                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                                               @NonNull CaptureRequest request,
                                                                               @NonNull TotalCaptureResult result) {
                                                    super.onCaptureCompleted(session, request, result);

                                                    // Notify sinks
                                                    for (Sink s : mAttachedSinks) {
                                                        s.frameAvailableSoon();
                                                    }

                                                    if (!mCameraHasSignal) {
                                                        mCameraHasSignal = true;
                                                        onSourceStateChanged(mId, mType, mCameraHasSignal);
                                                    }

                                                    // Stat fps for capture device
                                                    statFrameRate();
                                                }

                                                @Override
                                                public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                                                            @NonNull CaptureRequest request,
                                                                            @NonNull CaptureFailure failure) {
                                                    super.onCaptureFailed(session, request, failure);

                                                    LogUtil.e(TAG, "CameraCaptureSession.onCaptureFailed");

                                                    if (mCameraHasSignal) {
                                                        mCameraHasSignal = false;
                                                        onSourceStateChanged(mId, mType, mCameraHasSignal);
                                                    }

                                                    // TODO:
                                                }
                                            }, mCameraBackgroundHandler);

                                    synchronized (mCameraCaptureSessionLock) {
                                        mCameraCaptureSessionCreating = false;
                                        mCameraCaptureSessionConfigured = true;
                                        mCameraCaptureSessionLock.notify();
                                    }
                                } catch (CameraAccessException e) {
                                    LogUtil.e(TAG, e.toString());
                                }

                                // Clear opening flag delay 1s
                                new Thread(() -> {
                                        try {
                                            Thread.sleep(1000);
                                            mCameraReopeningFlags.get(mCameraId).trying = false;
                                        } catch (InterruptedException e) {
                                            LogUtil.d(TAG, e.toString());
                                        }
                                    }).start();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                LogUtil.e(TAG, "CameraCaptureSession onConfigureFailed!");
                                synchronized (mCameraCaptureSessionLock) {
                                    mCameraCaptureSessionCreating = false;
                                    mCameraCaptureSessionConfigured = false;
                                    mCameraCaptureSessionLock.notify();
                                }
                                mCameraReopeningFlags.get(mCameraId).trying = false;

                                if (mCameraHasSignal) {
                                    mCameraHasSignal = false;
                                    onSourceStateChanged(mId, mType, mCameraHasSignal);
                                }
                            }

                            @Override
                            public void onClosed(@NonNull CameraCaptureSession session) {
                                LogUtil.i(TAG, "CameraCaptureSession onClosed!");
                                synchronized (mCameraCaptureSessionLock) {
                                    mCameraCaptureSessionCreating = false;
                                    mCameraCaptureSessionConfigured = false;
                                    mCameraCaptureSessionLock.notify();
                                }

                                if (mCameraHasSignal) {
                                    mCameraHasSignal = false;
                                    onSourceStateChanged(mId, mType, mCameraHasSignal);
                                }
                            }
                        }, mCameraBackgroundHandler
                );
            } catch (CameraAccessException e) {
                LogUtil.e(TAG, e.toString());
            }
        }

        private void destroyCameraPreviewSession() {
            LogUtil.i(TAG, "Destroying camera preview session.");
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
        }

        private void recreateCameraPreviewSession() {
            try {
                // Destroy camera capture session
                destroyCameraPreviewSession();

                // Waiting for camera session closed
                synchronized (mCameraCaptureSessionLock) {
                    if (mCameraCaptureSessionConfigured) {
                        mCameraCaptureSessionLock.wait();
                    }
                }

                // Create new preview session
                createCameraPreviewSession();

                // Waiting for camera session configured
                synchronized (mCameraCaptureSessionLock) {
                    if (!mCameraCaptureSessionConfigured) {
                        mCameraCaptureSessionLock.wait();
                    }
                }
            } catch (InterruptedException ie) {
                // TODO:
            }
        }

        private void createDecoder() {
            String mime = null;
            if (mCodecName.equals("H264")) {
                mime = MediaFormat.MIMETYPE_VIDEO_AVC;
            } else if (mCodecName.equals("H265")) {
                mime = MediaFormat.MIMETYPE_VIDEO_HEVC;
            }

            MediaFormat fmt = MediaFormat.createVideoFormat(mime, mDecodeWidth, mDecodeHeight);

            try {
                mMediaCodec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                throw new RuntimeException("Create decoder for " + mime + " failed!");
            }

            // Configure media codec and start it!
            mMediaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mMediaCodec.configure(fmt, new Surface(mOutputSurfaceTexture), null, 0);
            mMediaCodec.start();
        }

        private void createOutputSurface() {
            mTextureProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
            int textureId = mTextureProgram.createTextureObject(mTextureFilteringMethod);
            mOutputSurfaceTexture = new SurfaceTexture(textureId);
            mOutputSurfaceTexture.setOnFrameAvailableListener(
                    new SurfaceTexture.OnFrameAvailableListener() {
                        @Override
                        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                            //LogUtil.d(TAG, "SurfaceTexture.OnFrameAvailableListener.onFrameAvailable");
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

        public void release() {
            // Stop decoder thread
            synchronized (mStopFence) {
                mStopFlag = true;

                // Detach from GL context, thread safe?
                if (mOutputSurfaceTexture != null) {
                    mOutputSurfaceTexture.detachFromGLContext();
                }
            }

            // Disable camera watchdog and close camera
            if (SOURCE_TYPE_CAPTURE == mType) {
                closeCamera();
            }
        }

        void statFrameRate() {
            // Stat fps
            synchronized (mStatFence) {
                long now = System.currentTimeMillis();
                // Detect decoder has output frame
                if (SOURCE_TYPE_DECODER == mType) {
                    long last_pts = mFrameRateStatist.lastFrameTimestamp();
                    boolean hasOutput = (last_pts > 0 && now - last_pts < NO_SIGNAL_THRESHOLD);
                    if (mDecoderHasOutput != hasOutput) {
                        mDecoderHasOutput = hasOutput;
                        onSourceStateChanged(mId, mType, mDecoderHasOutput);
                        LogUtil.d(TAG, "Source id=" + mId + " signal status changed: " + hasOutput);
                    }
                }

                mFrameRateStatist.incomingFrame(now);
                /*if (mFrameRateStatist.totalFrameCount() % (int) mDisplayRefreshRate == 0) {
                    LogUtil.v(TAG, "Video source #" + mId
                            + ", type: " + (mType == SOURCE_TYPE_CAPTURE ? "capture" : "decoder")
                            + ", fps: " + mFrameRateStatist.averageFrameRate()
                            + ", total frames: " + mFrameRateStatist.totalFrameCount());
                }*/
            }
        }

        public void draw(RectF srcRect, RectF dstRect) {
            // Update texture coordinates by srcRect
            float[] textureCoordArray = {
                    srcRect.left, srcRect.bottom,   // 0 bottom left
                    srcRect.right, srcRect.bottom,  // 1 bottom right
                    srcRect.left, srcRect.top,      // 2 top left
                    srcRect.right, srcRect.top      // 3 top right
            };

            // Translate canvas coordinates to OpenGL coordinates
            float posX = dstRect.left;
            float posY = 1.0f - dstRect.bottom;

            if (mUpdateTextureWhenDraw) {
                updateTexture();
            }

            synchronized (mDrawFence) {
                if (mTexCoordArray == null) {
                    mTexCoordArray = GlUtil.createFloatBuffer(textureCoordArray);
                } else {
                    mTexCoordArray.put(textureCoordArray);
                    mTexCoordArray.position(0);
                }

                mRect.setPosition(posX, posY);
                mRect.setRotation(0.0f);
                mRect.setScale(dstRect.width(), dstRect.height());
                mRect.draw(mTextureProgram, mDisplayProjectionMatrix, mTexCoordArray);

                //GLES20.glFinish();
            }
        }

        public void draw(FloatBuffer texRect, RectF dstRect) {
            // Translate canvas coordinates to OpenGL coordinates
            float posX = dstRect.left;
            float posY = 1.0f - dstRect.bottom;

            if (mUpdateTextureWhenDraw) {
                updateTexture();
            }

            synchronized (mDrawFence) {
                mRect.setPosition(posX, posY);
                mRect.setRotation(0.0f);
                mRect.setScale(dstRect.width(), dstRect.height());
                mRect.draw(mTextureProgram, mDisplayProjectionMatrix, texRect);

                //GLES20.glFinish();
            }
        }

        boolean hasSignal() {
            if (mType == SOURCE_TYPE_CAPTURE) {
                return mCameraHasSignal;
            } else {
                return mDecoderHasOutput;
            }
        }

        private Packet pollPacket(long timeout) {
            try {
                byte[] bytes = mPacketQueue.poll(timeout, TimeUnit.MILLISECONDS);
                return Packet.unserialize(bytes);
            } catch (InterruptedException ie) {
                // TODO:
                return null;
            }
        }

        public void run() {
            //LogUtil.d(TAG, "Video source #" + mId + " thread started!");

            // Create video decoder
            createDecoder();

            ByteBuffer[] decoderInputBuffers = mMediaCodec.getInputBuffers();
            ByteBuffer[] decoderOutputBuffers = mMediaCodec.getOutputBuffers();
            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            long firstInputTimeNsec = -1;

            //final int NUM_FRAMES = 120;
            //long periodStartTimeNS = 0;
            //int periodFrameCount = 0;
            //int periodFrameBytes = 0;

            boolean outputDone = false;
            boolean inputDone = false;

            Packet packet = null;
            LogUtil.d(TAG, "Enter video decoding loop.");
            while (!outputDone) {
                // Check stop flag
                if (mStopFlag) {
                    LogUtil.i(TAG, "VideoSource#" + mId + " thread exited!");

                    // Stop and release codec
                    mMediaCodec.stop();
                    mMediaCodec.release();
                    mMediaCodec = null;

                    // Release output surface texture
                    if (mOutputSurfaceTexture != null) {
                        mOutputSurfaceTexture.release();
                        mOutputSurfaceTexture = null;
                    }

                    // Quit thread
                    return;
                }

                // Feed more data to the decoder
                if (!inputDone) {
                    do {
                        if (packet == null) {
                            // Poll new packet from queue
                            packet = pollPacket(50);
                            if (packet == null) {
                                break;
                            }
                            //LogUtil.d(TAG, "Unserialized packet pts=" + packet.pts
                            //        + ", dts=" + packet.dts + ", media_type=" + packet.mediaType
                            //        + ", codec_type=" + packet.codecType + ", stream_index="
                            //        + packet.streamIndex + ", size=" + packet.size);
                        }

                        int inputBufIndex = mMediaCodec.dequeueInputBuffer(30 * 1000);
                        if (inputBufIndex < 0) {
                            LogUtil.i(TAG, "Decoder input buffer is full, try later. source#" + mId);
                            break;
                        }

                        if (packet.size == 0) {
                            // End of stream -- send empty frame with EOS flag set.
                            mMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                                         MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                            LogUtil.d(TAG, "Sent input EOS");
                            packet = null;
                            break;
                        }

                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        try {
                            // Read the sample data into the ByteBuffer.
                            // This neither respects nor updates inputBuf's position, limit, etc.
                            inputBuf.clear();
                            inputBuf.put(packet.data);
                            inputBuf.flip();
                        } catch (BufferOverflowException e) {
                            LogUtil.w(TAG, "Input buffer is not large enough (packet size="
                                    + packet.size + ")");
                        }

                        // Enqueue input buffer
                        packet.pts *= 1000;  // packet pts in ms, decode need in us.
                        mMediaCodec.queueInputBuffer(inputBufIndex, 0, packet.size, packet.pts, 0);

                        // Release packet
                        packet = null;
                    } while(false);
                }

                while (!outputDone) {
                    int decoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // No output available yet
                        break;
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // Not important for us, since we're using Surface
                        LogUtil.d(TAG, "Decoder output buffers changed");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = mMediaCodec.getOutputFormat();
                        // Update decoded size and crop rectangle
                        int frameWidth = newFormat.getInteger(MediaFormat.KEY_WIDTH);
                        int frameHeight = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        if (newFormat.containsKey("crop-left")
                                && newFormat.containsKey("crop-right")) {
                            int cropLeft = newFormat.getInteger("crop-left");
                            int cropRight = newFormat.getInteger("crop-right");
                            mDecodeWidth = cropRight + 1 - cropLeft;
                            mCropRect.left = (float)cropLeft / (float)frameWidth;
                            mCropRect.right = (float)(cropRight + 1) / (float)frameWidth;
                        }

                        if (newFormat.containsKey("crop-top")
                                && newFormat.containsKey("crop-bottom")) {
                            int cropTop = newFormat.getInteger("crop-top");
                            int cropBottom = newFormat.getInteger("crop-bottom");
                            mDecodeHeight = cropBottom + 1 - cropTop;
                            mCropRect.top = (float)cropTop / (float)frameHeight;
                            mCropRect.bottom = (float)(cropBottom + 1) / (float)frameHeight;
                        }

                        mCropRectChanged = true;

                        LogUtil.d(TAG, "Decoder output format changed: " + newFormat
                                + ", frame-size: " + mDecodeWidth + "x" + mDecodeHeight
                                + ", crop-rect: " + mCropRect);
                    } else if (decoderStatus < 0) {
                        throw new RuntimeException(
                                "Unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus);
                    } else { // decoderStatus >= 0
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            LogUtil.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                            outputDone = true;
                        }

                        // TODO: Playback speed control

                        boolean doRender = (mBufferInfo.size != 0);
                        mMediaCodec.releaseOutputBuffer(decoderStatus, doRender);

                        // Stat fps for decoder
                        statFrameRate();
                    }
                }
            }
        }

        private void updateTexture() {
            if (SOURCE_TYPE_CAPTURE == mType && null == mCameraDevice) {
                //LogUtil.e(TAG, "Video source #" + mId + " camera closed!");
                // Try re-open camera
                reopenCamera();
                // TODO: Draw No Signal Image
                return;
            }

            synchronized (mUpdateSurfaceLock) {
                if (mUpdateSurface) {
                    synchronized (mDrawFence) {
                        mOutputSurfaceTexture.updateTexImage();
                        /*float[] transformMatrix = new float[16];
                        mOutputSurfaceTexture.getTransformMatrix(transformMatrix);
                        String s = "[";
                        for (int i = 0; i < 16; i++) s += transformMatrix[i] + ",";
                        s += "]";
                        LogUtil.d(TAG, "Output transform matrix: " + s);*/
                    }
                    mUpdateSurface = false;
                }
            }
        }
    }

    private class Scene {
        private final static int ALIGNMENT_LEFT       = 0x0001;
        private final static int ALIGNMENT_RIGHT      = 0x0002;
        private final static int ALIGNMENT_HCENTER    = 0x0004;
        private final static int ALIGNMENT_TOP        = 0x0020;
        private final static int ALIGNMENT_BOTTOM     = 0x0040;
        private final static int ALIGNMENT_VCENTER    = 0x0080;

        //public static final int OFFSCREEN_TEXTURE_WIDTH = 1920;
        //public static final int OFFSCREEN_TEXTURE_HEIGHT = 1080;
        //public static final float OFFSCREEN_TEXTURE_ASPECT = (float)OFFSCREEN_TEXTURE_WIDTH
        //                                                        / (float)OFFSCREEN_TEXTURE_HEIGHT;

        private int mId;
        private int mWidth;
        private int mHeight;
        private int mFrameRate;
        private float mAspect;
        private int mWeak;

        private MixerThread mMixer;

        private EglCore mEglCore;
        // TODO: Enable multi display surfaces attahced.
        private Surface mDisplaySurface;
        private WindowSurface mDisplayWindowSurface;
        private FullFrameRect mFullScreen;
        private int mDisplaySurfaceFormat;
        private int mDisplaySurfaceWidth;
        private int mDisplaySurfaceHeight;

        // Used for off-screen rendering.
        private OffscreenSurface mOffscreenSurface;
        private int[] mOffscreenTextures = new int[3];
        private int[] mFramebuffers = new int[3];
        private int[] mDepthBuffers = new int[3];

        // Orthographic projection matrix.
        private float[] mDisplayProjectionMatrix = new float[16];

        // Flat shaded program
        private FlatShadedProgram mProgram;

        //private Texture2dProgram mBgImgTexPgm;
        //private int mBgImgTexId;
        //private Sprite2d mBgImgRect;

        // Shared Texture Objects - for logo, display name, marquee and image overlay
        //private Texture2dProgram mSharedImageTexPgm;
        //private int mSharedImageTexId;
        //private Sprite2d mSharedImageRect;
        private SharedTextureManager mSharedTexMgr;
        private int mNoSignalTexSlot = -1;
        private int mLoadingTexSlot = -1;
        private int mDebugInfoTexSlot = -1;

        private final Drawable2d mTriDrawable = new Drawable2d(Drawable2d.Prefab.TRIANGLE);
        private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);

        // One spinning triangle and one bouncing rectangle
        private Sprite2d mTri;
        private Sprite2d mRect;
        private float mRectVelX, mRectVelY;     // velocity, in viewport units per second

        private final float[] mIdentityMatrix = new float[16];

        private long mPrevTimeNanos;

        private int mDemultiplication;
        private int mLoopCount;

        private FrameRateStatist mFrameRateStatist = new FrameRateStatist();

        private class Config implements Cloneable {
            private int backgroundColor;
            //private Bitmap backgroundImage;
            private String displayName;
            private int displayNameAlignment;
            private StyleSheet displayNameStyle = new StyleSheet();

            private boolean needUpdateDisplayNameBitmap;
            private Bitmap displayNameImage;
            //private ByteBuffer displayNameImageBuffer;
            private int displayNameTexSlot = -1;

            @Override
            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            public void release() {
                // Release shared texture slot
                if (displayNameTexSlot >= 0) {
                    //LogUtil.d(TAG, "Config release display name texture slot, id=" + displayNameTexSlot);
                    mSharedTexMgr.releaseSlot(displayNameTexSlot);
                    displayNameTexSlot = -1;
                }
            }

            private void setBackgroundColor(String color) {
                backgroundColor = Color.parseColor(color);
            }

            private int getBackgroundColor() {
                return backgroundColor;
            }

            /* public void setBackgroundImage(String imagePath) {
                // Check is unset
                if (imagePath.equals("none")) {
                    LogUtil.i(TAG, "Disable background image for scene #" + mId);
                    mBackgroundImage = null;
                    return;
                }

                // Load image from file and convert into byte buffer
                LogUtil.i(TAG, "Set background image(" + imagePath + ") for scene #" + mId);
                mBackgroundImage = BitmapFactory.decodeFile(imagePath);
                if (mBackgroundImage != null) {
                    LogUtil.d(TAG, "Loaded background image: width=" + mBackgroundImage.getWidth()
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
                    LogUtil.e(TAG, "Loading background image failed!");
                    // TODO: Notify
                }
            } */
            private void setDisplayName(String name) {
                displayName = name;
                needUpdateDisplayNameBitmap = true;
            }

            private String getDisplayName() {
                return displayName;
            }

            private void setDisplayNameAlignment(String alignment) {
                displayNameAlignment = parseAlignment(alignment);
                needUpdateDisplayNameBitmap = true;
            }

            private int getDisplayNameAlignment() {
                return displayNameAlignment;
            }

            private void setDisplayNameStyleSheet(String styleSheet) {
                displayNameStyle = parseStyleSheet(styleSheet);
                needUpdateDisplayNameBitmap = true;
            }

            private StyleSheet getDisplayNameStyleSheet() {
                return displayNameStyle;
            }

            private void updateDisplayNameImageBuffer() {
                LogUtil.i(TAG, "Updating display name image buffer for scene #" + mId);
                // Render display name as bitmap
                displayNameImage = TextRenderer.renderTextAsBitmap(
                        displayName, displayNameStyle.fontFamily(), displayNameStyle.fontSize(),
                        displayNameStyle.fontWeight(), displayNameStyle.fontItalic(),
                        displayNameStyle.fontColor(), displayNameStyle.backgroundColor(), mWidth);

                /* Copy bitmap data into buffer
                displayNameImageBuffer = ByteBuffer.allocate(
                    displayNameImage.getWidth() * displayNameImage.getHeight() * 4);
                displayNameImage.copyPixelsToBuffer(displayNameImageBuffer);
                displayNameImageBuffer.position(0);*/

                // Reset flag
                needUpdateDisplayNameBitmap = false;
            }
        } // End of class Config

        private Config mCurrentConfig = new Config();
        private Config mPreviousConfig = new Config();

        private class Stats {
            private String  size;
            private int     frame_rate;
            private float   aspect;
            private String  display_surface_size;
            private int         background_color;
            private String      display_name;
            private int         display_name_alignment;
            private StyleSheet  display_name_style_sheet;
            private Config      config;
            private LinkedList<Overlay.Stats> overlays;

            private Stats() {
                size       = mWidth + "x" + mHeight;
                frame_rate = mFrameRate;
                aspect     = mAspect;
                display_surface_size    = mDisplaySurfaceWidth + "x" + mDisplaySurfaceHeight;
                background_color        = mCurrentConfig.getBackgroundColor();
                display_name            = mCurrentConfig.getDisplayName();
                display_name_alignment  = mCurrentConfig.getDisplayNameAlignment();
                display_name_style_sheet = mCurrentConfig.getDisplayNameStyleSheet();
                overlays = new LinkedList<>();
                for (Overlay overlay : mCurrentOverlays) {
                    if (overlay.mWeakRefSource != null) {
                        overlays.push(overlay.new Stats());
                    }
                }
            }
            // marquee
        } // End of class Stats

        private class Overlay {
            static final int OVERLAY_TYPE_STREAM = 0;
            static final int OVERLAY_TYPE_IMAGE = 1;

            private int mType;

            private WeakReference<Source> mWeakRefSource;

            private Bitmap mImage;
            //private ByteBuffer mImageBuffer;
            //private int mImageWidth;
            //private int mImageHeight;
            private int mImageTexSlot = -1;
            private boolean mImageTexUpdated = false;
            //FloatBuffer mImageTexCoordArray;

            private RectF mSrcRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
            private RectF mDstRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
            private FloatBuffer mSrcRectFB;

            private float mOpacity;

            private String mDisplayName;
            private int mDisplayNameAlignment;
            private StyleSheet mDisplayNameStyle = new StyleSheet();
            private boolean mNeedUpdateDisplayNameBitmap;
            private Bitmap mDisplayNameImage;
            //private ByteBuffer mDisplayNameImageBuffer;
            private int mDisplayNameTexSlot = -1;

            private class Stats {
                private String      type;
                private int         source_id;
                private String      src_rect;
                private String      dst_rect;
                //private int       z_index;
                private float       opacity;
                private String      display_name;
                private int         display_name_alignment;
                private StyleSheet      display_name_style_sheet;
                //private String display_name_visible;

                private Stats() {
                    if (mWeakRefSource != null) {
                        if (mType == OVERLAY_TYPE_STREAM) {
                            type = "stream";
                        } else if (mType == OVERLAY_TYPE_IMAGE) {
                            type = "image";
                        } else {
                            type = "unknown";
                        }

                        Source s = mWeakRefSource.get();
                        if (s != null) {
                            source_id = s.getId();
                        }
                        src_rect = mSrcRect.toString();
                        dst_rect = mDstRect.toString();
                        opacity = mOpacity;
                        display_name = mDisplayName;
                        display_name_alignment = mDisplayNameAlignment;
                        display_name_style_sheet = mDisplayNameStyle;
                    }
                }
            }

            private Overlay(Source s) {
                mType = OVERLAY_TYPE_STREAM;
                mWeakRefSource = new WeakReference<>(s);
            }

            private Overlay(Bitmap image) {
                mType = OVERLAY_TYPE_IMAGE;

                if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
                    // Resize to maximum limited size
                    /*int maxWidth = mSharedTexMgr.SINGLE_IMAGE_MAXIMUM_WIDTH;
                    int maxHeight = mSharedTexMgr.SINGLE_IMAGE_MAXIMUM_HEIGHT;

                    if (image.getWidth() <= maxWidth && image.getHeight() <= maxHeight) {
                        mImage = image;
                        mImageWidth = image.getWidth();
                        mImageHeight = image.getHeight();
                    } else {
                        float scale = Math.min(((float) maxWidth / image.getWidth()), ((float) maxHeight / image.getHeight()));
                        int dstWidth = (int)(scale * image.getWidth());
                        int dstHeight = (int)(scale * image.getHeight());
                        mImage = Bitmap.createScaledBitmap(image, dstWidth, dstHeight, false);
                        mImageWidth = dstWidth;
                        mImageHeight = dstHeight;
                    }

                    // Allocate RAW buffer
                    mImageBuffer = ByteBuffer.allocate(mImageWidth * mImageHeight * 4); // RGBA
                    mImage.copyPixelsToBuffer(mImageBuffer);
                    mImageBuffer.position(0);*/

                    // Get idle slot from shared texture
                    mImageTexSlot = mSharedTexMgr.getIdleSlot(0, 0);
                    //LogUtil.d(TAG, "Overlay request an texture slot for image, id=" + mImageTexSlot);

                    /*mImageTexCoordArray = mSharedTexMgr.getTexCoordArray(mImageTexSlot, mImageWidth, mImageHeight);*/
                    //LogUtil.i(TAG, "Allocated shared texture properties: slot="
                    //        + mImageTexSlot + ", size=" + image.getWidth() + "x" + image.getHeight());

                    mImage = image;
                }
            }

            private void draw() {
                if (OVERLAY_TYPE_STREAM == mType) {
                    // Draw source
                    drawSource();
                } else if (OVERLAY_TYPE_IMAGE == mType) {
                    // Draw image
                    drawImage();
                }

                // Draw display name
                drawDisplayName();
            }

            private void drawSource() {
                Source s = mWeakRefSource.get();
                if (s != null) {
                    if (s.hasSignal()) {
                        s.draw(getSrcRectFB(s), mDstRect);
                    } else if (mShowNoSignal) {
                        if (s.getType() == SOURCE_TYPE_CAPTURE)
                            drawNoSignal();
                        else
                            drawLoading();
                    }
                }
            }

            private void drawImage() {
                if (mImage == null) {
                    return;
                }

                if (!mImageTexUpdated) {
                    /*Point pos = mSharedTexMgr.getOffsetInPixel(mImageTexSlot);
                    // Update texture
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSharedImageTexId);
                    GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                    GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0,
                            pos.x, pos.y, mImageWidth, mImageHeight,
                            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mImageBuffer);*/
                    mSharedTexMgr.updateTexture(mImageTexSlot, mImage);
                    mImageTexUpdated = true;
                }

                /*float posX = mDstRect.left;
                float posY = 1.0f - mDstRect.bottom;
                mSharedImageRect.setPosition(posX, posY);
                mSharedImageRect.setRotation(0.0f);
                mSharedImageRect.setScale(mDstRect.width(), mDstRect.height());

                mSharedImageRect.draw(mSharedImageTexPgm, mDisplayProjectionMatrix, mImageTexCoordArray);*/
                mSharedTexMgr.draw(mImageTexSlot, mSrcRect, mDstRect);
            }

            private void drawDisplayName() {
                if (mDisplayName == null || mDisplayName.isEmpty()) {
                    return;
                }

                if (mNeedUpdateDisplayNameBitmap) {
                    updateDisplayNameImage();
                    // Update texture
                    if (mDisplayNameTexSlot < 0) {
                        mDisplayNameTexSlot = mSharedTexMgr.getIdleSlot(0, 0);
                        //LogUtil.d(TAG, "Overlay request an texture slot for display name, id=" + mDisplayNameTexSlot);
                    }
                    mSharedTexMgr.updateTexture(mDisplayNameTexSlot, mDisplayNameImage);
                }

                /*GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSharedImageTexId);
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                        mDisplayNameImage.getWidth(), mDisplayNameImage.getHeight(),
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mDisplayNameImageBuffer);*/

                float widthF = (float)mDisplayNameImage.getWidth() / (float)mWidth;
                float heightF = (float)mDisplayNameImage.getHeight() / (float)mHeight;
                float posX = 0.0f;
                float posY = 0.0f;

                switch (mDisplayNameAlignment & 0x000F) {
                    case ALIGNMENT_LEFT :
                        posX = mDstRect.left + DISPLAY_NAME_PADDINGS[3] * mDstRect.width();
                        break;
                    case ALIGNMENT_HCENTER :
                        posX = mDstRect.centerX() - widthF / 2.0f;
                        break;
                    case ALIGNMENT_RIGHT :
                        posX = mDstRect.right - widthF - DISPLAY_NAME_PADDINGS[1] * mDstRect.width();
                        break;
                }

                switch (mDisplayNameAlignment & 0x00F0) {
                    case ALIGNMENT_TOP :
                        posY = 1.0f - mDstRect.top - heightF - DISPLAY_NAME_PADDINGS[0] * mDstRect.height();
                        break;
                    case ALIGNMENT_VCENTER :
                        posY = 1.0f - mDstRect.centerY() - heightF / 2.0f;
                        break;
                    case ALIGNMENT_BOTTOM :
                        posY = 1.0f - mDstRect.bottom + DISPLAY_NAME_PADDINGS[2] * mDstRect.height();
                        break;
                }

                mSharedTexMgr.draw(mDisplayNameTexSlot, posX, posY, widthF, heightF);

                /*mSharedImageRect.setPosition(posX, posY);
                mSharedImageRect.setRotation(0.0f);
                mSharedImageRect.setScale(widthF, heightF);

                mSharedImageRect.draw(mSharedImageTexPgm, mDisplayProjectionMatrix);*/
            }

            private void drawNoSignal() {
                float widthF = (float)mNoSignalImage.getWidth() / 1920.0f;
                float heightF = (float)mNoSignalImage.getHeight() / 1080.0f;
                float posX = mDstRect.centerX() - widthF / 2.0f;
                float posY = 1.0f - mDstRect.centerY() - heightF / 2.0f;

                mSharedTexMgr.draw(mNoSignalTexSlot, posX, posY, widthF, heightF);
            }

            private void drawLoading() {
                float widthF = (float)mLoadingImage.getWidth() / 1920.0f;
                float heightF = (float)mLoadingImage.getHeight() / 1080.0f;
                float posX = mDstRect.centerX() - widthF / 2.0f;
                float posY = 1.0f - mDstRect.centerY() - heightF / 2.0f;

                mSharedTexMgr.draw(mLoadingTexSlot, posX, posY, widthF, heightF);
            }

            private FloatBuffer getSrcRectFB(Source s) {
                if (s.mCropRectChanged || mSrcRectFB == null) {
                    // TODO: Or adjust by output surface transform matrix
                    // Adjust by crop rectangle
                    RectF r1 = s.mCropRect;
                    RectF r2 = mSrcRect;
                    float dw = r2.width() * r1.width();
                    float dh = r2.height() * r1.height();
                    float dx = r1.left + (r2.left * r1.width());
                    float dy = r1.top + (r2.top * r1.height());

                    // Update texture coordinates by srcRect
                    float[] textureCoordArray = {
                            dx,      dy + dh, // 0 bottom left
                            dx + dw, dy + dh, // 1 bottom right
                            dx,      dy,      // 2 top left
                            dx + dw, dy       // 3 top right
                    };
                    mSrcRectFB = GlUtil.createFloatBuffer(textureCoordArray);

                    // Reset crop rectangle changed flag
                    s.mCropRectChanged = false;
                }

                return mSrcRectFB;
            }

            private void setSrcRect(String rectangle) {
                RectF r = parseRectangle(rectangle);
                if (r == null) {
                    return;
                }

                mSrcRect = r;
            }

            private void setDstRect(String rectangle) {
                RectF r = parseRectangle(rectangle);
                if (r == null) {
                    return;
                }

                mDstRect = r;
            }

            private void setOpacity(String opacity) {
                mOpacity = Integer.valueOf(opacity, 10);
            }

            private void setDisplayName(String displayName) {
                mDisplayName = displayName;
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void setDisplayNameAlignment(String alignment) {
                mDisplayNameAlignment = parseAlignment(alignment);
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void setDisplayNameStyleSheet(String styleSheet) {
                mDisplayNameStyle = parseStyleSheet(styleSheet);
                mNeedUpdateDisplayNameBitmap = true;
            }

            private void updateDisplayNameImage() {
                // Render display name image
                mDisplayNameImage = TextRenderer.renderTextAsBitmap(
                        mDisplayName, mDisplayNameStyle.fontFamily(), mDisplayNameStyle.fontSize(),
                        mDisplayNameStyle.fontWeight(), mDisplayNameStyle.fontItalic(),
                        mDisplayNameStyle.fontColor(), mDisplayNameStyle.backgroundColor(), mWidth);

                /*mDisplayNameImageBuffer = ByteBuffer.allocate(
                        mDisplayNameImage.getWidth() * mDisplayNameImage.getHeight() * 4);
                mDisplayNameImage.copyPixelsToBuffer(mDisplayNameImageBuffer);
                mDisplayNameImageBuffer.position(0);*/

                mNeedUpdateDisplayNameBitmap = false;
            }

            private void release() {
                if (OVERLAY_TYPE_IMAGE == mType && mImageTexSlot >= 0) {
                    //LogUtil.d(TAG, "Overlay release image texture slot, id=" + mImageTexSlot);
                    mSharedTexMgr.releaseSlot(mImageTexSlot);
                    mImageTexSlot = -1;
                }

                if (mDisplayNameTexSlot >= 0) {
                    //LogUtil.d(TAG, "Overlay release display name texture slot, id=" + mDisplayNameTexSlot);
                    mSharedTexMgr.releaseSlot(mDisplayNameTexSlot);
                    mDisplayNameTexSlot = -1;
                }
            }
        }

        public class Recorder {
            private MixerThread mMixer;
            private WeakReference<Scene> mWeakRefScene;
            private WeakReference<Sink> mWeakRefSink;
            private WindowSurface mInputWindowSurface;
            private int mInputSurfaceWidth;
            private int mInputSurfaceHeight;

            private int mDemultiplication;
            private int mLoopCount;

            public Recorder(MixerThread mixer, Scene scene, Sink sink) {
                mMixer = mixer;
                mWeakRefScene = new WeakReference<>(scene);
                mWeakRefSink = new WeakReference<>(sink);

                if (sink != null) {
                    // Create input window surface
                    //Surface inputSurface = sink.getInputSurface();
                    //if (inputSurface != null) {
                    //    mInputWindowSurface = new WindowSurface(mMixer.getEglCore(), inputSurface, true);
                    //} else {
                    //    LogUtil.e(TAG, "Sink input surface is null!");
                    //}

                    mInputSurfaceWidth = sink.getConfig().width;
                    mInputSurfaceHeight = sink.getConfig().height;

                    // Calculate: demultiplication = Math.floor(MIXING_RATE / fps)
                    float fps = sink.getConfig().frameRate;
                    mDemultiplication = (int)(Math.floor((float)scene.mFrameRate / fps));
                    //mDemultiplication = (int)(Math.floor(60.0F / fps));
                    if (mDemultiplication < 1) mDemultiplication = 1;
                    mLoopCount = 0;
                }
            }

            private boolean shouldRecord() {
                return (mLoopCount++ % mDemultiplication) == 0;
            }

            public void beginRecord() {
                Scene scene = mWeakRefScene.get();
                Sink sink = mWeakRefSink.get();
                if (scene != null && sink != null) {
                    //LogUtil.d(TAG, "Mixer.Recorder beginRecord for sink #" + sink.getId());
                    sink.frameAvailableSoon();

                    if (mInputWindowSurface == null) {
                        Surface inputSurface = sink.getInputSurface();
                        if (inputSurface != null) {
                            mInputWindowSurface = new WindowSurface(scene.getEglCore(), inputSurface, true);
                        } else {
                            LogUtil.e(TAG, "Sink input surface is null!");
                        }
                    }

                    mInputWindowSurface.makeCurrent();
                }
            }

            public void finishRecord(long timeStampNanos) {
                Sink sink = mWeakRefSink.get();
                if (sink != null) {
                    //LogUtil.d(TAG, "Mixer.Recorder finishRecord for sink #" + sink.getId());
                    mInputWindowSurface.setPresentationTime(timeStampNanos);
                    mInputWindowSurface.swapBuffers();
                }
            }

            public void release() {
                if (mInputWindowSurface != null) {
                    mInputWindowSurface.release();
                    mInputWindowSurface = null;
                }
            }
        }

        private class Transition {
            private String type = "none";
            private float duration = 0.0f;

            private float progress = 1.0f;
            private long mStartTimestampMs;
            private TransitionProgram mTransitionProgram;

            private void draw(int fromTexId, int toTexId) {
                if (mTransitionProgram == null) {
                    return;
                }

                mTransitionProgram.draw(fromTexId, toTexId, progress);
            }

            private void set(String transitionType, float transitionDuration) {
                if (transitionType == null || transitionType.equals("none") || transitionDuration <= 0.0f) {
                    progress = 1.0f; // No transition == transition finished
                } else {
                    // Validate type
                    if (!TransitionProgram.hasType(transitionType)) {
                        LogUtil.e(TAG, "Invalid transition type: " + transitionType);
                        return;
                    }

                    progress = 0.0f; // Activate transition
                    type = transitionType;
                    duration = transitionDuration;
                    //LogUtil.d(TAG, "Transition activate! type=" + type + ", progress=" + progress);

                    if (mTransitionProgram == null) {
                        mTransitionProgram = new TransitionProgram(type);
                    } else {
                        if (!mTransitionProgram.getType().equals(type)) {
                            mTransitionProgram.release();
                            mTransitionProgram = null;

                            try {
                                mTransitionProgram = new TransitionProgram(type);
                            } catch (RuntimeException e) {
                                LogUtil.e(TAG, "Allocate transition program failed! Skip configuration.");
                                mTransitionProgram = null;
                                progress = 1.0f;
                            }
                        }
                    }
                }
            }

            private void update(long timeStampNanos) {
                // Update progress
                if (progress < 1.0f) {
                    if (progress > 0.0f) {
                        // Calculate current progress
                        long elapsed = timeStampNanos / 1000000 - mStartTimestampMs;
                        progress = (float)elapsed / (duration * 1000.0f);
                        //LogUtil.d(TAG, "Transition running!, type=" + type + ", progress=" + progress);
                    } else {
                        mStartTimestampMs = timeStampNanos / 1000000;
                        progress = 0.001f;
                        //LogUtil.d(TAG, "Transition started!, type=" + type + ", progress=" + progress);
                    }
                } // else transition finished or inactived
            }
        }
        private Transition mTransition = new Transition();

        private class RenderThread extends Thread {
            private WeakReference<Scene> mSceneWeakRef;
            private volatile RenderHandler mRenderHandler;
            private Object mStartLock = new Object();
            private boolean mReady = false;

            public RenderThread(Scene scene) {
                mSceneWeakRef = new WeakReference<>(scene);
            }

            public void sendRender(long frameTimeNanos) {
                mRenderHandler.sendMessage(
                        mRenderHandler.obtainMessage(
                                mRenderHandler.MSG_RENDER,
                                (int)(frameTimeNanos >> 32),
                                (int)frameTimeNanos));
            }

            @Override
            public void run() {
                Scene scene = mSceneWeakRef.get();
                LogUtil.i(TAG, "RenderThread for scene#" + scene.mId + " started!");

                // Create looper
                Looper.prepare();

                // Create handler
                mRenderHandler = new RenderHandler(mSceneWeakRef);

                // Prepare GL
                scene.prepareGL(mMixer.getEglCore());

                // Notify
                synchronized (mStartLock) {
                    mReady = true;
                    mStartLock.notify();    // signal waitUntilReady()
                }

                // Enter message looper
                Looper.loop();

                LogUtil.d(TAG, "VideoSceneRender-" + mId + " thread looper exited!");

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
        }

        private class RenderHandler extends Handler {
            private static final int MSG_RENDER = 0x00;

            private WeakReference<Scene> mSceneWeakRef;

            public RenderHandler(WeakReference<Scene> sceneWeakRef) {
                mSceneWeakRef = sceneWeakRef;
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_RENDER : {
                        // Render frame
                        Scene scene = mSceneWeakRef.get();
                        if (scene != null) {
                            long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
                            scene.render2(timestamp);

                            //LogUtil.d(TAG, "Video scene #" + mId + " rendered done!");
                        }
                        break;
                    }

                    default : {
                        LogUtil.e(TAG, "Unknown message type!");
                    }
                }
            }
        }

        private RenderThread mRenderThread;

        private LinkedList<Overlay> mPreviousOverlays = new LinkedList<>();
        private LinkedList<Overlay> mCurrentOverlays = new LinkedList<>();

        private LinkedList<Recorder> mRecorders = new LinkedList<>();

        private Object mRenderStepLock = new Object();
        private volatile boolean mRenderStepDrawDone;
        private volatile boolean mRenderStepBlitDone;

        protected void finalize() throws Throwable {
            try {
                LogUtil.i(TAG, "Finalizing VideoEngine::Scene object. id=" + mId);
            } finally {
                super.finalize();
            }
        }

        private Scene(int id, int width, int height, int frameRate) {
            mId = id;
            mWidth = width;
            mHeight = height;
            mFrameRate = frameRate;
            mAspect = (float)mWidth / (float)mHeight;
            mWeak = 0;

            // Calculate demultiplication
            mDemultiplication = (int)(Math.floor(mDisplayRefreshRate / (float)frameRate));
            if (mDemultiplication < 1) mDemultiplication = 1;
            mLoopCount = 0;
        }

        @Deprecated
        private boolean reconfig(String config) {
            // Clone current configurations and overlays as previous for transition
            try {
                if (mPreviousConfig != null) {
                    // Release previous resources
                    mPreviousConfig.release();
                    mPreviousConfig = null;
                }
                mPreviousConfig = (Config) mCurrentConfig.clone();
                mCurrentConfig.displayNameTexSlot = -1; // Reset shared texture slot, NOT release!!!
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            // Parse config
            JsonObject configObject = (JsonObject)(new JsonParser().parse(config));

            // Parse scene properties
            final String CONFIG_BG_COLOR_KEY = "background_color";
            if (configObject.has(CONFIG_BG_COLOR_KEY)) {
                mCurrentConfig.setBackgroundColor(configObject.get(CONFIG_BG_COLOR_KEY).getAsString());
            }

            final String CONFIG_DISPLAY_NAME_KEY = "display_name";
            if (configObject.has(CONFIG_DISPLAY_NAME_KEY)) {
                mCurrentConfig.setDisplayName(configObject.get(CONFIG_DISPLAY_NAME_KEY).getAsString());
            }

            final String CONFIG_DISPLAY_NAME_ALIGNMENT_KEY = "display_name_alignment";
            if (configObject.has(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY)) {
                mCurrentConfig.setDisplayNameAlignment(configObject.get(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY).getAsString());
            }

            final String CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY = "display_name_style_sheet";
            if (configObject.has(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY)) {
                mCurrentConfig.setDisplayNameStyleSheet(configObject.get(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY).getAsString());
            }

            // Release previous overlays and clone current overlays
            for (Overlay overlay : mPreviousOverlays) {
                if (overlay != null) {
                    overlay.release();
                }
            }
            mPreviousOverlays = mCurrentOverlays;
            mCurrentOverlays = new LinkedList<>();

            // Parse overlays
            final String CONFIG_OVERLAYS_KEY = "overlays";
            if (configObject.has(CONFIG_OVERLAYS_KEY)) {
                JsonArray overlaysArray = configObject.get(CONFIG_OVERLAYS_KEY).getAsJsonArray();
                for (JsonElement overlay : overlaysArray) {
                    Overlay o = null;

                    if (!overlay.isJsonObject()) {
                        LogUtil.e(TAG, "Overlay element is not an object.");
                        continue;
                    }

                    JsonObject overlayObject = overlay.getAsJsonObject();

                    final String CONFIG_OVERLAY_TYPE_KEY = "type";
                    if (overlayObject.has(CONFIG_OVERLAY_TYPE_KEY)) {
                        String type = overlayObject.get(CONFIG_OVERLAY_TYPE_KEY).getAsString();
                        if (type.equals("stream")) {
                            final String CONFIG_SOURCE_ID_KEY = "source_id";
                            if (overlayObject.has(CONFIG_SOURCE_ID_KEY)) {
                                int source_id = overlayObject.get(CONFIG_SOURCE_ID_KEY).getAsInt();
                                Source source = mMixer.getSource(source_id);
                                if (source == null) {
                                    continue;
                                }
                                // Allocate overlay object
                                o = new Overlay(source);
                            }
                        } else if (type.equals("image")) {
                            final String CONFIG_IMAGE_PATH_KEY = "image_path";
                            if (overlayObject.has(CONFIG_IMAGE_PATH_KEY)) {
                                String image_path = overlayObject.get(CONFIG_IMAGE_PATH_KEY).getAsString();
                                if (!image_path.isEmpty()) {
                                    Bitmap image = BitmapFactory.decodeFile(image_path);
                                    // Allocate overlay object
                                    o = new Overlay(image);
                                } else {
                                    LogUtil.e(TAG, "Empty overlay image_path.");
                                    continue;
                                }
                            }
                        }
                    } else {
                        LogUtil.e(TAG, "Overlay object without type.");
                        continue;
                    }

                    final String CONFIG_SRC_RECT_KEY = "src_rect";
                    if (overlayObject.has(CONFIG_SRC_RECT_KEY)) {
                        String rect = overlayObject.get(CONFIG_SRC_RECT_KEY).getAsString();
                        o.setSrcRect(rect);
                    }

                    final String CONFIG_DST_RECT_KEY = "dst_rect";
                    if (overlayObject.has(CONFIG_DST_RECT_KEY)) {
                        String rect = overlayObject.get(CONFIG_DST_RECT_KEY).getAsString();
                        o.setDstRect(rect);
                    }

                    if (overlayObject.has(CONFIG_DISPLAY_NAME_KEY)) {
                        o.setDisplayName(overlayObject.get(CONFIG_DISPLAY_NAME_KEY).getAsString());
                    }

                    if (overlayObject.has(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY)) {
                        o.setDisplayNameAlignment(overlayObject.get(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY).getAsString());
                    }

                    if (overlayObject.has(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY)) {
                        o.setDisplayNameStyleSheet(overlayObject.get(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY).getAsString());
                    }

                    // Add to overlay list
                    addOverlay(o);
                }
            }

            // TODO: Parse marquee

            // Parse transition
            String transitionType = "none";
            float transitionDuration = 0.0f;

            final String CONFIG_TRANSITION_KEY = "transition";
            if (configObject.has(CONFIG_TRANSITION_KEY)) {
                JsonElement transitionElement = configObject.get(CONFIG_TRANSITION_KEY);
                if (transitionElement.isJsonObject()) {
                    JsonObject transitionObject = transitionElement.getAsJsonObject();
                    final String CONFIG_TRANSITION_TYPE_KEY = "type";
                    if (transitionObject.has(CONFIG_TRANSITION_TYPE_KEY)) {
                        transitionType = transitionObject.get(CONFIG_TRANSITION_TYPE_KEY).getAsString();
                        //LogUtil.d(TAG, "Got transitionType: " + transitionType);
                    }

                    final String CONFIG_TRANSITION_DURATION_KEY = "duration";
                    if (transitionObject.has(CONFIG_TRANSITION_DURATION_KEY)) {
                        transitionDuration = transitionObject.get(CONFIG_TRANSITION_DURATION_KEY).getAsFloat();
                        //LogUtil.d(TAG, "Got transitionDuration: " + transitionDuration);
                    }
                }
            }

            mTransition.set(transitionType, transitionDuration);

            return true;
        }

        private boolean reconfig2(String config) {
            boolean updateTrans = false;

            // Clone current configurations and overlays as previous for transition
            try {
                if (mPreviousConfig != null) {
                    // Release previous resources
                    mPreviousConfig.release();
                    mPreviousConfig = null;
                }
                mPreviousConfig = (Config) mCurrentConfig.clone();
                mCurrentConfig.displayNameTexSlot = -1; // Reset shared texture slot, NOT release!!!
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            // Parse config
            JsonObject configObject = (JsonObject)(new JsonParser().parse(config));

            // Parse scene properties
            final String CONFIG_BG_COLOR_KEY = "background_color";
            if (configObject.has(CONFIG_BG_COLOR_KEY)) {
                mCurrentConfig.setBackgroundColor(configObject.get(CONFIG_BG_COLOR_KEY).getAsString());
            }

            final String CONFIG_DISPLAY_NAME_KEY = "display_name";
            if (configObject.has(CONFIG_DISPLAY_NAME_KEY)) {
                mCurrentConfig.setDisplayName(configObject.get(CONFIG_DISPLAY_NAME_KEY).getAsString());
            }

            final String CONFIG_DISPLAY_NAME_VISIBLE_KEY = "display_name_visible";
            if (configObject.has(CONFIG_DISPLAY_NAME_VISIBLE_KEY)) {
                if (configObject.get(CONFIG_DISPLAY_NAME_VISIBLE_KEY).getAsString().equals("false"))
                    mCurrentConfig.setDisplayName("");
            }

            if (mCurrentConfig.displayName != null && !mCurrentConfig.displayName.isEmpty())
                // the slot has been reset previously (begin of @reconfig)
                mCurrentConfig.needUpdateDisplayNameBitmap = true;

            final String CONFIG_DISPLAY_NAME_ALIGNMENT_KEY = "display_name_alignment";
            if (configObject.has(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY)) {
                mCurrentConfig.setDisplayNameAlignment(configObject.get(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY).getAsString());
            }

            final String CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY = "display_name_style_sheet";
            if (configObject.has(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY)) {
                mCurrentConfig.setDisplayNameStyleSheet(configObject.get(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY).getAsString());
            }

            // Parse overlays
            final String CONFIG_OVERLAYS_KEY = "overlays";
            if (configObject.has(CONFIG_OVERLAYS_KEY)) {

                // Release previous overlays and clone current overlays
                for (Overlay overlay : mPreviousOverlays) {
                    if (overlay != null) {
                        overlay.release();
                    }
                }
                mPreviousOverlays = mCurrentOverlays;
                mCurrentOverlays = new LinkedList<>();
                updateTrans = true;

                JsonArray overlaysArray = configObject.get(CONFIG_OVERLAYS_KEY).getAsJsonArray();
                for (JsonElement overlay : overlaysArray) {
                    Overlay o = null;

                    if (!overlay.isJsonObject()) {
                        LogUtil.e(TAG, "Overlay element is not an object.");
                        continue;
                    }

                    JsonObject overlayObject = overlay.getAsJsonObject();

                    final String CONFIG_OVERLAY_TYPE_KEY = "type";
                    if (overlayObject.has(CONFIG_OVERLAY_TYPE_KEY)) {
                        String type = overlayObject.get(CONFIG_OVERLAY_TYPE_KEY).getAsString();
                        if (type.equals("stream")) {
                            final String CONFIG_SOURCE_ID_KEY = "source_id";
                            if (overlayObject.has(CONFIG_SOURCE_ID_KEY)) {
                                int source_id = overlayObject.get(CONFIG_SOURCE_ID_KEY).getAsInt();
                                Source source = mMixer.getSource(source_id);
                                if (source == null) {
                                    continue;
                                }
                                // Allocate overlay object
                                o = new Overlay(source);
                            }
                        } else if (type.equals("image")) {
                            final String CONFIG_IMAGE_PATH_KEY = "image_path";
                            if (overlayObject.has(CONFIG_IMAGE_PATH_KEY)) {
                                String image_path = overlayObject.get(CONFIG_IMAGE_PATH_KEY).getAsString();
                                if (!image_path.isEmpty()) {
                                    Bitmap image = BitmapFactory.decodeFile(image_path);
                                    // Allocate overlay object
                                    o = new Overlay(image);
                                } else {
                                    LogUtil.e(TAG, "Empty overlay image_path.");
                                    continue;
                                }
                            }
                        }
                    } else {
                        LogUtil.e(TAG, "Overlay object without type.");
                        continue;
                    }

                    final String CONFIG_SRC_RECT_KEY = "src_rect";
                    if (overlayObject.has(CONFIG_SRC_RECT_KEY)) {
                        String rect = overlayObject.get(CONFIG_SRC_RECT_KEY).getAsString();
                        o.setSrcRect(rect);
                    }

                    final String CONFIG_DST_RECT_KEY = "dst_rect";
                    if (overlayObject.has(CONFIG_DST_RECT_KEY)) {
                        String rect = overlayObject.get(CONFIG_DST_RECT_KEY).getAsString();
                        o.setDstRect(rect);
                    }

                    if (overlayObject.has(CONFIG_DISPLAY_NAME_KEY)) {
                        o.setDisplayName(overlayObject.get(CONFIG_DISPLAY_NAME_KEY).getAsString());
                    }

                    if (overlayObject.has(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY)) {
                        o.setDisplayNameAlignment(overlayObject.get(CONFIG_DISPLAY_NAME_ALIGNMENT_KEY).getAsString());
                    }

                    if (overlayObject.has(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY)) {
                        o.setDisplayNameStyleSheet(overlayObject.get(CONFIG_DISPLAY_NAME_STYLE_SHEET_KEY).getAsString());
                    }

                    // Add to overlay list
                    addOverlay(o);
                }
            }

            // TODO: Parse marquee

            // Parse transition
            String transitionType = mTransition.type;
            float transitionDuration = mTransition.duration;

            final String CONFIG_TRANSITION_KEY = "transition";
            if (configObject.has(CONFIG_TRANSITION_KEY)) {
                updateTrans = true;

                JsonElement transitionElement = configObject.get(CONFIG_TRANSITION_KEY);
                if (transitionElement.isJsonObject()) {
                    JsonObject transitionObject = transitionElement.getAsJsonObject();
                    final String CONFIG_TRANSITION_TYPE_KEY = "type";
                    if (transitionObject.has(CONFIG_TRANSITION_TYPE_KEY)) {
                        transitionType = transitionObject.get(CONFIG_TRANSITION_TYPE_KEY).getAsString();
                        //LogUtil.d(TAG, "Got transitionType: " + transitionType);
                    }

                    final String CONFIG_TRANSITION_DURATION_KEY = "duration";
                    if (transitionObject.has(CONFIG_TRANSITION_DURATION_KEY)) {
                        transitionDuration = transitionObject.get(CONFIG_TRANSITION_DURATION_KEY).getAsFloat();
                        //LogUtil.d(TAG, "Got transitionDuration: " + transitionDuration);
                    }
                }
            }

            if (updateTrans)
                mTransition.set(transitionType, transitionDuration);

            return true;
        }

        private void attachDisplaySurface(Surface surface) {
            if (mMixer.getEglCore() == null) {
                LogUtil.e(TAG, "EGL Core is not initialized!");
                return;
            }

            mDisplaySurface = surface;
            if (mDisplaySurface == null) {
                LogUtil.e(TAG, "Display surface is null, nothing to attach!");
            }

            mDisplayWindowSurface = null;
            //mDisplayWindowSurface = new WindowSurface(mMixer.getEglCore(), mDisplaySurface, false);
            //mDisplayWindowSurface.makeCurrent();

            // Set the background color.
            //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // Disable depth testing -- we're 2D only.
            //GLES20.glDisable(GLES20.GL_DEPTH_TEST);

            // Don't need backface culling.
            // (If you're feeling pedantic, you can turn it on to make sure we're
            // defining our shapes correctly.)
            //GLES20.glDisable(GLES20.GL_CULL_FACE);
        }

        private int getId() {
            return mId;
        }

        private void onDisplaySurfaceChanged(int format, Size size) {
            LogUtil.d(TAG, "Video scene #" + mId + " display surface changed, format: " + format
                    + ", size: " + size.getWidth() + "x" + size.getHeight());
            mDisplaySurfaceFormat = format;
            mDisplaySurfaceWidth = size.getWidth();
            mDisplaySurfaceHeight = size.getHeight();
        }

        private void detachDisplaySurface() {
            if (mDisplaySurface == null) {
                LogUtil.e(TAG, "Display surface is null, nothing to detach!");
            } else {
                if (mDisplayWindowSurface != null) {
                    mDisplayWindowSurface.release();
                    mDisplayWindowSurface = null;
                }
                mDisplaySurface = null;
            }
        }

        public void addOverlay(Overlay overlay) {
            //synchronized (mCurrentOverlays) {
                mCurrentOverlays.add(overlay);
            //}
        }

        public void clearOverlays() {
            //synchronized (mCurrentOverlays) {
                mCurrentOverlays.clear();
            //}
        }

        public void addSink(Sink sink) {
            // TODO: synchronized mRecorders?
            mRecorders.add(new Recorder(mMixer, this, sink));
        }

        public void deleteSink(int sinkId) {
            // TODO: synchronized mRecorders?
            for (Recorder recoder : mRecorders) {
                Sink sink = recoder.mWeakRefSink.get();
                if (sink != null && sink.getId() == sinkId) {
                    LogUtil.d(TAG, "Video sink id=" + sinkId + " recorder removed from mixing thread.");
                    recoder.release();
                    mRecorders.remove(recoder);
                    return;
                }
            }
        }

        public void clearSinks() {
            mRecorders.clear();
        }

        public String query() {
            Stats stats = new Stats();
            Gson gson = new Gson();
            return gson.toJson(stats);
        }

        private void init(MixerThread mixer) {
            mMixer = mixer;

            if (mEnableMultiThreadRender) {
                // Start render thread
                mRenderThread = new RenderThread(this);
                mRenderThread.setName("VideoSceneRender-" + mId);
                mRenderThread.start();
                mRenderThread.waitUntilReady();
            } else {
                prepareGL(mMixer.getEglCore());
            }
        }

        private void release() {
            LogUtil.d(TAG, "Release video scene #" + mId + " internal resources.");
            if (mEnableMultiThreadRender) {
                // TODO: Call releaseGL in render thread.
            } else {
                if (mSharedTexMgr != null) {
                    LogUtil.i(TAG, "Release shared texture manager for scene#" + mId);
                    mSharedTexMgr.release();
                    mSharedTexMgr = null;
                }
                releaseGL();
                mMixer = null;
            }
        }

        private void prepareGL(EglCore eglCore) {
            LogUtil.d(TAG, "Prepare Gl for video scene");

            if (mEnableMultiThreadRender) {
                mEglCore = new EglCore(eglCore.getEGLContext(), // Shared context
                        EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
            } else {
                mEglCore = eglCore;
            }

            mOffscreenSurface = new OffscreenSurface(mEglCore, mWidth, mHeight);
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
            prepareFramebuffer(mWidth, mHeight);

            // Prepare texture for background image
            //prepareBgImgTexture();
            prepareSharedTexture();

            // Simple orthographic projection, with (0,0) in lower-left corner.
            //Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, OFFSCREEN_TEXTURE_WIDTH,
            //        0, OFFSCREEN_TEXTURE_HEIGHT, -1, 1);
            Matrix.orthoM(mDisplayProjectionMatrix, 0, 0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f);

            //int smallDim = Math.min(OFFSCREEN_TEXTURE_WIDTH, OFFSCREEN_TEXTURE_HEIGHT);
            //float OFFSCREEN_TEXTURE_ASPECT = (float)OFFSCREEN_TEXTURE_WIDTH / (float)OFFSCREEN_TEXTURE_HEIGHT;

            // Set initial shape size / position / velocity based on window size.  Movement
            // has the same "feel" on all devices, but the actual path will vary depending
            // on the screen proportions.  We do it here, rather than defining fixed values
            // and tweaking the projection matrix, so that our squares are square.
            mTri = new Sprite2d(mTriDrawable);
            mTri.setColor(0.1f, 0.9f, 0.1f);
            mTri.setScale(1.0f / 3.0f, 1.0f / 3.0f);
            mTri.setPosition(0.5f, 0.5f);

            mRect = new Sprite2d(mRectDrawable);
            mRect.setColor(0.9f, 0.1f, 0.1f);
            mRect.setScale(1.0f / 8.0f, mAspect / 8.0f);
            mRect.setPosition(0.5f, 0.5f);
            mRectVelX = 0.25f;
            mRectVelY = 0.25f / mAspect;

            // Program used for drawing testing items.
            mProgram = new FlatShadedProgram();

            Matrix.setIdentityM(mIdentityMatrix, 0);

            // Used for blitting texture to FBO.
            mFullScreen = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));
        }

        /**
         * Prepares the off-screen framebuffer.
         */
        private void prepareFramebuffer(int width, int height) {
            GlUtil.checkGlError("prepareFramebuffer start");

            for (int i = 0; i < 3; i++) {
                int[] values = new int[1];

                // Create a texture object and bind it.  This will be the color buffer.
                GLES20.glGenTextures(1, values, 0);
                GlUtil.checkGlError("glGenTextures");
                mOffscreenTextures[i] = values[0];   // expected > 0
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextures[i]);
                GlUtil.checkGlError("glBindTexture " + mOffscreenTextures[i]);

                // Create texture storage.
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                // Set parameters.  We're probably using non-power-of-two dimensions, so
                // some values may not be available for use.
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, mTextureFilteringMethod);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, mTextureFilteringMethod);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GlUtil.checkGlError("glTexParameter");

                // Create framebuffer object and bind it.
                GLES20.glGenFramebuffers(1, values, 0);
                GlUtil.checkGlError("glGenFramebuffers");
                mFramebuffers[i] = values[0];    // expected > 0
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffers[i]);
                GlUtil.checkGlError("glBindFramebuffer " + mFramebuffers[i]);

                // Create a depth buffer and bind it.
                GLES20.glGenRenderbuffers(1, values, 0);
                GlUtil.checkGlError("glGenRenderbuffers");
                mDepthBuffers[i] = values[0];    // expected > 0
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffers[i]);
                GlUtil.checkGlError("glBindRenderbuffer " + mDepthBuffers[i]);

                // Allocate storage for the depth buffer.
                GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                        width, height);
                GlUtil.checkGlError("glRenderbufferStorage");

                // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                        GLES20.GL_RENDERBUFFER, mDepthBuffers[i]);
                GlUtil.checkGlError("glFramebufferRenderbuffer");
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, mOffscreenTextures[i], 0);
                GlUtil.checkGlError("glFramebufferTexture2D");

                // See if GLES is happy with all this.
                int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
                if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                    throw new RuntimeException("Framebuffer not complete, status=" + status);
                }

                // Switch back to the default framebuffer.
                //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }

            GlUtil.checkGlError("prepareFramebuffer done");
        }

        /*private void prepareBgImgTexture() {
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
                    mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            // Set parameters.  We're probably using non-power-of-two dimensions, so
            // some values may not be available for use.
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    TEXTURE_SCALE_METHOD);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    TEXTURE_SCALE_METHOD);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GlUtil.checkGlError("glTexParameter");

            ScaledDrawable2d rectDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
            mBgImgRect = new Sprite2d(rectDrawable);
            mBgImgRect.setTexture(mBgImgTexId);

            GlUtil.checkGlError("prepareBgImgTexture done");
        }*/

        private void prepareSharedTexture() {
            LogUtil.i(TAG, "Allocate shared texture manager for scene#" + mId);
            mSharedTexMgr = new SharedTextureManager(4, 4);

            // Update No Signal texture
            if (mShowNoSignal && null != mNoSignalImage) {
                mNoSignalTexSlot = mSharedTexMgr.getIdleSlot(0, 0);
                mSharedTexMgr.updateTexture(mNoSignalTexSlot, mNoSignalImage);
            }

            // Update Loading texture
            if (mShowNoSignal && null != mLoadingImage) {
                mLoadingTexSlot = mSharedTexMgr.getIdleSlot(0, 0);
                mSharedTexMgr.updateTexture(mLoadingTexSlot, mLoadingImage);
            }

            /*
            GlUtil.checkGlError("prepareDisplayNameTexture start");

            mSharedImageTexPgm = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);

            // Create a texture object and bind it
            int[] values = new int[1];
            GLES20.glGenTextures(1, values, 0);
            GlUtil.checkGlError("glGenTextures");
            mSharedImageTexId = values[0];   // expected > 0
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSharedImageTexId);
            GlUtil.checkGlError("glBindTexture " + mSharedImageTexId);

            // Create texture storage
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    mSharedTexMgr.getTotalWidth(), mSharedTexMgr.getTotalHeight(),
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            // Set parameters.  We're probably using non-power-of-two dimensions, so
            // some values may not be available for use.
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    TEXTURE_SCALE_METHOD);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    TEXTURE_SCALE_METHOD);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GlUtil.checkGlError("glTexParameter");

            ScaledDrawable2d rectDrawable = new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
            mSharedImageRect = new Sprite2d(rectDrawable);
            mSharedImageRect.setTexture(mSharedImageTexId);

            GlUtil.checkGlError("prepareDisplayNameTexture done");*/
        }

        private void releaseGL() {
            LogUtil.d(TAG, "Release GL resources.");
            // Release frame buffer and depth buffer and textures
            for (int i = 0; i < 3; i++) {
                GLES20.glDeleteFramebuffers(1, new int[]{ mFramebuffers[i] }, 0);
                GLES20.glDeleteRenderbuffers(1, new int[]{ mDepthBuffers[i] }, 0);
                GLES20.glDeleteTextures(1, new int[]{ mOffscreenTextures[i] }, 0);
            }

            //GLES20.glDeleteTextures(1, new int[] { mBgImgTexId }, 0);
            //GLES20.glDeleteTextures(1, new int[] { mSharedImageTexId }, 0);

            // Release contexts
            if (mOffscreenSurface != null) {
                mOffscreenSurface.release();
                mOffscreenSurface = null;
            }

            if (mDisplayWindowSurface != null) {
                mDisplayWindowSurface.release();
                mDisplayWindowSurface = null;
            }
        }

        public EglCore getEglCore() {
            return mEglCore;
        }

        private boolean shouldRender() {
            return (mLoopCount++ % mDemultiplication) == 0;
        }

        private void asyncRender(long timeStampNanos) {
            mRenderThread.sendRender(timeStampNanos);
        }

        private void clearRenderStepFlags() {
            synchronized (mRenderStepLock) {
                mRenderStepDrawDone = false;
                mRenderStepBlitDone = false;
            }
        }

        private boolean isRenderDrawDone() {
            synchronized (mRenderStepLock) {
                return mRenderStepDrawDone;
            }
        }

        private boolean isRenderBlitDone() {
            synchronized (mRenderStepLock) {
                return mRenderStepBlitDone;
            }
        }

        private void renderOffscreen() {
            mOffscreenSurface.makeCurrent();
            GlUtil.checkGlError("render start");

            // Update viewport
            GLES20.glViewport(0, 0, mWidth, mHeight);

            if (mTransition.progress < 1.0f && mTransition.mTransitionProgram != null) {
                // Render previous config
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffers[1]);
                GlUtil.checkGlError("glBindFramebuffer");
                drawBackground(mPreviousConfig);
                drawOverlays(mPreviousOverlays);
                drawDisplayName(mPreviousConfig);

                // Render current config
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffers[2]);
                GlUtil.checkGlError("glBindFramebuffer");
                drawBackground(mCurrentConfig);
                drawOverlays(mCurrentOverlays);
                drawDisplayName(mCurrentConfig);

                // Render transition
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffers[0]);
                GlUtil.checkGlError("glBindFramebuffer");
                mTransition.draw(mOffscreenTextures[1], mOffscreenTextures[2]);

            } else {
                // Render current config only
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffers[0]);
                GlUtil.checkGlError("glBindFramebuffer");

                // Draw background, overlays and display name
                drawBackground(mCurrentConfig);
                drawOverlays(mCurrentOverlays);
                drawDisplayName(mCurrentConfig);
            }

            // Draw testing items
            //drawTestingItems();

            // Draw debug information
            //drawDebugInfo();

            // Draw something unvisible, fix crash on RK3288
            mTri.setPosition(2.0f, 2.0f);
            mTri.draw(mProgram, mDisplayProjectionMatrix);

            GLES20.glFlush();
        }

        private void render(long timeStampNanos) {
            //LogUtil.d(TAG, "Rendering scene #" + mId + "timestamp:" + timeStampNanos);

            // Update transition progress if neccessary
            update(timeStampNanos);

            // Render off-screen
            renderOffscreen();

            synchronized (mRenderStepLock) {
                mRenderStepDrawDone = true;
            }

            // Restore default frame buffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GlUtil.checkGlError("glBindFramebuffer");

            // Blit to display surface
            if (mDisplaySurface != null) {
                if (mDisplayWindowSurface == null) {
                    mDisplayWindowSurface = new WindowSurface(mEglCore, mDisplaySurface, false);
                }

                mDisplayWindowSurface.makeCurrent();

                if (mUseBlitFramebuffer) {
                    // GLES 3.0: glBlitFramebuffer
                    blitFramebuffer(mWidth, mHeight, mDisplaySurfaceWidth, mDisplaySurfaceHeight);
                } else {
                    // GLES 2.0: Draw fullscreen rectangle
                    GLES20.glViewport(0, 0, mDisplaySurfaceWidth, mDisplaySurfaceHeight);
                    mFullScreen.drawFrame(mOffscreenTextures[0], mIdentityMatrix);
                }

                mDisplayWindowSurface.setPresentationTime(timeStampNanos);
                mDisplayWindowSurface.swapBuffers();
            }

            // Blit to encoder surface
            for (Recorder recorder : mRecorders) {
                if (recorder != null && recorder.shouldRecord()) {
                    // Prepare for recording
                    recorder.beginRecord();

                    if (mUseBlitFramebuffer) {
                        // GLES 3.0: glBlitFramebuffer
                        blitFramebuffer(mWidth, mHeight, recorder.mInputSurfaceWidth, recorder.mInputSurfaceHeight);
                    } else {
                        // GLES 2.0: Draw fullscreen rectangle
                        GLES20.glViewport(0, 0, recorder.mInputSurfaceWidth, recorder.mInputSurfaceHeight);
                        mFullScreen.drawFrame(mOffscreenTextures[0], mIdentityMatrix);
                    }

                    // Finish recording
                    recorder.finishRecord(timeStampNanos);
                }
            }

            // Stats frame rate
            mFrameRateStatist.incomingFrame();
            /*
            if (mFrameRateStatist.totalFrameCount() % mFrameRate == 0) {
                LogUtil.d(TAG, "VideoScene-" + mId
                        + " render speed: " + mFrameRateStatist.averageFrameRate());
            }
            */

            synchronized (mRenderStepLock) {
                mRenderStepBlitDone = true;
            }
        }

        private void render2(long timeStampNanos) {
            if (mWeak > 0) {
                --mWeak;
                return;
            }

            try {
                render(timeStampNanos);
            } catch (Exception e) {
                LogUtil.e(TAG, "video scene#" + mId + " render error, flag it with weak: ", e);
                mWeak = 60;
            }
        }

        private void drawBackground(Config config) {
            // Clear color
            float red = Color.red(config.backgroundColor) / 255.0f;
            float green = Color.green(config.backgroundColor) / 255.0f;
            float blue = Color.blue(config.backgroundColor) / 255.0f;

            GLES20.glClearColor(red, green, blue, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Draw image
            /*if (config.backgroundImage != null) {
                // Translate canvas coordinates to OpenGL coordinates
                mBgImgRect.setPosition(0.0f, 0.0f);
                mBgImgRect.setRotation(0.0f);
                mBgImgRect.setScale(1.0f, 1.0f);

                mBgImgRect.draw(mBgImgTexPgm, mDisplayProjectionMatrix);
            }*/
        }

        private void drawOverlays(LinkedList<Overlay> overlays) {
            //synchronized (mCurrentOverlays) {
                for (Overlay overlay : overlays) {
                    if (overlay != null) {
                        overlay.draw();
                    }
                }
            //}
        }

        private void drawTestingItems() {
            mTri.setPosition(mAspect / 2.0f, 0.5f);
            //LogUtil.d(TAG, "mTri: " + mTri.toString());

            float[] displayProjectionMatrix = new float[16];
            Matrix.orthoM(displayProjectionMatrix, 0,0, mAspect,0, 1.0f, -1, 1);
            mTri.draw(mProgram, displayProjectionMatrix);

            mRect.draw(mProgram, mDisplayProjectionMatrix);
        }

        private void drawDisplayName(Config config) {
            if (config.displayName == null || config.displayName.isEmpty()) {
                return;
            }

            if (config.needUpdateDisplayNameBitmap) {
                config.updateDisplayNameImageBuffer();
                // Update texture
                if (config.displayNameTexSlot < 0) {
                    config.displayNameTexSlot = mSharedTexMgr.getIdleSlot(0, 0);
                    //LogUtil.d(TAG, "Config request an texture slot for display name, id=" + config.displayNameTexSlot);
                }
                mSharedTexMgr.updateTexture(config.displayNameTexSlot, config.displayNameImage);
            }

            /*GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSharedImageTexId);
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    config.displayNameImage.getWidth(), config.displayNameImage.getHeight(),
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                    config.displayNameImageBuffer);*/

            float widthF = (float)config.displayNameImage.getWidth() / (float)mWidth;
            float heightF = (float)config.displayNameImage.getHeight() / (float)mHeight;
            float posX = 0.0f;
            float posY = 0.0f;

            switch (config.displayNameAlignment & 0x000F) {
                case ALIGNMENT_LEFT :
                    posX = 0.0f + DISPLAY_NAME_PADDINGS[3];
                    break;
                case ALIGNMENT_HCENTER :
                    posX = 0.5f - widthF / 2.0f;
                    break;
                case ALIGNMENT_RIGHT :
                    posX = 1.0f - widthF - DISPLAY_NAME_PADDINGS[1];
                    break;
            }

            switch (config.displayNameAlignment & 0x00F0) {
                case ALIGNMENT_TOP :
                    posY = 1.0f - heightF - DISPLAY_NAME_PADDINGS[0];
                    break;
                case ALIGNMENT_VCENTER :
                    posY = 0.5f - heightF / 2.0f;
                    break;
                case ALIGNMENT_BOTTOM :
                    posY = 0.0f + DISPLAY_NAME_PADDINGS[2];
                    break;
            }

            mSharedTexMgr.draw(config.displayNameTexSlot, posX, posY, widthF, heightF);

            /*mSharedImageRect.setPosition(posX, posY);
            mSharedImageRect.setRotation(0.0f);
            mSharedImageRect.setScale(widthF, heightF);

            mSharedImageRect.draw(mSharedImageTexPgm, mDisplayProjectionMatrix);*/
        }

        private void drawDebugInfo() {
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = dateFormat.format(now);

            String fpsStr = Double.toString(mFrameRateStatist.averageFrameRate());
            BigDecimal bd = new BigDecimal(fpsStr);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            fpsStr = bd.toString();

            String debugText = "EP VideoEngine / " + timeStr + " / FPS: " + fpsStr;

            // Render as bitmap
            Bitmap image = TextRenderer.renderTextAsBitmap(debugText, null, 24, 100, false, Color.YELLOW, Color.TRANSPARENT, mWidth);
            if (mDebugInfoTexSlot < 0) {
                mDebugInfoTexSlot = mSharedTexMgr.getIdleSlot(0, 0);
            }
            mSharedTexMgr.updateTexture(mDebugInfoTexSlot, image);

            /*ByteBuffer buffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 4);
            image.copyPixelsToBuffer(buffer);
            buffer.position(0);

            // Draw
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSharedImageTexId);
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    image.getWidth(), image.getHeight(),
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);*/

            float widthF = (float) image.getWidth() / (float) mWidth;
            float heightF = (float) image.getHeight() / (float) mHeight;
            float posX = 0.0f;
            float posY = 1.0f - heightF;

            mSharedTexMgr.draw(mDebugInfoTexSlot, posX, posY, widthF, heightF);

            /*mSharedImageRect.setPosition(posX, posY);
            mSharedImageRect.setRotation(0.0f);
            mSharedImageRect.setScale(widthF, heightF);

            mSharedImageRect.draw(mSharedImageTexPgm, mDisplayProjectionMatrix);*/
        }

        private void blitFramebuffer(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
            // Bind the offscreen FBO to the read framebuffer
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, mFramebuffers[0]);

            // Bind the default framebuffer as the draw framebuffer
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0);

            // Specify the entire framebuffer from the offscreen FBO,
            // and specify a small corner area for the blit target area
            GLES30.glBlitFramebuffer(
                    0, 0, srcWidth, srcHeight,
                    0, 0, dstWidth, dstHeight,
                    GLES30.GL_COLOR_BUFFER_BIT, // Color only
                    mTextureFilteringMethod); // Using global filtering method

            // Unbind the read framebuffer
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, 0);
        }

        private void update(long timeStampNanos) {
            // Update transition progress
            mTransition.update(timeStampNanos);

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
                    LogUtil.d(TAG, "Time delta too large: " +
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
            //mTri.setRotation(0.0f);

            // Bounce the rect around the screen.  The rect is a 1x1 square scaled up to NxN.
            // We don't do fancy collision detection, so it's possible for the box to slightly
            // overlap the edges.  We render the edges last, so it's not noticeable.
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

        private int parseAlignment(String alignment) {
            final String ALIGNMENT_REGEXP = "^(left|right|hcenter):(top|bottom|vcenter)$";
            Pattern alignmentRegExpPattern = Pattern.compile(ALIGNMENT_REGEXP);
            Matcher matcher = alignmentRegExpPattern.matcher(alignment);
            if (!matcher.matches()) {
                return 0;
            }

            String horizontal = matcher.group(1);
            String vertical = matcher.group(2);
            int v = 0;
            switch (horizontal) {
                case "left" :
                    v |= ALIGNMENT_LEFT;
                    break;

                case "right" :
                    v |= ALIGNMENT_RIGHT;
                    break;

                case "hcenter" :
                    v |= ALIGNMENT_HCENTER;
                    break;

                default :
                    LogUtil.e(TAG, "Invalid horizontal alignment type!");
                    return 0;
            }

            switch (vertical) {
                case "top" :
                    v |= ALIGNMENT_TOP;
                    break;

                case "bottom" :
                    v |= ALIGNMENT_BOTTOM;
                    break;

                case "vcenter" :
                    v |= ALIGNMENT_VCENTER;
                    break;

                default :
                    LogUtil.e(TAG, "Invalid vertical alignment type!");
                    return 0;
            }

            return v;
        }

        private RectF parseRectangle(String rectangle) {
            final String RECTANGLE_REGEXP = "^((?:0\\.[0-9]+)|(?:1\\.0+))"
                                          + ":((?:0\\.[0-9]+)|(?:1\\.0+))"
                                          + ":((?:0\\.[0-9]+)|(?:1\\.0+))"
                                          + ":((?:0\\.[0-9]+)|(?:1\\.0+))$";
            Pattern rectangleRegExpPattern = Pattern.compile(RECTANGLE_REGEXP);
            Matcher matcher = rectangleRegExpPattern.matcher(rectangle);
            if (!matcher.matches()) {
                return null;
            }

            String left = matcher.group(1);
            String top = matcher.group(2);
            String width = matcher.group(3);
            String height = matcher.group(4);

            float x = Float.valueOf(left);
            float y = Float.valueOf(top);
            float w = Float.valueOf(width);
            float h = Float.valueOf(height);

            return new RectF(x, y, x + w, y + h);
        }

        private StyleSheet parseStyleSheet(String styleSheet) {
            return new StyleSheet(styleSheet);
        }
    }

    private class Sink implements Runnable {
        public static final int INPUT_TYPE_SCENE    = 0;
        public static final int INPUT_TYPE_SOURCE   = 1;

        public static final int STATUS_UNINITIALIZED = 1;
        public static final int STATUS_CONFIGURED = 2;
        public static final int STATUS_RUNNING = 3;

        private static final long MIN_KEY_FRAME_INTERVAL = 1000; // ms

        private int mId;
        private int mInputType;
        private int mSceneId;
        private int mSourceId;
        private String mConfigStr;
        private Config mConfig;

        private Surface mInputSurface;
        private MediaCodec mMediaCodec;
        private long mEncodedFrames;
        private MediaCodec.BufferInfo mBufferInfo;
        private Packet mIDRPacket;

        private Thread mEncodeThread;
        private volatile EncodeHandler mHandler;
        private Timer mPeriodRequestKeyFrameTimer;

        private Object mStatusFence = new Object();
        private volatile int mStatus;

        private long mLastKeyFrameTimeStamp;

        private BitrateStatist mBitrateStatist = new BitrateStatist();
        private FrameRateStatist mFrameRateStatist = new FrameRateStatist();
        private Object mStatFence = new Object();

        private class Config {
            public int      bitrate;
            public String   codec;
            public String   encoder;
            public String   profile;
            public AVCLevel level = AVCLevel.UNSPECIFIED;
            public int      width = 1920;
            public int      height = 1080;
            public int      frameRate = 30;
            public int      keyFrameInterval;

            public Config(String config) {
                JsonObject configObject = (JsonObject)new JsonParser().parse(config);

                final String CONFIG_BITRATE_KEY = "bitrate";
                if (configObject.has(CONFIG_BITRATE_KEY)) {
                    bitrate = configObject.get(CONFIG_BITRATE_KEY).getAsInt();
                }

                final String CONFIG_CODEC_KEY = "codec";
                if (configObject.has(CONFIG_CODEC_KEY)) {
                    codec = configObject.get(CONFIG_CODEC_KEY).getAsString();
                }

                final String CONFIG_ENCODER_KEY = "encoder";
                if (configObject.has(CONFIG_ENCODER_KEY)) {
                    encoder = configObject.get(CONFIG_ENCODER_KEY).getAsString();
                }

                final String CONFIG_PROFILE_KEY = "profile";
                if (configObject.has(CONFIG_PROFILE_KEY)) {
                    profile = configObject.get(CONFIG_PROFILE_KEY).getAsString();
                }

                final String CONFIG_LEVEL_KEY = "level";
                if (configObject.has(CONFIG_LEVEL_KEY)) {
                    int index = configObject.get(CONFIG_LEVEL_KEY).getAsInt();
                    level = AVCLevel.fromIndex(index);
                }

                final String CONFIG_RESOLUTION_KEY = "resolution";
                if (configObject.has(CONFIG_RESOLUTION_KEY)) {
                    String resolution = configObject.get(CONFIG_RESOLUTION_KEY).getAsString();
                    if (resolution.toLowerCase().equals("auto")) {
                        // TODO:
                        LogUtil.w(TAG, "Auto resolution NOT IMPLEMENTED yet. Using 1920x1080 instead.");
                    } else {
                        String[] parts = resolution.split("x");
                        if (parts.length == 2) {
                            width = Integer.parseInt(parts[0]);
                            height = Integer.parseInt(parts[1]);

                            if (width <= 0 || width > MAXIMUM_WIDTH
                                    || height <= 0 || height > MAXIMUM_HEIGHT) {
                                LogUtil.e(TAG, "Invalid resolution: " + resolution);
                            }
                        } else {
                            LogUtil.e(TAG, "Invalid resolution: " + resolution);
                        }
                    }
                }

                final String CONFIG_FRAME_RATE_KEY = "frame_rate";
                if (configObject.has(CONFIG_FRAME_RATE_KEY)) {
                    frameRate = configObject.get(CONFIG_FRAME_RATE_KEY).getAsInt();
                    if (frameRate <= 0 || frameRate > MAXIMUM_FPS) {
                        LogUtil.e(TAG, "Invalid frame rate(" + frameRate + ") for video sink #" + mId + ", set to 1 fps.");
                        frameRate = 1;
                    }
                }

                final String CONFIG_KEY_FRAME_INTERVAL_KEY = "key_frame_interval";
                if (configObject.has(CONFIG_KEY_FRAME_INTERVAL_KEY)) {
                    keyFrameInterval = configObject.get(CONFIG_KEY_FRAME_INTERVAL_KEY).getAsInt();
                    if (keyFrameInterval <= 0 || keyFrameInterval > Integer.MAX_VALUE / 1000) {
                        LogUtil.i(TAG, "Key frame interval(" + keyFrameInterval + ") is out of range. Set to FPS * 3600.");
                        keyFrameInterval = frameRate * 3600;
                    }
                }
            }

            public boolean isValid() {
                // TODO:
                return false;
            }

            public String getMimeType() {
                if (codec.equals("H264")) {
                    return MediaFormat.MIMETYPE_VIDEO_AVC;
                } else if (codec.equals("H265")) {
                    return MediaFormat.MIMETYPE_VIDEO_HEVC;
                } else {
                    LogUtil.e(TAG, "Unsupported video codec " + codec);
                }

                return null;
            }

            public MediaFormat getMediaFormat() {
                String mimeType = getMimeType();
                // Set encoder media format
                MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);

                // Color format
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

                // Profile
                int profile = -1;
                if (mimeType.equals(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    switch (mConfig.profile.toLowerCase()) {
                        case "baseline" :
                            profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;
                            break;
                        case "main" :
                            profile = MediaCodecInfo.CodecProfileLevel.AVCProfileMain;
                            break;
                        case "high" :
                            profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh;
                            break;
                        default :
                            LogUtil.e(TAG, "Invalid profile(" + profile + ") for codec(" + codec + ")");
                    }
                } else if (mimeType.equals(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                    switch (mConfig.profile.toLowerCase()) {
                        case "main" :
                            profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain;
                            break;
                        default :
                            LogUtil.e(TAG, "Invalid profile(" + profile + ") for codec(" + codec + ")");
                    }
                }
                if (profile != -1) {
                    if (!Rockchip.is3BUVersion())
                        format.setInteger(MediaFormat.KEY_PROFILE, profile);
                }

                // Level
                if (level != AVCLevel.UNSPECIFIED) {
                    if (!Rockchip.is3BUVersion())
                        format.setInteger(MediaFormat.KEY_LEVEL, level.android);
                }

                // Bitrate
                format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

                // Frame rate
                format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);

                // I frame interval - DISABLE, self request instead.
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyFrameInterval);

                return format;
            }
        }

        public class Stats {
            private String  codec;
            private long    bitrate;
            private float   frame_rate;
            private String  resolution;
            private String  config;
            private String  level;

            private Stats() {
                codec       = mConfig.codec;
                bitrate     = mBitrateStatist.averageBitrate();
                frame_rate  = mFrameRateStatist.averageFrameRate();
                resolution  = mConfig.width + "x" + mConfig.height;
                config      = mConfigStr;
                level       = mConfig.level.name;
            }
        } // End of class Stats

        private class EncodeHandler extends Handler {
            private static final int MSG_STOP_RECORDING     = 0x01;
            private static final int MSG_FRAME_AVAILABLE    = 0x02;
            private static final int MSG_REQUEST_KEY_FRAME  = 0x03;

            private WeakReference<Sink> mWeakRefSink;

            public EncodeHandler(Sink sink) {
                mWeakRefSink = new WeakReference<>(sink);
            }

            @Override
            public void handleMessage(Message inputMessage) {
                int what = inputMessage.what;

                Sink videoSink = mWeakRefSink.get();
                if (videoSink == null) {
                    LogUtil.w(TAG, "EncodeHandler.handleMessage: sink is null");
                    return;
                }

                switch (what) {
                    case MSG_STOP_RECORDING :
                        videoSink.handleStopRecording();
                        Looper.myLooper().quit();
                        break;

                    case MSG_FRAME_AVAILABLE :
                        videoSink.handleFrameAvailable();
                        break;

                    case MSG_REQUEST_KEY_FRAME :
                        videoSink.handleRequestKeyFrame();
                        break;

                    default:
                        throw new RuntimeException("Unhandled msg what=" + what);
                }
            }
        }

        public Sink(int id, int inputType, int inputObjectId, String config) {
            mId = id;
            mInputType = inputType;
            if (inputType == INPUT_TYPE_SCENE) {
                mSceneId = inputObjectId;
                mSourceId = -1;
            } else if (inputType == INPUT_TYPE_SOURCE) {
                mSceneId = -1;
                mSourceId = inputObjectId;
            }

            mConfigStr = config;
            mConfig = new Config(mConfigStr);
            // TODO: mConfig.isValid()

            try {
                // Create a MediaCodec encoder, and configure it.
                mMediaCodec = MediaCodec.createEncoderByType(mConfig.getMimeType());

                // Config as encoder
                mMediaCodec.configure(mConfig.getMediaFormat(), null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);

                // Create input surface
                mInputSurface = mMediaCodec.createInputSurface();

                mBufferInfo = new MediaCodec.BufferInfo();

                // Start encoder
                mMediaCodec.start();
                mEncodedFrames = 0;
                mLastKeyFrameTimeStamp = 0;

                mEncodeThread = new Thread(this, "VideoEncoder-" + mId);
                mEncodeThread.start();

                // Request key frame periodically
                LogUtil.i(TAG, "Create timer for request key frame periodically.");
                mPeriodRequestKeyFrameTimer = new Timer();
                int period = (int)Math.ceil(1000.0f * mConfig.keyFrameInterval / mConfig.frameRate);
                mPeriodRequestKeyFrameTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        requestKeyFrame();
                    }
                }, period, period);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        protected void finalize() throws Throwable {
            try {
                LogUtil.i(TAG, "Finalizing VideoEngine::Sink object. id=" + mId);
            } finally {
                super.finalize();
            }
        }

        public int getId() {
            return mId;
        }

        public int getInputType() {
            return mInputType;
        }

        public int getSceneId() {
            if (mInputType != INPUT_TYPE_SCENE) {
                LogUtil.e(TAG, "Input type mismatched!");
                return -1;
            }
            return mSceneId;
        }

        public int getSourceId() {
            if (mInputType != INPUT_TYPE_SOURCE) {
                LogUtil.e(TAG, "Input type mismatched!");
                return -1;
            }
            return mSourceId;
        }

        public Config getConfig() {
            return mConfig;
        }

        public Surface getInputSurface() {
            return mInputSurface;
        }

        public void reconfig(String config) {
            // TODO:
        }

        public String query() {
            synchronized (mStatFence) {
                Stats stats = new Stats();
                Gson gson = new Gson();

                return gson.toJson(stats);
            } // End of synchronized (mStatFence)
        }

        public void release() {
            // Stop encoder
            stopRecording();

            // Wait for stopped
            while (true) {
                synchronized (mStatusFence) {
                    if (mStatus == STATUS_UNINITIALIZED) {
                        break;
                    }
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

            // Release media codec
            mMediaCodec.release();
            mMediaCodec = null;
            mInputSurface = null;
            mBufferInfo = null;
            mConfig = null;
            LogUtil.i(TAG, "Video sink id=" + mId + " released!");
        }

        public void frameAvailableSoon() {
            synchronized (mStatusFence) {
                if (mStatus != STATUS_RUNNING) {
                    return;
                }
            }

            //LogUtil.d(TAG, "frameAvailableSoon");
            mHandler.sendMessage(mHandler.obtainMessage(EncodeHandler.MSG_FRAME_AVAILABLE));
        }

        public void requestKeyFrame() {
            synchronized (mStatusFence) {
                if (mStatus != STATUS_RUNNING) {
                    return;
                }
            }

            //LogUtil.d(TAG, "requestKeyFrame");
            mHandler.sendMessage(mHandler.obtainMessage(EncodeHandler.MSG_REQUEST_KEY_FRAME));
        }

        public void stopRecording() {
            synchronized (mStatusFence) {
                if (mStatus != STATUS_RUNNING) {
                    return;
                }
            }

            if (mPeriodRequestKeyFrameTimer != null) {
                mPeriodRequestKeyFrameTimer.cancel();
            }

            //LogUtil.d(TAG, "stopRecording");
            mHandler.sendMessage(mHandler.obtainMessage(EncodeHandler.MSG_STOP_RECORDING));
        }

        @Override
        public void run() {
            // Establish a Looper for this thread, and define a Handler for it.
            Looper.prepare();

            synchronized (mStatusFence) {
                mHandler = new EncodeHandler(this);
                mStatus = STATUS_RUNNING;
                mStatusFence.notify();
            }

            // Enter loop
            Looper.loop();

            synchronized (mStatusFence) {
                mStatus = STATUS_UNINITIALIZED;
                mHandler = null;
                mStatusFence.notify();
            }

            LogUtil.i(TAG, "Video sink thread exited");
        }

        /**
         * Handles notification of an available frame.
         */
        private void handleFrameAvailable() {
            //LogUtil.d(TAG, "handleFrameAvailable");
            drainEncoder(false);
        }

        private void handleRequestKeyFrame() {
            LogUtil.d(TAG, "Video sink id=" + mId + " requestKeyFrame.");

            long now = System.currentTimeMillis();
            if (now >= mLastKeyFrameTimeStamp + MIN_KEY_FRAME_INTERVAL) {
                mLastKeyFrameTimeStamp = now;

                try {
                    Bundle b = new Bundle();
                    b.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                    mMediaCodec.setParameters(b);
                } catch (IllegalStateException e) {
                    LogUtil.e(TAG, "Video sink id=" + mId + " requestKeyFrame failed: "
                            + e.getMessage());
                }
            } else {
                LogUtil.i(TAG, "Video sink requestKeyFrame, got key frame request repeatedly in "
                        + MIN_KEY_FRAME_INTERVAL + "ms. Ignore current request.");
            }
        }

        /**
         * Handles a request to stop encoding.
         */
        private void handleStopRecording() {
            LogUtil.d(TAG, "handleStopRecording");
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

            //LogUtil.d(TAG, "drainEncoder");
            if (endOfStream) {
                //if (VERBOSE) LogManager.d("sending EOS to encoder");
                mMediaCodec.signalEndOfInputStream();
            }

            // Stats frame rate
            synchronized (mStatFence) {
                mFrameRateStatist.incomingFrame();
            }

            while (true) {
                int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // No output available yet
                    if (!endOfStream) {
                        break;      // out of while
                    } else {
                        //if (VERBOSE) LogManager.d("no output available, spinning to await EOS");
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // Not expected for an encoder
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    LogUtil.d(TAG, "Encoder output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    LogUtil.w(TAG, "Unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = mMediaCodec.getOutputBuffer(encoderStatus);
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    if (mBufferInfo.presentationTimeUs < 0) {
                        LogUtil.e(TAG, "!!! Sink error: pts from encoder is invalid "
                                + mBufferInfo.presentationTimeUs);
                    } else if (mBufferInfo.size != 0) {
                        // Adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                        // pack encoded data and launch it
                        onEncodedData2(encodedData);
                    }

                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!endOfStream) {
                            LogUtil.w(TAG, "Reached end of stream unexpectedly");
                        } else {
                            //if (VERBOSE) LogManager.d("end of stream reached");
                        }
                    }

                    break;
                }
            } // End of while (true)
        }

        // implementation for insert sps/pps packet before each key frame
        private void onEncodedData(ByteBuffer encodedData) {
            // Construct packet
            Packet packet = new Packet();
            packet.dts = -1;
            packet.pts = mBufferInfo.presentationTimeUs / 1000;
            packet.mediaType = 2;   // media::MEDIA_TYPE_VIDEO
            if (mConfig.codec.equals("H264")) {
                packet.codecType = 3;   // media::CODEC_TYPE_H264
            } else if (mConfig.codec.equals("H265")) {
                packet.codecType = 4;   // media::CODEC_TYPE_H265
            } else {
                packet.codecType = 0;   // media::CODEC_TYPE_UNKNOWN
            }
            packet.streamIndex = 0;
            packet.isKeyFrame = (((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) ||
                    (((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)));
            packet.isBeginOfFrame = true;
            packet.isEndOfFrame = true;
            packet.isAvccNal = false;

            // Copy payload
            packet.size = mBufferInfo.size;
            packet.data = new byte[mBufferInfo.size];
            encodedData.get(packet.data, 0, mBufferInfo.size);

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                LogUtil.d(TAG, "Update IDR from encoder, sink_id=" + mId);
                mIDRPacket = packet;
            } else if (packet.isKeyFrame && mIDRPacket != null) {
                LogUtil.d(TAG, "Prepend IDR ahead of key frame, sink_id=" + mId);
                mLastKeyFrameTimeStamp = System.currentTimeMillis();
                mIDRPacket.pts = packet.pts - 1;

                // Stats bitrate
                synchronized (mStatFence) {
                    mBitrateStatist.incomingPacket(mIDRPacket.size);
                }

                // Send packet callback
                onEncodedPacketReady(mId, mIDRPacket.serialize());
            }

            // Stats bitrate
            synchronized (mStatFence) {
                mBitrateStatist.incomingPacket(packet.size);
            }

            // Send packet callback
            onEncodedPacketReady(mId, packet.serialize());

            // TODO: Update statistics
        }

        // implementation for attach sps/pps bitstream before each key frame bit stream
        private void onEncodedData2(ByteBuffer encodedData) {
            // Construct packet
            Packet packet = new Packet();
            packet.dts = -1;
            packet.pts = mBufferInfo.presentationTimeUs / 1000;
            packet.mediaType = 2;   // media::MEDIA_TYPE_VIDEO
            if (mConfig.codec.equals("H264")) {
                packet.codecType = 3;   // media::CODEC_TYPE_H264
            } else if (mConfig.codec.equals("H265")) {
                packet.codecType = 4;   // media::CODEC_TYPE_H265
            } else {
                packet.codecType = 0;   // media::CODEC_TYPE_UNKNOWN
            }
            packet.streamIndex = 0;
            packet.isKeyFrame = (((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) ||
                    (((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)));
            packet.isBeginOfFrame = true;
            packet.isEndOfFrame = true;
            packet.isAvccNal = false;

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                LogUtil.d(TAG, "Update IDR from encoder, sink_id=" + mId);
                packet.size = mBufferInfo.size;
                packet.data = new byte[mBufferInfo.size];
                encodedData.get(packet.data, 0, mBufferInfo.size);

                mIDRPacket = packet;
            } else if (packet.isKeyFrame && mIDRPacket != null) {
                LogUtil.d(TAG, "Prepend IDR ahead of key frame, sink_id=" + mId);
                mLastKeyFrameTimeStamp = System.currentTimeMillis();

                packet.size = mIDRPacket.size + mBufferInfo.size;
                packet.data = new byte[mIDRPacket.size + mBufferInfo.size];
                System.arraycopy(mIDRPacket.data, 0, packet.data, 0, mIDRPacket.size);
                encodedData.get(packet.data, mIDRPacket.size, mBufferInfo.size);
            } else {
                // normal frame
                packet.size = mBufferInfo.size;
                packet.data = new byte[mBufferInfo.size];
                encodedData.get(packet.data, 0, mBufferInfo.size);
            }

            // Stats bitrate
            synchronized (mStatFence) {
                mBitrateStatist.incomingPacket(packet.size);
            }

            // Send packet callback
            onEncodedPacketReady(mId, packet.serialize());

            // TODO: Update statistics
        }
    }

    private class MixerThread extends Thread {
        private volatile MixerHandler mMixerHandler;
        private volatile LinkedBlockingQueue<Integer> mReplyQueue;
        private volatile LinkedBlockingQueue<String> mReplySceneStatsQueue;

        private Object mStartLock = new Object();
        private boolean mReady = false;

        private EglCore mEglCore;
        private OffscreenSurface mDummySurface;

        private Map<Integer, Source> mSources   = new TreeMap<>();
        private Map<Integer, Scene> mScenes     = new TreeMap<>();

        private FrameRateStatist mFrameRateStatist = new FrameRateStatist();

        @Override
        public void run() {
            // Create looper
            Looper.prepare();

            // Create handler and reply queue
            mMixerHandler = new MixerHandler(this);
            mReplyQueue = new LinkedBlockingQueue<>();
            mReplySceneStatsQueue = new LinkedBlockingQueue<>();

            // Prepare EGL and OpenGL ES
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

            // Create dummy surface for texture upload
            mDummySurface = new OffscreenSurface(mEglCore, 128, 128);
            mDummySurface.makeCurrent();

            // Query OpenGL ES properties
            int[] maxTextureUnits = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureUnits, 0);
            LogUtil.i(TAG, "[GLES] Maximum texture image units: " + maxTextureUnits[0]);

            int[] maxTextureSize = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
            LogUtil.i(TAG, "[GLES] Maximum texture size: " + maxTextureSize[0]);

            // Notify
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();    // signal waitUntilReady()
            }

            // Enter message looper
            Looper.loop();

            LogUtil.d(TAG, "Mixer thread looper exited!");
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

        public MixerHandler getHandler() {
            return mMixerHandler;
        }

        public EglCore getEglCore() {
            return mEglCore;
        }

        public float getFrameRate() {
            return mFrameRateStatist.averageFrameRate();
        }

        public int waitForReply() {
            try {
                return mReplyQueue.take();
            } catch (InterruptedException ie) {
                LogUtil.e(TAG, ie.toString());
            }

            return -1;
        }

        private void sendReply(int reply) {
            try {
                mReplyQueue.put(reply);
            } catch (InterruptedException ie) {
                LogUtil.e(TAG, ie.toString());
            }
        }

        public String waitForSceneStatsReply() {
            try {
                return mReplySceneStatsQueue.take();
            } catch (InterruptedException ie) {
                LogUtil.e(TAG, ie.toString());
            }

            return "";
        }

        private void sendSceneStatsReply(String stats) {
            try {
                mReplySceneStatsQueue.put(stats);
            } catch (InterruptedException ie) {
                LogUtil.e(TAG, ie.toString());
            }
        }

        private void releaseGl() {
            GlUtil.checkGlError("releaseGl start");

            // TODO:

            mEglCore.makeNothingCurrent();
        }

        private void render(long timeStampNanos) {
            //LogUtil.d(TAG, "Rendering timestamp: " + timeStampNanos / 1000000);
            // Render scenes
            for (Map.Entry<Integer, Scene> entry : mScenes.entrySet()) {
                Scene scene = entry.getValue();
                if (scene != null && scene.shouldRender()) {
                    scene.clearRenderStepFlags();
                    if (mEnableMultiThreadRender) {
                        scene.asyncRender(timeStampNanos);
                    } else {
                        scene.render2(timeStampNanos);
                    }
                }
            }

            if (!mUpdateTextureWhenDraw) {
                // Update all sources textures
                //mDummySurface.makeCurrent();
                for (Map.Entry<Integer, Source> entry : mSources.entrySet()) {
                    Source source = entry.getValue();
                    if (source != null) {
                        source.updateTexture();
                    }
                }
                //mDummySurface.swapBuffers();
            }

            if (mEnableMultiThreadRender) {
                // Wait for all scenes draw all done
                boolean stepDone = false;
                while (!stepDone) {
                    boolean isBreak = false;
                    for (Map.Entry<Integer, Scene> entry : mScenes.entrySet()) {
                        Scene scene = entry.getValue();
                        if (scene != null) {
                            if (!scene.isRenderBlitDone()) {
                                try {
                                    Thread.sleep(1);
                                    isBreak = true;
                                    break;
                                } catch (InterruptedException ie) {
                                    // Ignore
                                }
                            }
                        }
                    }
                    stepDone = !isBreak;
                }
            }

            // Stat fps
            mFrameRateStatist.incomingFrame();
            if (mFrameRateStatist.totalFrameCount() % (int)mDisplayRefreshRate == 0) {
                LogUtil.v(TAG, "MixerThread current speed: "
                        + mFrameRateStatist.averageFrameRate() + " fps, total frames: "
                        + mFrameRateStatist.totalFrameCount());
            }
        }

        private Source getSource(int sourceId) {
            return mSources.get(sourceId);
        }

        private boolean addSource(Source source) {
            // Initialize source in mixer thread
            boolean succ = source.init();

            mSources.put(source.getId(), source);

            // Send reply
            sendReply(MixerHandler.MSG_ADD_SOURCE);

            return succ;
        }

        private boolean removeSource(int sourceId) {
            if (!mSources.containsKey(sourceId)) {
                LogUtil.e(TAG, "Source id=" + sourceId + " not exists!");
                return false;
            }

            mSources.get(sourceId).release();
            mSources.remove(sourceId);

            // Send reply
            sendReply(MixerHandler.MSG_REMOVE_SOURCE);

            return true;
        }

        private boolean addScene(Scene scene) {
            if (mScenes.containsKey(scene.getId())) {
                LogUtil.e(TAG, "Duplicated scene id.");
                return false;
            }

            // Initialize
            scene.init(this);

            // Add to render list
            mScenes.put(scene.getId(), scene);

            // Send reply
            sendReply(MixerHandler.MSG_ADD_SCENE);

            return true;
        }

        private void deleteScene(int id) {
            if (mScenes.containsKey(id)) {
                mScenes.get(id).release();
            }

            mScenes.remove(id);

            // Send reply
            sendReply(MixerHandler.MSG_DELETE_SCENE);
        }

        private boolean reconfigScene(int sceneId, String config) {
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "reconfigScene: Scene id not exists!");
                return false;
            }
            //LogUtil.i(TAG, "Reconfig video scene#" + sceneId + ", config: " + config);

            return mScenes.get(sceneId).reconfig2(config);
        }

        private void queryScene(int sceneId) {
            String stats = "{}";
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "queryScene: Scene id" + "(#" + sceneId + ")"+" not exists!");
            } else {
                stats = mScenes.get(sceneId).query();
            }

            sendSceneStatsReply(stats);
        }

        private boolean attachDisplaySurface(int sceneId, Surface surface) {
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "attachDisplaySurface: Scene id not exists!");
                return false;
            }

            mScenes.get(sceneId).attachDisplaySurface(surface);
            return true;
        }

        private boolean onDisplaySurfaceChanged(int sceneId, int surfaceFormat, Size surfaceSize) {
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "onDisplaySurfaceChanged: Scene id not exists!");
                return false;
            }

            mScenes.get(sceneId).onDisplaySurfaceChanged(surfaceFormat, surfaceSize);
            return true;
        }

        private boolean detachDisplaySurface(int sceneId) {
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "detachDisplaySurface: Scene id not exists!");
                return false;
            }

            mScenes.get(sceneId).detachDisplaySurface();
            return true;
        }

        private boolean addSink(Sink sink) {
            int sceneId = sink.getSceneId();
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "addSink: Scene id not exists!");
                return false;
            }

            mScenes.get(sceneId).addSink(sink);

            // Send reply
            sendReply(MixerHandler.MSG_ADD_SINK);

            return true;
        }

        private boolean deleteSink(int sceneId, int sinkId) {
            // TODO:
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "deleteSink: Scene id not exists!");
                return false;
            }

            mScenes.get(sceneId).deleteSink(sinkId);

            // Send reply
            sendReply(MixerHandler.MSG_DELETE_SINK);

            return true;

        }
    }

    private static class MixerHandler extends Handler implements Choreographer.FrameCallback {
        private static final int MSG_RENDER                     = 0x00;
        private static final int MSG_ADD_SOURCE                 = 0x01;
        private static final int MSG_REMOVE_SOURCE              = 0x02;
        private static final int MSG_ADD_SCENE                  = 0x03;
        private static final int MSG_DELETE_SCENE               = 0x04;
        private static final int MSG_RECONFIG_SCENE             = 0x05;
        private static final int MSG_ATTACH_DISPLAY_SURFACE     = 0x06;
        private static final int MSG_DISPLAY_SURFACE_CHANGED    = 0x07;
        private static final int MSG_DETACH_DISPLAY_SURFACE     = 0x08;
        private static final int MSG_ADD_SINK                   = 0x09;
        private static final int MSG_DELETE_SINK                = 0x0A;
        private static final int MSG_QUERY_SCENE                = 0x0B;

        private WeakReference<MixerThread> mMixerWeakRef;

        public MixerHandler(MixerThread mixer) {
            mMixerWeakRef = new WeakReference<>(mixer);

            // Start choreographer
            LogUtil.i(TAG, "Starting choreographer.");
            Choreographer.getInstance().postFrameCallback(this);
        }

        /*
         * Choreographer callback, called near vsync.
         * @see android.view.Choreographer.FrameCallback#doFrame(long)
         */
        @Override
        public void doFrame(long frameTimeNanos) {
/*
            if (VIDEO_RENDER_FPS >= 59.0f) {
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
*/
            sendRender(frameTimeNanos);

            Choreographer.getInstance().postFrameCallback(this);
        }

        public void handleMessage(Message msg) {
            // Process incoming messages here
            MixerThread mixer = mMixerWeakRef.get();
            if (mixer == null) {
                LogUtil.w(TAG, "MixerHandler.handleMessage: weak ref is null");
                return;
            }

            switch (msg.what) {
                case MSG_RENDER : {
                    // Render frame
                    if (gRenderFrozen) {
                        LogUtil.d(TAG, "gRenderFrozen, skip current rendering");
                        break;
                    }

                    long timestamp = (((long)msg.arg1) << 32) | (((long)msg.arg2) & 0xffffffffL);
                    mixer.render(timestamp);
                    break;
                }

                case MSG_ADD_SOURCE : {
                    if (msg.obj instanceof Source) {
                        Source s = (Source) msg.obj;
                        mixer.addSource(s);
                    }
                    break;
                }

                case MSG_REMOVE_SOURCE : {
                    int sourceId = msg.arg1;
                    mixer.removeSource(sourceId);

                    break;
                }

                case MSG_ADD_SCENE : {
                    if (msg.obj instanceof Scene) {
                        Scene s = (Scene) msg.obj;
                        mixer.addScene(s);
                    } else {
                        LogUtil.e(TAG, "Object not a valid string array.");
                    }

                    break;
                }

                case MSG_DELETE_SCENE : {
                    int sceneId = msg.arg1;
                    mixer.deleteScene(sceneId);
                    break;
                }

                case MSG_RECONFIG_SCENE : {
                    int sceneId = msg.arg1;
                    String config = (String)msg.obj;

                    mixer.reconfigScene(sceneId, config);
                    break;
                }

                case MSG_ATTACH_DISPLAY_SURFACE : {
                    int sceneId = msg.arg1;
                    if (msg.obj instanceof Surface) {
                        Surface surface = (Surface) msg.obj;
                        mixer.attachDisplaySurface(sceneId, surface);
                    }
                    break;
                }

                case MSG_DISPLAY_SURFACE_CHANGED : {
                    int sceneId = msg.arg1;
                    int surfaceFormat = msg.arg2;
                    Size surfaceSize = (Size)msg.obj;
                    mixer.onDisplaySurfaceChanged(sceneId, surfaceFormat, surfaceSize);
                    break;
                }

                case MSG_DETACH_DISPLAY_SURFACE : {
                    int sceneId = msg.arg1;
                    mixer.detachDisplaySurface(sceneId);
                    break;
                }

                case MSG_ADD_SINK : {
                    if (msg.obj instanceof Sink) {
                        Sink sink = (Sink)msg.obj;
                        mixer.addSink(sink);
                    }
                    break;
                }

                case MSG_DELETE_SINK : {
                    int sceneId = msg.arg1;
                    int sinkId = msg.arg2;
                    mixer.deleteSink(sceneId, sinkId);
                    break;
                }

                case MSG_QUERY_SCENE : {
                    int sceneId = msg.arg1;
                    mixer.queryScene(sceneId);
                    break;
                }

                default : {
                    LogUtil.e(TAG, "Unknown message type!");
                }
            }
        }

        private void sendRender(long frameTimeNanos) {
            sendMessage(obtainMessage(MSG_RENDER,
                    (int)(frameTimeNanos >> 32),
                    (int)frameTimeNanos));
        }

        private void sendAddSource(Source source) {
            sendMessage(obtainMessage(MSG_ADD_SOURCE, source));
        }

        private void sendRemoveSource(int sourceId) {
            sendMessage(obtainMessage(MSG_REMOVE_SOURCE, sourceId, -1));
        }

        private void sendAddScene(Scene scene) {
            sendMessage(obtainMessage(MSG_ADD_SCENE, scene));
        }

        private void sendDeleteScene(int sceneId) {
            sendMessage(obtainMessage(MSG_DELETE_SCENE, sceneId, -1));
        }

        private void sendReconfigScene(int sceneId, String config) {
            sendMessage(obtainMessage(MSG_RECONFIG_SCENE, sceneId, -1, config));
        }

        private void sendQueryScene(int sceneId) {
            sendMessage(obtainMessage(MSG_QUERY_SCENE, sceneId, -1));
        }

        private void sendAttachDisplaySurface(int sceneId, Surface surface) {
            sendMessage(obtainMessage(MSG_ATTACH_DISPLAY_SURFACE, sceneId, -1, surface));
        }

        private void sendDisplaySurfaceChanged(int sceneId, int surfaceFormat, Size surfaceSize) {
            sendMessage(obtainMessage(MSG_DISPLAY_SURFACE_CHANGED, sceneId, surfaceFormat, surfaceSize));
        }

        private void sendDetachDisplaySurface(int sceneId) {
            sendMessage(obtainMessage(MSG_DETACH_DISPLAY_SURFACE, sceneId, -1));
        }

        private void sendAddSink(Sink sink) {
            sendMessage(obtainMessage(MSG_ADD_SINK, sink));
        }

        private void sendDeleteSink(int sceneId, int sinkId) {
            sendMessage(obtainMessage(MSG_DELETE_SINK, sceneId, sinkId));
        }
    }

    public static VideoEngine allocateInstance(Context context, boolean enableNDK) {
        return allocateInstance(context, enableNDK, true);
    }

    public static VideoEngine allocateInstance(Context context, boolean enableNDK, boolean showNoSignal) {
        if (mInstance == null) {
            synchronized (VideoEngine.class) {
                if (mInstance ==null) {
                    mInstance = new VideoEngine(context, enableNDK, showNoSignal);
                }
            }
        }
        return mInstance;
    }

    public static VideoEngine getInstance() {
        return mInstance;
    }

    private VideoEngine(Context context, boolean enableNDK, boolean showNoSignal) {
        LogUtil.i(TAG, "Constructing VideoEngine singleton object.");
        mEnableNDK = enableNDK;
        mShowNoSignal = showNoSignal;

        if (context != null) {
            mContext = context;
        }

        // Query board information
        LogUtil.i(TAG, "Board name: " + Build.BOARD);
        LogUtil.i(TAG, "Device: " + Build.DEVICE);
        LogUtil.i(TAG, "Model: " + Build.MODEL);

        // Query display properties
        mDisplayRefreshRate = 0.0f;
        WindowManager manager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        if (manager != null) {
            Display display = manager.getDefaultDisplay();
            if (display != null) {
                display.getRealSize(mDisplaySize);
                mDisplayRefreshRate = display.getRefreshRate();
                LogUtil.i(TAG, "Default display size: " + mDisplaySize.x + "x" + mDisplaySize.y
                        + ", refresh rate: " + mDisplayRefreshRate);
            } else {
                LogUtil.e(TAG, "Get default display failed!");
            }
        } else {
            LogUtil.e(TAG, "Get window service failed!");
        }

        if (mDisplaySize.x == 0 || mDisplaySize.y == 0) {
            LogUtil.w(TAG, "Query display size failed, suppose to 1920x1080.");
            mDisplaySize.set(1920, 1080);
        }

        if (mDisplayRefreshRate < 1.0f || mDisplayRefreshRate > 60.0f) {
            LogUtil.w(TAG, "Query display refresh rate failed, suppose to 60.0.");
            mDisplayRefreshRate = 60.0f;
        }

        // Enumerate local cameras
        if (!mEnableNDK) {
            mCameraMgr = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

            List<String> ids = getCameraIdList();
            mCameraReopeningFlags = new HashMap<>(ids.size());
            for (String id: ids) {
                mCameraReopeningFlags.put(id, new CameraReopenFlag(false, CAMERA_REOPEN_MIN_THRESHOLD, CAMERA_REOPEN_MIN_THRESHOLD));
            }
        }

        // Query HDMI-IN status
        /*while (!mHdmiInputDevice.isPlugged(HdmiInputDevice.HDMI_IN_PORT_2)) {
            LogUtil.w(TAG, "HDMI-IN 2 NOT PLUGGED.");
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                LogUtil.e(TAG, e.toString());
            }
        }*/

        // Query MediaCodecInfo
        /*String info = "";
        MediaCodecInfo[] mediaCodecInfos = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
        for (int i = 0; i < mediaCodecInfos.length; i++) {
            MediaCodecInfo codecInfo = mediaCodecInfos[i];
            if (!codecInfo.isEncoder()) {
                continue;
            }

            info += "Name=" + codecInfo.getName() + ", supported_types:profile_levels=\n";
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                info += "  type=" + types[j] + "\n";
                MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(types[j]);
                //int [] colorFormats = codecCapabilities.colorFormats;
                MediaCodecInfo.CodecProfileLevel[] profileLevels = codecCapabilities.profileLevels;
                for (int k = 0; k < profileLevels.length; k++) {
                    info += "   profile=" + profileLevels[k].profile + ", level=" + profileLevels[k].level + "\n";
                }
            }
        }
        LogUtil.i(TAG, info);*/
        if (!mEnableNDK){
	        // Prepare No Signal image
            if (mShowNoSignal) {
                mNoSignalImage = genHintBitmap(mNoSignalHint);
                mLoadingImage = genHintBitmap(mLoadingHint);
            }

	        // Start mixer thread
	        LogUtil.i(TAG, "Starting video mixer thread.");
	        mMixerThread = new MixerThread();
	        mMixerThread.setName("MixerThread");
	        mMixerThread.start();
	        mMixerThread.waitUntilReady();

	        // Get render handler
	        mMixerHandler = mMixerThread.getHandler();
	        if (mMixerHandler != null) {
	            // TODO: Extra initialize operations
	        }
		}
    }

    public List<String> getCameraIdList() {
        return mCameraHelper.getConnectedCameras();
    }

    public float getDisplayRefreshRate() {
        return mDisplayRefreshRate;
    }

    public int registerDisplaySurface(Surface surface) {
        if (surface == null) {
            LogUtil.e(TAG, "Display surface CAN NOT be null.");
            return -1;
        }

        if (mEnableNDK) {
            return registerDisplaySurfaceNDK(surface);
        } else {

            // TODO: Check has already registered

            for (int i = 0; i < MAXIMUM_DISPLAY_SURFACE_COUNT; ++i) {
                if (mDisplaySurfaces[i] == null) {
                    mDisplaySurfaces[i] = surface;
                    return i;
                }
            }
        }

        LogUtil.e(TAG, "Can not register more display surface, max=" + MAXIMUM_DISPLAY_SURFACE_COUNT);
        return -1;
    }

    public boolean unregisterDisplaySurface(int surfaceId) {
        if (mEnableNDK) {
            return unregisterDisplaySurfaceNDK(surfaceId);
        } else {
            if (surfaceId > -1 && surfaceId < MAXIMUM_DISPLAY_SURFACE_COUNT) {
                mDisplaySurfaces[surfaceId] = null;
                return true;
            }
        }
        LogUtil.e(TAG, "Can not unregister displayId" + surfaceId);
        return false;
    }

    public boolean addSource(int id, int sourceType, String config) {
        if (mSources.containsKey(id)) {
            LogUtil.e(TAG, "Add source failed! - Duplicate source id=" + id);
            return false;
        }

        Source s = new Source(id, sourceType, config);

        // Add source to mixer thread
        mMixerHandler.sendAddSource(s);

        // Wait for mixer done
        if (MixerHandler.MSG_ADD_SOURCE == mMixerThread.waitForReply()) {
            LogUtil.d(TAG, "Add source successfully!");
        } else {
            LogUtil.e(TAG, "Invalid reply from mixer thread.");
            return false;
        }

        mSources.put(id, s);
        if (sourceType == SOURCE_TYPE_CAPTURE)
            mCameras.put(s.mCameraId, s);

        return true;
    }

    public boolean enqueueSourcePacket(int sourceId, byte[] packet) {
        if (!mSources.containsKey(sourceId)) {
            LogUtil.e(TAG, "Source id=" + sourceId +" not exists!");
            return false;
        }

        return mSources.get(sourceId).enqueuePacket(packet);
    }

    public boolean removeSource(int sourceId) {
        if (!mSources.containsKey(sourceId)) {
            LogUtil.e(TAG, "Source id=" + sourceId +" not exists!");
            return false;
        }

        // Remove source from mixer thread
        mMixerHandler.sendRemoveSource(sourceId);

        // Wait for mixer done
        if (MixerHandler.MSG_REMOVE_SOURCE == mMixerThread.waitForReply()) {
            LogUtil.d(TAG, "Source id=" + sourceId + " removed successfully!");
        } else {
            LogUtil.e(TAG, "Invalid reply from mixer thread.");
            return false;
        }

        Source source = mSources.remove(sourceId);
        if (source != null && source.mType == SOURCE_TYPE_CAPTURE)
            mCameras.remove(source.mCameraId);

        return true;
    }

    public String querySource(int sourceId) {
        if (!mSources.containsKey(sourceId)) {
            LogUtil.e(TAG, "Source id=" + sourceId +" not exists!");
            return "";
        }

        String stats = mSources.get(sourceId).query();
        //LogUtil.d(TAG, stats);

        return stats;
    }

    public boolean hasSignal(int sourceId) {
        Source source = mSources.get(sourceId);
        return source == null ? false : source.hasSignal();
    }

    public boolean hasCameraSignal(String cameraId) {
        Source source = mCameras.get(cameraId);
        return source == null ? false : source.hasSignal();
    }

    public boolean attachDisplaySurfaceToSource(int sourceId, int surfaceId) {
        if (!mSources.containsKey(sourceId)) {
            LogUtil.e(TAG, "Source id not exists!");
            return false;
        }

        if (surfaceId < 0 || surfaceId >= MAXIMUM_DISPLAY_SURFACE_COUNT) {
            LogUtil.e(TAG, "Surface id out of range.");
            return false;
        }

        if (mDisplaySurfaces[surfaceId] == null) {
            LogUtil.e(TAG, "Surface with id=" + surfaceId + " hasn't register yet.");
            return false;
        }

        mSurfaceToSource.put(surfaceId, sourceId);

        Surface surface = mDisplaySurfaces[surfaceId];
        mSources.get(sourceId).attachDisplaySurface(surface);

        return true;
    }

    public boolean detachDisplaySurfaceFromSource(int surfaceId) {
        // Find scene by surface
        if (!mSurfaceToSource.containsKey(surfaceId)) {
            LogUtil.e(TAG, "Can not find source by surface id=" + surfaceId);
            return false;
        }

        int sourceId = mSurfaceToSource.get(surfaceId);
        if (!mSources.containsKey(sourceId)) {
            LogUtil.e(TAG, "detachDisplaySurface: Source id=" + sourceId + " not exists!");
            return false;
        }

        Surface surface = mDisplaySurfaces[surfaceId];
        mSources.get(sourceId).detachDisplaySurface(surface);

        return true;
    }

    public boolean createScene(int id, int width, int height, int frameRate) {
        if (mScenes.containsKey(id)) {
            LogUtil.e(TAG, "Create scene failed! - Duplicate scene id=" + id);
            return false;
        }

        if (width <= 0 || width > MAXIMUM_WIDTH
                || height <= 0 || height > MAXIMUM_HEIGHT
                || frameRate <= 0 || frameRate > MAXIMUM_FPS) {
            LogUtil.e(TAG, "Invalid parameters for createScene. width="
                    + width + ", height=" + height + ", fps=" + frameRate);
            return false;
        }

        Scene s = new Scene(id, width, height, frameRate);
        mScenes.put(id, s);

        // Add scene to mixer thread
        mMixerHandler.sendAddScene(s);

        // Wait for mixer done
        if (MixerHandler.MSG_ADD_SCENE == mMixerThread.waitForReply()) {
            LogUtil.d(TAG, "New scene created successfully!");
        } else {
            LogUtil.e(TAG, "Invalid reply from mixer thread.");
            return false;
        }

        return true;
    }

    public boolean attachDisplaySurfaceToScene(int sceneId, int surfaceId) {
        if (!mScenes.containsKey(sceneId)) {
            LogUtil.e(TAG, "Scene id not exists!");
            return false;
        }

        if (surfaceId < 0 || surfaceId >= MAXIMUM_DISPLAY_SURFACE_COUNT) {
            LogUtil.e(TAG, "Surface id out of range.");
            return false;
        }

        if (mDisplaySurfaces[surfaceId] == null) {
            LogUtil.e(TAG, "Surface with id=" + surfaceId + " hasn't register yet.");
            return false;
        }

        mSurfaceToScene.put(surfaceId, sceneId);

        Surface surface = mDisplaySurfaces[surfaceId];
        mMixerHandler.sendAttachDisplaySurface(sceneId, surface);

        if (mDisplaySurfaceSizes[surfaceId] != null) {
            mMixerHandler.sendDisplaySurfaceChanged(sceneId, mDisplaySurfaceFormats[surfaceId],
                    mDisplaySurfaceSizes[surfaceId]);
        }

        return true;
    }

    public boolean onDisplaySurfaceChanged(int surfaceId, int format, int width, int height) {
        if (mEnableNDK) {
            return onDisplaySurfaceChangedNDK(surfaceId, format, width, height);
        } else {
            if (surfaceId < 0 || surfaceId >= MAXIMUM_DISPLAY_SURFACE_COUNT) {
                LogUtil.e(TAG, "Surface id out of range.");
                return false;
            }
            mDisplaySurfaceFormats[surfaceId] = format;
            mDisplaySurfaceSizes[surfaceId] = new Size(width, height);

            // Find scene by surface
            if (!mSurfaceToScene.containsKey(surfaceId)) {
                LogUtil.w(TAG, "No scene attached to surface id=" + surfaceId);
                return true;
            }

            int sceneId = mSurfaceToScene.get(surfaceId);
            if (!mScenes.containsKey(sceneId)) {
                LogUtil.e(TAG, "onDisplaySurfaceChanged: Scene id not exists!");
                return false;
            }

            mMixerHandler.sendDisplaySurfaceChanged(sceneId, format, new Size(width, height));

            return true;
        }
    }

    public boolean detachDisplaySurfaceFromScene(int surfaceId) {
        // Find scene by surface
        if (!mSurfaceToScene.containsKey(surfaceId)) {
            LogUtil.e(TAG, "Can not find scene by surface id=" + surfaceId);
            return false;
        }

        int sceneId = mSurfaceToScene.get(surfaceId);
        if (!mScenes.containsKey(sceneId)) {
            LogUtil.e(TAG, "detachDisplaySurface: Scene id=" + sceneId + " not exists!");
            return false;
        }

        mMixerHandler.sendDetachDisplaySurface(sceneId);

        return true;
    }

    public boolean deleteScene(int sceneId) {
        if (!mScenes.containsKey(sceneId)) {
            LogUtil.e(TAG, "deleteScene: Scene id=" + sceneId + " not exists!");
            return false;
        }

        mScenes.remove(sceneId);
        mMixerHandler.sendDeleteScene(sceneId);

        // Wait for mixer done
        if (MixerHandler.MSG_DELETE_SCENE == mMixerThread.waitForReply()) {
            LogUtil.d(TAG, "Scene deleted successfully!");
        } else {
            LogUtil.e(TAG, "Invalid reply from mixer thread.");
            return false;
        }

        return true;
    }

    public boolean reconfigScene(int sceneId, String config) {
        if (!mScenes.containsKey(sceneId)) {
            LogUtil.e(TAG, "reconfigScene: Scene id=" + sceneId + " not exists!");
            return false;
        }

        mMixerHandler.sendReconfigScene(sceneId, config);

        return true;
    }

    public String queryScene(int sceneId) {
        if (!mScenes.containsKey(sceneId)) {
            LogUtil.e(TAG, "Scene id=" + sceneId +" not exists!");
            return "";
        }

        //LogUtil.i(TAG, stats);
        mMixerHandler.sendQueryScene(sceneId);

        return mMixerThread.waitForSceneStatsReply();
    }

    public boolean createSinkFromScene(int id, int sceneId, String config) {
        if (mSinks.containsKey(id)) {
            LogUtil.e(TAG, "Create sink failed! - Duplicate sink id=" + id);
            return false;
        }

        Sink sink = new Sink(id, Sink.INPUT_TYPE_SCENE, sceneId, config);
        mSinks.put(id, sink);

        // Add sink to mixer thread
        mMixerHandler.sendAddSink(sink);

        // Wait for mixer done
        if (MixerHandler.MSG_ADD_SINK == mMixerThread.waitForReply()) {
            LogUtil.d(TAG, "New sink created successfully!");
        } else {
            LogUtil.e(TAG, "Invalid reply from mixer thread.");
            return false;
        }

        return true;
    }

    public boolean createSinkFromSource(int id, int sourceId, String config) {
        if (mSinks.containsKey(id)) {
            LogUtil.e(TAG, "Create sink failed! - Duplicate sink id=" + id);
            return false;
        }

        if (!mSources.containsKey(sourceId)) {
            LogUtil.e(TAG, "Source id=" + sourceId +" not exists!");
            return false;
        }

        Sink sink = new Sink(id, Sink.INPUT_TYPE_SOURCE, sourceId, config);
        mSinks.put(id, sink);

        // Add sink to source
        if (mSources.get(sourceId).addSink(sink)) {
            LogUtil.d(TAG, "New sink created successfully!");
            return true;
        } else {
            LogUtil.e(TAG, "Create video sink from source failed!");
            return false;
        }
    }

    public boolean deleteSink(int sinkId) {
        if (!mSinks.containsKey(sinkId)) {
            LogUtil.e(TAG, "deleteSink: Sink id=" + sinkId + " not exists!");
            return false;
        }

        Sink sink = mSinks.get(sinkId);

        if (sink.getInputType() == Sink.INPUT_TYPE_SCENE) {
            mMixerHandler.sendDeleteSink(sink.getSceneId(), sinkId);

            // Wait for mixer done
            if (MixerHandler.MSG_DELETE_SINK == mMixerThread.waitForReply()) {
                LogUtil.d(TAG, "Sink deleted successfully!");
            } else {
                LogUtil.e(TAG, "Invalid reply from mixer thread.");
                return false;
            }
        } else if (sink.getInputType() == Sink.INPUT_TYPE_SOURCE) {
            if (mSources.get(sink.getSourceId()).deleteSink(sink)) {
                LogUtil.d(TAG, "Sink deleted successfully!");
            } else {
                LogUtil.e(TAG, "Delete video sink from source failed!");
                return false;
            }
        }

        // Release sink
        sink.release();
        mSinks.remove(sinkId);

        return true;
    }

    public String querySink(int sinkId) {
        if (!mSinks.containsKey(sinkId)) {
            LogUtil.e(TAG, "querySink: Sink id=" + sinkId + " not exists!");
            return "";
        }

        return mSinks.get(sinkId).query();
    }

    public boolean reconfigSink(int sinkId, String config) {
        // TODO:
        return false;
    }

    public boolean requestKeyFrame(int sinkId) {
        if (!mSinks.containsKey(sinkId)) {
            LogUtil.e(TAG, "requestKeyFrame: Sink id=" + sinkId + " not exists!");
            return false;
        }

        mSinks.get(sinkId).requestKeyFrame();

        return true;
    }

    public float queryMixerFrameRate() {
        // NOTE: Not thread-safe, only be used for test
        return mMixerThread.getFrameRate();
    }

    private static final String FILE_PREFIX = "file://";
    private static final String TEXT_PREFIX = "text://";

    public static void setNoSignalImage(String path) {
        mNoSignalHint = path.startsWith(FILE_PREFIX) ? path : (FILE_PREFIX + path);
    }

    public static void setNoSignalText(String text) {
        mNoSignalHint = TEXT_PREFIX + text;
    }

    public static void setLoadingImage(String path) {
        mLoadingHint = path.startsWith(FILE_PREFIX) ? path : (FILE_PREFIX + path);
    }

    public static void setLoadingText(String text) {
        mLoadingHint = TEXT_PREFIX + text;
    }

    private static Bitmap genHintBitmap(String hint) {
        if (hint.startsWith(TEXT_PREFIX)) {
            String text = hint.substring(TEXT_PREFIX.length());
            if (StringUtil.isEmpty(text))
                throw new RuntimeException("genHintBitmap, invalid hint: " + hint);
            return TextRenderer.renderTextAsBitmap(text,
                    null, 48,
                    100, false,
                    0xffffffff, 0x00000000);
        } else if (hint.startsWith(FILE_PREFIX)) {
            String path = hint.substring(FILE_PREFIX.length());
            if (StringUtil.isEmpty(path))
                throw new RuntimeException("genHintBitmap, invalid hint: " + hint);
            return BitmapFactory.decodeFile(path);
        } else {
            throw new RuntimeException("genHintBitmap, invalid hint: " + hint);
        }
    }

    private static class CameraReopenFlag {
        public volatile boolean trying;
        public volatile long attempts;
        public volatile long threshold;

        public CameraReopenFlag(boolean trying, long attempts, long threshold) {
            this.trying = trying;
            this.attempts = attempts;
            this.threshold = threshold;
        }
    }

    private void onSourceStateChanged(int sourceId, int sourceType, boolean ready) {
        onSDKEvent(MediaEventId.SOURCE_DECODING_STATE_CHANGED.value, sourceId, sourceType, ready ? "true" : "false");
    }

    private native int onEncodedPacketReady(int sinkId, byte[] packetData);
    private native void onSDKEvent(int eventId, int arg1, int arg2, String data);

    private native int registerDisplaySurfaceNDK(Surface surface);
    private native boolean unregisterDisplaySurfaceNDK(int surfaceID);
    private native boolean onDisplaySurfaceChangedNDK(int surfaceId, int format, int width, int height);
}
