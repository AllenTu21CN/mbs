package com.sanbu.base;

public class NetInfo {
    public final NetType type;
    public final String ip;
    public final String mask;
    public final String gateway;
    public final String dns1;
    public final String dns2;

    public static NetInfo buildEmpty() {
        return new NetInfo(NetType.None, "0.0.0.0", "255.255.255.0", "0.0.0.0", "8.8.8.8", "");
    }

    public NetInfo(NetType type, String ip, String mask, String gateway, String dns1, String dns2) {
        this.type = type;
        this.ip = ip;
        this.mask = mask;
        this.gateway = gateway;
        this.dns1 = dns1;
        this.dns2 = dns2;
    }
}
