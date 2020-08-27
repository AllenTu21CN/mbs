package cn.sanbu.avalon.endpoint3.ext.camera;

import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.State;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import cn.sanbu.avalon.endpoint3.EPConst;
import cn.sanbu.avalon.endpoint3.ext.ExtEvent;
import cn.sanbu.avalon.endpoint3.ext.ExtJni;

public class IxNCamera {

    private static final String TAG = IxNCamera.class.getSimpleName();

    public static final int MOVING_SPEED_MAX     = 0x14;
    public static final int MOVING_SPEED_MIN     = 0x00;
    public static final int MOVING_SPEED_DEFAULT = 0x0a;

    private static final int TRACK_MODE_MANUALLY = 81;
    private static final int TRACK_MODE_AUTO     = 80;
    private static final int MOVING_STEP         = 20;

    private static final int PRESET_NUMBER_OFFSET = 10;

    public interface EventListener {
        void onIxNEvent(ExtEvent event);
    }

    // handler
    private int mJniObject = -1;
    private EventListener mListener;

    // status
    private int mCurrentCamId = -1;
    private int mSpeed = MOVING_SPEED_DEFAULT;

    // connection
    private State mState = State.None;
    private Callback mConnectingCallback;

    public IxNCamera() {

    }

    // @param masterId [IN], if of master camera which is const to 1 normally
    public int init(int masterId, EventListener listener) {
        if (!ExtJni.isInited())
            throw new RuntimeException("call ExtJni.initEnv first");
        if (mJniObject >= 0)
            return 0;

        mJniObject = jniCreateCameraCtrl(masterId);
        if (mJniObject < 0)
            return mJniObject;

        // set camera event pattern
        int ret = jniSetEventCharacter(mJniObject, ExtEvent.FIXED_PREFIX[0], ExtEvent.FIXED_FULL_LENGTH, ExtEvent.FIXED_SUFFIX[0]);
        if (ret != 0) {
            release();
            return ret;
        }

        // set camera moving step
        jniSetMovementStep(mJniObject, MOVING_STEP);

        // init handler and status
        mListener = listener;
        mCurrentCamId = -1;
        mSpeed = MOVING_SPEED_DEFAULT;
        mState = State.None;
        mConnectingCallback = null;
        return 0;
    }

    public void release() {
        disconnect();

        if (mJniObject >= 0) {
            jniReleaseCamera(mJniObject);
            mJniObject = -1;
        }

        mListener = null;
        mCurrentCamId = -1;
        mSpeed = MOVING_SPEED_DEFAULT;
        mState = State.None;
        mConnectingCallback = null;
    }

    ///////////////// connecting

    public int connect(String camIp, int camPort, Callback callback) {
        if (mJniObject < 0) {
            LogUtil.e(EPConst.TAG, TAG, "init first");
            return BaseError.ACTION_ILLEGAL;
        }
        if (mState != State.None) {
            LogUtil.w(EPConst.TAG, TAG, "had been connecting");
            return BaseError.ACTION_ILLEGAL;
        }

        mState = State.Doing;
        mConnectingCallback = callback;
        return jniAsyncConnectCameraHost(mJniObject, camIp, camPort);
    }

    public void disconnect() {
        if (mState == State.None)
            return;

        // cancel the previous connecting
        if (mConnectingCallback != null) {
            Callback callback = mConnectingCallback;
            mConnectingCallback = null;
            if (callback != null)
                callback.done(new Result(BaseError.ACTION_CANCELED, "canceled by disconnect"));
        }

        jniDisconnectCamera(mJniObject);
        mState = State.None;
    }

    public State getConnectionState() {
        return mState;
    }

    ///////////////// control for PTZ

    public int setPTZTarget(int targetId) {
        if (mJniObject < 0) {
            LogUtil.e(EPConst.TAG, TAG, "init first");
            return BaseError.ACTION_ILLEGAL;
        }

        mCurrentCamId = targetId;
        return 0;
    }

    public int getPTZSpeed() {
        return mSpeed;
    }

    public int setPTZSpeed(int speed) {
        if (!checkPTZControl("setPTZSpeed"))
            return BaseError.ACTION_ILLEGAL;
        if (speed < MOVING_SPEED_MIN || speed > MOVING_SPEED_MAX) {
            LogUtil.w(EPConst.TAG, TAG, "setPTZSpeed: invalid speed: " + speed);
            return BaseError.INVALID_PARAM;
        }

        int ret = jniSetPTZSpeed(mJniObject, mCurrentCamId, speed, speed);
        if (ret == 0)
            mSpeed = speed;
        return 0;
    }

    public int movePTZ(CameraDirective dir) {
        if (!checkPTZControl("movePTZ"))
            return BaseError.ACTION_ILLEGAL;

        return jniSetPTZPos(mJniObject, mCurrentCamId, dir.ordinal());
    }

    public int setPTZZoom(ZoomAction action) {
        if (!checkPTZControl("setPTZZoom"))
            return BaseError.ACTION_ILLEGAL;

        if (action == ZoomAction.toNear)
            return jniSetZoomNear(mJniObject, mCurrentCamId);
        else if (action == ZoomAction.toFar)
            return jniSetZoomFar(mJniObject, mCurrentCamId);
        else
            return jniSetZoomStop(mJniObject, mCurrentCamId);
    }

    public int setPTZPreset(int number) {
        if (!checkPTZControl("setPreset"))
            return BaseError.ACTION_ILLEGAL;

        return jniSetPresetValue(mJniObject, mCurrentCamId, number + PRESET_NUMBER_OFFSET);
    }

