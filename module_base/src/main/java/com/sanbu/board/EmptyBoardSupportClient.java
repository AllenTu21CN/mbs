package com.sanbu.board;

import com.sanbu.tools.LogUtil;

public class EmptyBoardSupportClient implements BoardSupportClient {

    private static final String TAG = EmptyBoardSupportClient.class.getSimpleName();

    @Override
    public int cameraIdToHdmiDeviceId(int cameraId) {
        if (HDMI_IN_PORT_1 == cameraId) {
            return 0;
        } else if (HDMI_IN_PORT_2 == cameraId) {
            return 1;
        } else {
            LogUtil.e(TAG, "Invalid camera id. " + cameraId);
            return -1;
        }
    }

    @Override
    public boolean isSupportHdmiChecking() {
        return false;
    }

    @Override
    public boolean invokeVideoInputPortIsPlugged(int port) {
        return true;
    }

    @Override
    public HDMIFormat invokeVideoInputPortQueryFormat(int port) {
        return new HDMIFormat(1920, 1080, 60);
    }
}
