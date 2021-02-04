package cn.sanbu.avalon.endpoint3.structures;

import com.sanbu.network.TransProtocol;

import cn.sanbu.avalon.endpoint3.structures.jni.Reconnecting;

public class RTSPSourceConfig {

    // configure of reconnecting
    public Reconnecting reconnecting;

    // protocol of media transport
    public TransProtocol transProtocol;

    // onOff of attach features
    public boolean attachSpsPps;
    public boolean attachAdts;

    public RTSPSourceConfig(Reconnecting reconnecting) {
        this.reconnecting = reconnecting;
        this.attachSpsPps = false;
        this.attachAdts = false;
    }

    public RTSPSourceConfig(Reconnecting reconnecting, TransProtocol transProtocol) {
        this.reconnecting = reconnecting;
        this.transProtocol = transProtocol;
        this.attachSpsPps = false;
        this.attachAdts = false;
    }

    public RTSPSourceConfig(Reconnecting reconnecting, TransProtocol transProtocol,
                            boolean attachSpsPps, boolean attachAdts) {
        this.reconnecting = reconnecting;
        this.transProtocol = transProtocol;
        this.attachSpsPps = attachSpsPps;
        this.attachAdts = attachAdts;
    }

    public RTSPSourceConfig setTransProtocol(TransProtocol transProtocol) {
        this.transProtocol = transProtocol;
        return this;
    }

    public RTSPSourceConfig setAttachSpsPps(boolean attachSpsPps) {
        this.attachSpsPps = attachSpsPps;
        return this;
    }

    public RTSPSourceConfig setAttachAdts(boolean attachAdts) {
        this.attachAdts = attachAdts;
        return this;
    }
}