    public int loadPTZPreset(int number) {
        if (!checkPTZControl("loadPTZPreset"))
            return BaseError.ACTION_ILLEGAL;

        return jniApplyPresetValue(mJniObject, mCurrentCamId, number + PRESET_NUMBER_OFFSET);
    }

    public int setTrackMode(int targetId, boolean auto) {
        if (!checkPTZControl("setTrackMode", targetId))
            return BaseError.ACTION_ILLEGAL;

        int number = auto ? TRACK_MODE_AUTO : TRACK_MODE_MANUALLY;
        return jniApplyPresetValue(mJniObject, targetId, number);
    }

    public int switchDefaultScene(IxNScene scene) {
        if (mState != State.Done) {
            LogUtil.w(EPConst.TAG, TAG, "switchDefaultScene: has not connected with camera");
            return BaseError.ACTION_ILLEGAL;
        }

        return jniSetSwitchScene(mJniObject, scene.id);
    }

    public int switchChannelMode(IxNChannelMode mode) {
        if (mState != State.Done) {
            LogUtil.w(EPConst.TAG, TAG, "switchChannelMode: has not connected with camera");
            return BaseError.ACTION_ILLEGAL;
        }

        return jniSetSwitchChannel(mJniObject, mode.id);
    }

    ///////////////// callback for JNI

    public void onCameraConnected(int id, String ip, int port) {
        LogUtil.i(EPConst.TAG, TAG, String.format("onCameraConnected: id[%d] ip[%s] port[%d]", id, ip, port));
        if (id != mJniObject) {
            LogUtil.d(EPConst.TAG, TAG, "ignore expired onCameraConnected");
            return;
        }
        if (mState != State.Doing) {
            LogUtil.w(EPConst.TAG, TAG, "onCameraConnected, logical error: " + mState.name());
            return;
        }

        Callback callback = mConnectingCallback;
        mConnectingCallback = null;
        mState = State.Done;
        if (callback != null)
            callback.done(Result.SUCCESS);
    }

    public void onCameraEvent(int id, byte[] data) {
        if (id != mJniObject) {
            LogUtil.v(EPConst.TAG, TAG, "ignore expired onCameraEvent");
            return;
        }

        ExtEvent event = ExtEvent.fromBin(1, data, 0, data.length);
        if (event == null) {
            LogUtil.i(EPConst.TAG, TAG, "onCameraEvent, unknown data: [" + StringUtil.bytesToHexString(data, 0, 10, " ") + "]");
            return;
        } else if (LogUtil.isEnabledVerbose()) {
            LogUtil.v(EPConst.TAG, TAG, "onCameraEvent: [" + StringUtil.bytesToHexString(data, 0, 10, " ") + "]");
        }

        LogUtil.v(EPConst.TAG, TAG, "onCameraEvent: " + event.name());
        if (mListener != null)
            mListener.onIxNEvent(event);
    }

    public void onCameraError(int id, int error) {
        LogUtil.i(EPConst.TAG, TAG, String.format("onCameraError: id[%d] error[%d]", id, error));
        if (id != mJniObject) {
            LogUtil.d(EPConst.TAG, TAG, "ignore expired onCameraError");
            return;
        }
        if (mState == State.None) {
            LogUtil.w(EPConst.TAG, TAG, "onCameraError, logical error: " + mState.name());
            return;
        }

        Callback callback = mConnectingCallback;
        mConnectingCallback = null;
        mState = State.Doing;
        if (callback != null)
            callback.done(new Result(BaseError.INTERNAL_ERROR, "connect failed: " + error));
    }

    ///////////////// private functions

    private boolean checkPTZControl(String action) {
        return checkPTZControl(action, mCurrentCamId);
    }

    private boolean checkPTZControl(String action, int targetId) {
        if (mState != State.Done) {
            LogUtil.w(EPConst.TAG, TAG, action + ": has not connected with PTZ");
            return false;
        }
        if (targetId <= 0) {
            LogUtil.w(EPConst.TAG, TAG, action + ": setPTZTarget first");
            return false;
        }
        return true;
    }

    ///////////////// JNI functions

    private native int jniCreateCameraCtrl(int camera_id);

    private native int jniSetEventCharacter(int object_id, byte start_code, int length, byte end_code);

    private native int jniSetMovementStep(int object_id, int step);

    private native int jniAsyncConnectCameraHost(int object_id, String ip, int port);

    private native int jniDisconnectCamera(int object_id);

    private native void jniReleaseCamera(int object_id);

    private native int jniSetPTZPos(int object_id, int camera_id, int dir);

    private native int jniSetPTZSpeed(int object_id, int camera_id, int horiz_speed, int vertical_speed);

    private native boolean jniGetPTZSpeed(int object_id, int camera_id, int[] speeds);

    private native int jniSetPresetValue(int object_id, int camera_id, int channel);

    private native int jniApplyPresetValue(int object_id, int camera_id, int channel);

    private native int jniSetZoomNear(int object_id, int camera_id);

    private native int jniSetZoomFar(int object_id, int camera_id);

    private native int jniSetZoomStop(int object_id, int camera_id);

    private native int jniSetZoomValue(int object_id, int camera_id, int zoom);

    private native int jniGetZoomValue(int object_id, int camera_id);

    private native int jniSetSwitchScene(int object_id, int target_id);

    private native int jniSetSwitchChannel(int object_id, int channel_id);
}
