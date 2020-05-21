package com.sanbu.android;

/**
 * Created by Tuyj on 2017/6/1.
 */

public class StaticIpConfiguration {
    public String ip;
    public String mask;
    public String gateway;
    public String dns;
    public String dns2;

    public StaticIpConfiguration() { }

    public StaticIpConfiguration(StaticIpConfiguration other) {
        this.ip = other.ip;
        this.mask = other.mask;
        this.gateway = other.gateway;
        this.dns = other.dns;
        this.dns2 = other.dns2;
    }
}
