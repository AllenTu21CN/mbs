package cn.sanbu.avalon.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.sanbu.board.HDMIFormat;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera {
    private static String TAG = "avalon_" + Camera.class.getSimpleName();
    private Context mContext = null;

    int mCameraID = 0;
    private CameraManager mCameraMgr;
    private CameraDevice mCameraDevice;
    private int mCaptureWidth = -1;
    private int mCaptureHeight = -1;
    private HandlerThread mCameraBackgroundThread;
    private SurfaceTexture mOutputSurfaceTexture;
    private CaptureRequest.Builder mCameraPreviewRequestBuilder;
    private Object mCameraCaptureSessionLock = new Object();
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCameraPreviewRequest;
    private LinkedList<Surface> mAttachedDisplaySurfaces = new LinkedList<>();
    private LinkedList<Integer> mAttachedSinks = new LinkedList<>();
    private final boolean mCaptureSizeAutoFit = true;
    private HdmiInputDevice mHdmiInputDevice = HdmiInputDevice.getInstance();
    private volatile boolean mCameraReopening = false;
    private volatile boolean mCameraClosing   = false;
    private volatile boolean mCameraCaptureSessionCreating;
    private volatile boolean mCameraCaptureSessionConfigured;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    public Camera(int width, int height, int texName) {
        Log.i(TAG, "Use texture id: " + texName);
        mCaptureWidth           = width;
        mCaptureHeight          = height;
        mOutputSurfaceTexture   = new SurfaceTexture(texName);
    }

    public Camera(int width, int height, SurfaceTexture surfaceTexture) {
        mCaptureWidth           = width;
        mCaptureHeight          = height;
        mOutputSurfaceTexture   = surfaceTexture;
    }

    public SurfaceTexture getOutputSurfaceTexture() {
        return mOutputSurfaceTexture;
    }

    public void attachDisplaySurface(Surface surface) {
        mAttachedDisplaySurfaces.add(surface);
        recreateCameraPreviewSession();
    }

    public void detachDisplaySurface(Surface surface) {
        Log.d(TAG,"detach display surface camera#" + mCameraID);
        if (mAttachedDisplaySurfaces.remove(surface)) {
            recreateCameraPreviewSession();
        }
    }

    public void addSink(int id) {
        mAttachedSinks.add(id);
    }

    public void removeSink(int id) {
        mAttachedSinks.remove(Integer.valueOf(id));
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.
            Log.i(TAG, "CameraDevice onOpened!");
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            mCameraReopening = false;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e(TAG, "CameraDevice onDisconnected!");
            mCameraClosing = true;
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mCameraReopening = false;
            mCameraClosing = false;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "CameraDevice onError!-" + error);
            mCameraClosing = true;
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mCameraReopening = false;
            mCameraClosing   = false;
        }
    };

    private Handler mCameraBackgroundHandler;

    public void reopen(int cameraID, Context context, int width, int height) {
        if (mCameraReopening || mCameraDevice != null) {
            return;
        }

        // If auto fit capture size, use current resolution
        if (mCaptureSizeAutoFit) {
            // Re-open only HDMI-IN is plugged
            int port = mHdmiInputDevice.cameraIdToHdmiDeviceId(cameraID);
            if (!mHdmiInputDevice.isPlugged(port)) {
                return;
            }

            // Always wait for format valid, otherwise query format opertion may conflicts
            // open camera operation which leads always unplugged.
            HDMIFormat fmt = mHdmiInputDevice.queryFormat(port);
            if (fmt == null) {
                return;
            }
            Log.d(TAG, "Using detected size=" + fmt.width + "x" + fmt.height + " for capture.");
            mCaptureWidth = fmt.width;
            mCaptureHeight = fmt.height;
        } else {
            mCaptureWidth       = width;
            mCaptureHeight      = height;
        }

        Log.i(TAG, "Reopening camera: " + cameraID + "...");
        mCameraReopening    = true;
        mCameraID           = cameraID;
        mContext            = context;

        close();

        open();
        Log.i(TAG, "reopen camera end");
    }

    private void open() {
        Log.i(TAG,"Open camera#" + mCameraID);

        mCameraMgr = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            // Prepare background thread and handler for repeating capture request.
            if (mCameraBackgroundThread != null) {
                Log.e(TAG, "Camera background thread already started!");
                return;
            }

            mCameraBackgroundThread = new HandlerThread("CameraBackground");
            mCameraBackgroundThread.start();
            mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());

            mCameraMgr.openCamera(String.valueOf(mCameraID), mCameraStateCallback, mCameraBackgroundHandler);

            // Waiting for camera ready
            synchronized (mCameraCaptureSessionLock) {
                if (!mCameraCaptureSessionConfigured) {
                    mCameraCaptureSessionLock.wait();
                }
            }
        } catch (CameraAccessException e) {
            // Open failed, release lock
            mCameraOpenCloseLock.release();
            Log.e(TAG, "Got CameraAccessException when opening camera. " + e.toString());
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while trying to lock camera opening.");
        } catch (SecurityException e) {
            Log.e(TAG, "Got SecurityException when opening camera. " + e.toString());
        } catch (Exception e) {
            Log.e(TAG, "Got unknown exception when opening camera. " + e.toString());
        }
    }

    public void close() {
        Log.i(TAG,"Close camera#" + mCameraID);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Time out waiting to lock camera closing.");
            }

            destroyCameraPreviewSession();

            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while trying to lock camera closing :" + e.toString());
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
                Log.e(TAG, e.toString());
            }
        }

        int port = mHdmiInputDevice.cameraIdToHdmiDeviceId(mCameraID);
        mHdmiInputDevice.clearCache(port);
    }

    private void createCameraPreviewSession() {
        Log.i(TAG, "Creating camera#" + mCameraID + " preview session.");
        if (mCameraCaptureSessionCreating || mCameraCaptureSessionConfigured) {
            Log.e(TAG, "Invalid camera session status: creating="
                    + mCameraCaptureSessionCreating + ", configured="
                    + mCameraCaptureSessionConfigured);
            return;
        }

        // Set capture size
        if (mCaptureWidth <= 0 || mCaptureHeight <= 0) {
            // TODO: Query input resolution from hardware
            Log.i(TAG, "No capture size specified, using 1920x1080");
            mCaptureWidth = 1920;
            mCaptureHeight = 1080;
        }

        Log.i(TAG, "Set device#" + mCameraID + " capture size to " + mCaptureWidth + "x" + mCaptureHeight);
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

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(
                    output_surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onActive(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.i(TAG, "onActive ...." + cameraCaptureSession.toString());
                        }

                        public void onReady(@NonNull CameraCaptureSession session) {
                            Log.i(TAG, "onReady ....");
                        }

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "CameraCaptureSession onConfigured! " + cameraCaptureSession.toString());
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                Log.e(TAG, "The camera is already closed");
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                // Disable 3A settings for HDMI-IN devices
                                mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE,
                                        CaptureRequest.CONTROL_MODE_OFF);

                                // For device which has built-in camera
                                //mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                //        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                                //mCameraPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                                //        CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ);

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
                                                for (int id : mAttachedSinks) {
                                                    frameAvailableSoon(id);
                                                }

                                                // Stat fps for capture device
