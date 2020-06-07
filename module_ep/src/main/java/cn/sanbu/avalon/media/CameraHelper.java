package cn.sanbu.avalon.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Size;

import com.sanbu.board.BoardSupportClient;
import com.sanbu.board.HDMIFormat;
import com.sanbu.board.Qualcomm;
import com.sanbu.board.Rockchip;
import com.sanbu.tools.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CameraHelper {

    private static final String TAG = "avalon_" + CameraHelper.class.getSimpleName();

    private static final Size DEFAULT_SIZE_4_NORMATIVE = new Size(1280, 720);

    private static CameraHelper gInstance = null;

    public static void init(Context context, BoardSupportClient client) {
        synchronized (CameraHelper.class) {
            if (gInstance == null)
                gInstance = new CameraHelper(context, client);
        }
    }

    public static CameraHelper getInstance() {
        return gInstance;
    }

    private final boolean mIsNormative;

    private final CameraManager mCameraManager;

    private volatile Map<String, Boolean/*availability*/> mCameras;
    private volatile Map<String, List<Size>> mCamerasPreviewSizes;
    private BoardSupportClient mBoardSupportClient;

    private CameraHelper(Context context, BoardSupportClient client) {
        mBoardSupportClient = client;

        mIsNormative = (!Qualcomm.isVT6105() && !Rockchip.is3BUVersion());

        final boolean waitAvailability = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Looper.myLooper() != null;
        if (!waitAvailability)
            LogUtil.w(TAG, "non-support waitAvailability");

        mCameras = new HashMap<>(5);
        mCamerasPreviewSizes = new HashMap<>(5);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] ids = mCameraManager.getCameraIdList();
            for (String id: ids) {
                try {
                    mCameras.put(id, !waitAvailability);

                    if (isNormative(id)) {
                        CameraCharacteristics camCaps = mCameraManager.getCameraCharacteristics(String.valueOf(id));
                        StreamConfigurationMap configMap = camCaps.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        Size outSizes[] = configMap.getOutputSizes(SurfaceTexture.class);
                        List<Size> sizes = new ArrayList<>(outSizes.length);
                        for (Size size : outSizes)
                            sizes.add(size);
                        mCamerasPreviewSizes.put(id, sizes);
                    }
                } catch (Exception e) {
                    LogUtil.w(TAG, "ignore removable camera: " + id);
                }
            }

            if (waitAvailability)
                mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, null);
        } catch (Exception e) {
            LogUtil.w(TAG, "getCameraIdList failed", e);
        }
    }

    public String[] getCameraIdList() {
        try {
            return mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            LogUtil.w(TAG, "getCameraIdList error", e);
            return null;
        }
    }

    public CameraCharacteristics getCameraCharacteristics(String cameraId) {
        try {
            return mCameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            LogUtil.w(TAG, "getCameraCharacteristics error", e);
            return null;
        }
    }

    public boolean isNormative(String cameraId) {
        // camera is normative with SDK
        return mIsNormative;
    }

    public List<String> getConnectedCameras() {
        return new ArrayList<>(mCameras.keySet());
    }

    public void flush(String cameraId) {
        if (!isNormative(cameraId)) {
            int port = cameraId2HDMIPort(cameraId);
            mBoardSupportClient.startQueryingHDMIIn(port);
        }
    }

    public boolean isConnected(String cameraId) {
        // 1. is connected with camera manager
        if (!mCameras.containsKey(cameraId))
            return false;

        if (isNormative(cameraId))
            return true;

        // 2. is plugged if the camera is hdmi actually
        int port = cameraId2HDMIPort(cameraId);
        return mBoardSupportClient.isHDMIInPlugged(port);
    }

    public boolean isAvailable(String cameraId) {
        // 1. is connected
        if (!isConnected(cameraId))
            return false;

        // 2. is available for camera manager
        boolean availability = mCameras.get(cameraId);
        if (!availability)
            return false;

        if (isNormative(cameraId))
            return true;

        int port = cameraId2HDMIPort(cameraId);

        // 3. has got valid format for dummy camera
        HDMIFormat format = mBoardSupportClient.getHDMIInFormat(port);
        if (format.width <= 0 || format.height <= 0)
            return false;

        // 4. allow to open for dummy camera (not in querying otherwise query format
        // operation may conflicts open camera operation which leads always unplugged.)
        if (mBoardSupportClient.isQueryingHDMIIn(port))
            return false;

        return true;
    }

    public Size getDefaultSize(String cameraId) {
        if (!isConnected(cameraId))
            return null;

        if (isNormative(cameraId)) {
            List<Size> sizes = mCamerasPreviewSizes.get(cameraId);
            if (sizes == null)
                return null;
            if (sizes.contains(DEFAULT_SIZE_4_NORMATIVE))
                return DEFAULT_SIZE_4_NORMATIVE;
            else
                return sizes.get(0);
        } else {
            int port = cameraId2HDMIPort(cameraId);
            HDMIFormat format = mBoardSupportClient.getHDMIInFormat(port);
            return new Size(format.width, format.height);
        }
    }

    public int cameraId2HDMIPort(String cameraId) {
        return mBoardSupportClient.cameraIdToHdmiDeviceId(cameraId);
    }

    public String HDMIPort2CameraId(int port) {
        return mBoardSupportClient.hdmiDeviceIdToCameraId(port);
    }

    private CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            mCameras.put(cameraId, true);
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            mCameras.put(cameraId, false);
        }
    };
}
