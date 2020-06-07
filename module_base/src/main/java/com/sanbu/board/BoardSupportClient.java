package com.sanbu.board;

public interface BoardSupportClient {
    int HDMI_IN_PORT_1 = 0x00;
    int HDMI_IN_PORT_2 = 0x01;

    int cameraIdToHdmiDeviceId(int cameraId);
    int hdmiDeviceIdToCameraId(int port);

    void startQueryingHDMIIn(int port);
    boolean isQueryingHDMIIn(int port);

    boolean isHDMIInPlugged(int port);
    HDMIFormat getHDMIInFormat(int port);
}
