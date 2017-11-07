/**
 * Created by Tuyj on 2017/5/31.
 * Wrapper for android.net.IpConfiguration
 */
package sanp.tools.utils.net;

import java.util.ArrayList;

public class IpConfiguration {

    public enum IpMode {
        /* Use statically configured IP settings. Configuration can be accessed
         * with staticIpConfiguration */
        STATIC,
        /* Use dynamically configured IP settigns */
        DHCP,
        /* no IP details are assigned, this is used to indicate
         * that any existing IP settings should be retained */
        UNASSIGNED
    }

    public class StaticIpConfiguration {
        public String ipAddress;
        public String mask;
        public String gateway;
        public ArrayList<String> dnsServers = new ArrayList<>();

        public StaticIpConfiguration() { }
        public StaticIpConfiguration(StaticIpConfiguration other) {
            this.ipAddress = other.ipAddress;
            this.mask = other.mask;
            this.gateway = other.gateway;
            this.dnsServers = (ArrayList<String>)other.dnsServers.clone();
        }
    }

    public IpMode ipMode = IpMode.UNASSIGNED;
    public StaticIpConfiguration staticIpConfiguration = null;

    public IpConfiguration() {
    }

    public IpConfiguration(IpMode ipMode,
                           StaticIpConfiguration staticIpConfiguration) {
        this.ipMode = ipMode;
        this.staticIpConfiguration = new StaticIpConfiguration(staticIpConfiguration);
    }
}