package com.sanbu.board;

import com.sanbu.tools.LogUtil;

public class EmptyBoardSupportClient implements BoardSupportClient {

    private static final String TAG = EmptyBoardSupportClient.class.getSimpleName();

    @Override
    public int cameraIdToHdmiDeviceId(int cameraId) {
        if (0 == cameraId) {
            return HDMI_IN_PORT_1;
        } else if (1 == cameraId) {
            return HDMI_IN_PORT_2;
        } else {
            LogUtil.e(TAG, "Invalid camera id. " + cameraId);
            return -1;
        }
    }

    @Override
    public int hdmiDeviceIdToCameraId(int port) {
        if (HDMI_IN_PORT_1 == port) {
            return 0;
        } else if (HDMI_IN_PORT_2 == port) {
            return 1;
        } else {
            LogUtil.e(TAG, "Invalid port id. " + port);
            return -1;
        }
    }

    @Override
    public void startQueryingHDMIIn(int port) {

    }

    @Override
    public boolean isQueryingHDMIIn(int port) {
        return false;
    }

    @Override
    public boolean isHDMIInPlugged(int port) {
        return true;
    }

    @Override
    public HDMIFormat getHDMIInFormat(int port) {
        return new HDMIFormat(1920, 1080, 60);
    }


}