//                                                statFrameRate();
                                            }

                                            @Override
                                            public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                                                        @NonNull CaptureRequest request,
                                                                        @NonNull CaptureFailure failure) {
                                                super.onCaptureFailed(session, request, failure);

                                                Log.e(TAG, "CameraCaptureSession.onCaptureFailed: " + failure.getReason());
                                                // TODO:
                                            }
                                        }, mCameraBackgroundHandler);

                                synchronized (mCameraCaptureSessionLock) {
                                    mCameraCaptureSessionCreating = false;
                                    mCameraCaptureSessionConfigured = true;
                                    mCameraCaptureSessionLock.notify();
                                }
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "setRepeatingRequest failed: " + e.toString());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "CameraCaptureSession onConfigureFailed!");
                            synchronized (mCameraCaptureSessionLock) {
                                mCameraCaptureSessionCreating = false;
                                mCameraCaptureSessionConfigured = false;
                                mCameraCaptureSessionLock.notify();
                            }
                        }

                        @Override
                        public void onClosed(@NonNull CameraCaptureSession session) {
                            Log.i(TAG, "CameraCaptureSession onClosed!" + session.toString());
                            synchronized (mCameraCaptureSessionLock) {
                                mCameraCaptureSessionCreating = false;
                                mCameraCaptureSessionConfigured = false;
                                mCameraCaptureSessionLock.notify();
                            }
                        }
                    }, mCameraBackgroundHandler
            );
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCaptureSession failed: " + e.toString());
        }
    }

    private void destroyCameraPreviewSession() {
        Log.i(TAG, "Destroying camera#" + mCameraID + " preview session.");
        if (null != mCameraCaptureSession) {
            Log.i(TAG, "Close CameraCaptureSession:" + mCameraCaptureSession.toString());
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    private void recreateCameraPreviewSession() {
        if (mCameraDevice == null) {
            Log.e(TAG, "Camera: " + mCameraID + " not opened.");
            return;
        }

        if (mCameraClosing) return;

        try {
            Log.d(TAG, "recreateCameraPreviewSession camera#" + mCameraID);
            // Destroy camera capture session
            destroyCameraPreviewSession();

            // Waiting for camera session closed
            synchronized (mCameraCaptureSessionLock) {
                if (mCameraCaptureSessionConfigured) {
                    mCameraCaptureSessionLock.wait();
                }
            }
            Log.i(TAG, "mCameraCaptureSessionConfigured :" + mCameraCaptureSessionConfigured + "mCameraCaptureSession: " + mCameraCaptureSession);

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
            Log.e(TAG, "Recreate camera preview session fail: " + ie.toString());
        }
    }

    private native void frameAvailableSoon(int sink_id);
}
