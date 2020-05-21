package com.sanbu.board;

public interface BoardSupportClient {
    int HDMI_IN_PORT_1 = 0x00;
    int HDMI_IN_PORT_2 = 0x01;

    int cameraIdToHdmiDeviceId(int cameraId);
    boolean isSupportHdmiChecking();
    boolean invokeVideoInputPortIsPlugged(int port);
    HDMIFormat invokeVideoInputPortQueryFormat(int port);
}
