package com.sanbu.base;

public enum NetType {
    None("未连接"),
    Mobile("移动网络"),
    WiFi("无线网络"),
    Ethernet("有线网络");

    public final String name;

    NetType(String name) {
        this.name = name;
    }
}
