package cn.sanbu.avalon.endpoint3.utils;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;

import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.tools.LocalLinuxUtil;
import com.sanbu.tools.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.sanbu.avalon.media.CameraHelper;

public class SysResChecking {

    private static final String TAG = SysResChecking.class.getSimpleName();

    public static class Port {
        public String protocol;
        public String number;

        public Port(String protocol, String number) {
            this.protocol = protocol;
            this.number = number;
        }
    }

    public static void check(Context context, Handler handler, Callback callback,
                                      final List<Port> ports, final List<String> cameras) {
        checkLocalPortsUsing(ports, handler, result -> {
            if (!result.isSuccessful()) {
                Result error = genError("checkResource", result.code,
                        "checkLocalPortsUsing failed: " + result.getMessage(), "检查端口失败: " + result.getMessage());
                callback.done(error);
                return;
            }

            List<String> usingPorts = (List<String>) result.data;
            if (usingPorts.size() > 0) {
                String hint = "端口被占用: ";
                for (String port: usingPorts)
                    hint += port + ", ";
                Result error = genError("checkLocalPortsUsing", BaseError.ACTION_ILLEGAL, hint, hint);
                callback.done(error);
                return;
            }
            // ports is ready

            checkCameraUsing(cameras, context, handler, result1 -> {
                if (!result1.isSuccessful()) {
                    Result error = genError("checkResource", result1.code,
                            "checkLocalPortsUsing failed: " + result1.getMessage(), "检查HDMI-IN失败: " + result1.getMessage());
                    callback.done(error);
                    return;
                }

                List<String> usingCameras = (List<String>) result1.data;
                if (usingCameras.size() > 0) {
                    CameraHelper helper = CameraHelper.getInstance();
                    String hint = "硬件资源被占用: ";
                    for (String id: usingCameras) {
                        if (helper.isNormative(id)) {
                            int hdmi = CameraHelper.getInstance().cameraId2HDMIPort(id) + 1;
                            hint += "HDMI-IN#" + hdmi + ", ";
                        } else {
                            hint += "Camera#" + id + ", ";
                        }

                    }

                    Result error = genError("checkCameraUsing", BaseError.ACTION_ILLEGAL, hint, hint);
                    callback.done(error);
                    return;
                }

                callback.done(Result.SUCCESS);
            });
        });
    }

    public static void checkLocalPortsUsing(final List<Port> localPorts, Handler handler,
                                            final Callback/*List<String>*/ callback) {
        handler.post(() -> {
            if (localPorts == null || localPorts.size() == 0) {
                if (callback != null)
                    callback.done(Result.buildSuccess(new ArrayList<>()));
                return;
            }

            String ports = "";
            String pattern = "";
            boolean added = false;
            for (Port port: localPorts) {
                if (added) {
                    ports += "|";
                    pattern += "|";
                }

                ports += port.number;
                pattern += port.protocol + " " + port.number;
                added = true;
            }

            String cmd = String.format("netstat -natu | grep -E '(%s).*LISTEN' | busybox awk '{print $1,$4}'| busybox awk -F'[ :]' '{print $1,$NF}' |grep -E '(%s)'",
                    ports, pattern);

            Result out;
            LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd);
            if (result.code == 1 || result.code == 0) {
                List<String> using = new LinkedList<>();
                for (String line: result.stdOut)
                    using.add(line);
                out = Result.buildSuccess(using);
            } else {
                String message = "checkLocalPortsUsing with linux cmd failed: " + result.AllToString();
                LogUtil.w(TAG, message);
                out = new Result(BaseError.INTERNAL_ERROR, message);
            }

            if (callback != null)
                callback.done(out);
        });
    }

    public static void checkCameraUsing(List<String> cameras, Context context, Handler handler,
                                        Callback/*List<String>*/ callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkCameraUsingV23(cameras, context, handler, callback);
        else
            checkCameraUsingV21(cameras, handler, callback);
    }

    private static void checkCameraUsingV21(final List<String> cameras, Handler handler,
                                            final Callback callback) {
        handler.post(() -> {
            List<String> result = new LinkedList<>();
            if (cameras != null) {
                for (String id : cameras) {
                    boolean using = isCameraUsingV21(id);
                    if (using)
                        result.add(id);
                }
            }
            if (callback != null)
                callback.done(Result.buildSuccess(result));
        });
    }

    private static void checkCameraUsingV23(final List<String> cameras, Context context,
                                            Handler handler, final Callback callback) {

        if (cameras == null || cameras.size() == 0) {
            handler.post(() -> {
                if (callback != null)
                    callback.done(Result.buildSuccess(new ArrayList<>()));
            });
            return;
        }

        final Map<String, Boolean> using = new HashMap<>(cameras.size());
        final boolean ret[] = {false};

        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        final CameraManager.AvailabilityCallback availabilityCallback = new CameraManager.AvailabilityCallback() {
            @Override
            public void onCameraAvailable(String cameraId) {
                if (ret[0]) {
                    manager.unregisterAvailabilityCallback(this);
                    return;
                }

                LogUtil.i(TAG, "camera#" + cameraId + " is available");
                try {
                    if (cameras.contains(cameraId)) {
                        using.put(cameraId, false);
                        if (using.size() == cameras.size()) {
                            ret[0] = true;
                            manager.unregisterAvailabilityCallback(this);
                            callbackCameraUsing(using, callback);
                        }
                    }
                } catch (Exception e) {
                    LogUtil.w(TAG, "AvailabilityCallback unknown camera id: " + cameraId);
                }
            }

            @Override
            public void onCameraUnavailable(String cameraId) {
                if (ret[0]) {
                    manager.unregisterAvailabilityCallback(this);
                    return;
                }

                LogUtil.i(TAG, "camera#" + cameraId + " is unavailable");
                try {
                    if (cameras.contains(cameraId)) {
                        using.put(cameraId, true);
                        if (using.size() == cameras.size()) {
                            ret[0] = true;
                            manager.unregisterAvailabilityCallback(this);
                            callbackCameraUsing(using, callback);
                        }
                    }
                } catch (Exception e) {
                    LogUtil.w(TAG, "AvailabilityCallback unknown camera id: " + cameraId);
                }
            }
        };
        manager.registerAvailabilityCallback(availabilityCallback, handler);

        handler.postDelayed(() -> {
            if (ret[0])
                return;
            ret[0] = true;
            manager.unregisterAvailabilityCallback(availabilityCallback);
            if (callback != null)
                callback.done(new Result(BaseError.ACTION_TIMEOUT, "CameraManager.AvailabilityCallback timeout"));
        }, 2000);
    }

    private static void callbackCameraUsing(final Map<String, Boolean> using, Callback callback) {
        List<String> ret = new LinkedList<>();
        for (Map.Entry<String, Boolean> status: using.entrySet()) {
            if (status.getValue())
                ret.add(status.getKey());
        }
        if (callback != null)
            callback.done(Result.buildSuccess(ret));
    }

    private static boolean isCameraUsingV21(String id) {
        int cameraId;
        try {
            cameraId = Integer.valueOf(id);
        } catch (Exception e) {
            LogUtil.w(TAG, "isCameraUsingV21, camera id is not Integer: " + id, e);
            return true;
        }

        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
            return false;
        } catch (Exception e) {
            return true;
        } finally {
            if (camera != null) camera.release();
        }
    }

    private static Result genError(String action, int error, String message, String hint) {
        LogUtil.w(TAG, action + ":" + message);
        return new Result(error, hint);
    }
}
