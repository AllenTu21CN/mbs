/**
 * Created by Tuyj on 2017/5/31.
 * Wrapper for android.net.IpCfg
 */
package com.sanbu.android;

public class IpCfg {

    public enum IpMode {
        /* Use statically configured IP settings. Configuration can be accessed
         * with staticIpConfiguration */
        STATIC,
        /* Use dynamically configured IP settigns */
        DHCP,
    }

    public IpMode ipMode = IpMode.DHCP;
    public StaticIpConfiguration staticIpConfiguration = null;

    public IpCfg() {
    }

    public IpCfg(IpMode ipMode,
                 StaticIpConfiguration staticIpConfiguration) {
        this.ipMode = ipMode;
        if (staticIpConfiguration != null) {
            this.staticIpConfiguration = new StaticIpConfiguration(staticIpConfiguration);
        }

    }
}