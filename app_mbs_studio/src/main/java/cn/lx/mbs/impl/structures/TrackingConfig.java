package cn.lx.mbs.impl.structures;

import com.sanbu.tools.StringUtil;

public class TrackingConfig {
    public DevModel model;       // 型号
    public String address;       // 地址
    public Integer port;         // 端口

    public TrackingConfig(DevModel model, String address, int port) {
        this.model = model;
        this.address = address;
        this.port = port;
    }

    public TrackingConfig(TrackingConfig other) {
        this(other.model, other.address, other.port);
    }

    public boolean isValid() {
        return (model != null &&
                !StringUtil.isEmpty(address) &&
                port != null && port > 0
        );
    }
}
