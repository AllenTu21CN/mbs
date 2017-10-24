package sanp.avalon.libs.network.ip;

import java.util.ArrayList;

/**
 * Created by Tuyj on 2017/6/1.
 */

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
