package sanp.avalon.libs.network.ip;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.LruCache;

import sanp.avalon.libs.base.utils.CommonUtil;

/**
 * Created by huang on 2017/6/20.
 */

public class AccessPoint implements Comparable<AccessPoint> {

    /**
     * These values are matched in string arrays -- changes must be kept in sync
     */
    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;

    public static final int SIGNAL_LEVELS = 4;
    public static final int INVALID_NETWORK_ID = -1;

    /**
     * psk Type
     */
    private static final int PSK_UNKNOWN = 0;
    private static final int PSK_WPA = 1;
    private static final int PSK_WPA2 = 2;
    private static final int PSK_WPA_WPA2 = 3;


    /**
     * connect
     */
    public static final int NOCONNECT = 0;//无连接
    public static final int CONNECTED = 1;//已连接
    public static final int CONNECTING = 2;//连接中


    public LruCache<String, ScanResult> mScanResultCache = new LruCache<String, ScanResult>(32);

    private String ssid;
    private String bssid;
    private int security;
    private int pskType;
    private int mRssi;
    private long mSeen;
    private int networkId = -1;
    private int connect;
    //链接详情
    private NetworkInfo.DetailedState connectState;

    private WifiConfiguration mConfig;

    private Context mContext;

    public AccessPoint(Context context, ScanResult result) {
        mContext = context;
        initWithScanResult(result);
    }

    public AccessPoint(Context context, WifiConfiguration config) {
        mContext = context;
        loadConfig(config);
    }

    public boolean update(ScanResult result) {
        if (matches(result)) {

            /* Add or update the scan result for the BSSID */
            mScanResultCache.put(result.BSSID, result);
            mRssi = getRssi();
            // This flag only comes from scans, is not easily saved in config
            if (security == SECURITY_PSK) {
                pskType = getPskType(result);
            }

            return true;
        }
        return false;
    }

    public void update(WifiConfiguration config) {
        mConfig = config;
        networkId = config.networkId;
    }


    private void initWithScanResult(ScanResult result) {
        ssid = result.SSID;
        bssid = result.BSSID;
        security = getSecurity(result);
        if (security == SECURITY_PSK)
            pskType = getPskType(result);
        mRssi = result.level;
        mSeen = result.timestamp;
    }

    public void loadConfig(WifiConfiguration config) {
        ssid = removeDoubleQuotes(config.SSID);
        bssid = config.BSSID;
        security = getSecurity(config);
        networkId = config.networkId;
        mConfig = config;
    }


    private static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    private static int getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PSK_WPA_WPA2;
        } else if (wpa2) {
            return PSK_WPA2;
        } else if (wpa) {
            return PSK_WPA;
        } else {
            return PSK_UNKNOWN;
        }
    }

    public boolean matches(ScanResult result) {
        return ssid.equals(result.SSID) && security == getSecurity(result);
    }

    public boolean matches(WifiConfiguration config) {
        return ssid.equals(removeDoubleQuotes(config.SSID))
                && security == getSecurity(config);
    }

    static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public WifiConfiguration getConfig() {
        return mConfig;
    }

    public void clearConfig() {
        mConfig = null;
        networkId = INVALID_NETWORK_ID;
    }


    public int getLevel() {
        return WifiManager.calculateSignalLevel(mRssi, SIGNAL_LEVELS);
    }

    public int getRssi() {
        int rssi = Integer.MIN_VALUE;
        for (ScanResult result : mScanResultCache.snapshot().values()) {
            if (result.level > rssi) {
                rssi = result.level;
            }
        }

        return rssi;
    }


    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    public int getSecurity() {
        return security;
    }

    public int getConnect() {
        return connect;
    }

    public void setConnect(int connect) {
        this.connect = connect;
    }

    public NetworkInfo.DetailedState getConnectState() {
        return connectState;
    }

    public void setConnectState(NetworkInfo.DetailedState connectState) {
        this.connectState = connectState;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    @Override
    public int compareTo(@NonNull AccessPoint other) {

        // Reachable one goes before unreachable one.
        if (mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) return -1;
        if (mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) return 1;
        int difference = other.getConnect() - this.getConnect();
        if (difference != 0) {
            return difference;
        }
        // Sort by signal strength, bucketed by level
        difference = other.getLevel() - this.getLevel();
        if (difference != 0) {
            return difference;
        }
        // Sort by ssid.
        return ssid.compareToIgnoreCase(other.ssid);

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessPoint that = (AccessPoint) o;

        if (security != that.security) return false;
        if (pskType != that.pskType) return false;
        if (mRssi != that.mRssi) return false;
        if (mSeen != that.mSeen) return false;
        if (networkId != that.networkId) return false;
        if (mScanResultCache != null ? !mScanResultCache.equals(that.mScanResultCache) : that.mScanResultCache != null)
            return false;
        if (ssid != null ? !ssid.equals(that.ssid) : that.ssid != null) return false;
        if (bssid != null ? !bssid.equals(that.bssid) : that.bssid != null) return false;
        if (mConfig != null ? !mConfig.equals(that.mConfig) : that.mConfig != null) return false;
        return mContext != null ? mContext.equals(that.mContext) : that.mContext == null;

    }

    @Override
    public int hashCode() {
        int result = mScanResultCache != null ? mScanResultCache.hashCode() : 0;
        result = 31 * result + (ssid != null ? ssid.hashCode() : 0);
        result = 31 * result + (bssid != null ? bssid.hashCode() : 0);
        result = 31 * result + security;
        result = 31 * result + pskType;
        result = 31 * result + mRssi;
        result = 31 * result + (int) (mSeen ^ (mSeen >>> 32));
        result = 31 * result + networkId;
        result = 31 * result + (mConfig != null ? mConfig.hashCode() : 0);
        result = 31 * result + (mContext != null ? mContext.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AccessPoint{" +
                "mScanResultCache=" + mScanResultCache +
                ", ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", security=" + security +
                ", pskType=" + pskType +
                ", mRssi=" + mRssi +
                ", mSeen=" + mSeen +
                ", networkId=" + networkId +
                ", mConfig=" + mConfig +
                '}';
    }

    public static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    public boolean isSaved() {
        return networkId != -1;
    }

    public void submitConfig(){
        if (mConfig == null) mConfig = init(null);
    }

    public void submitConfig(String password) {
        if (mConfig == null) mConfig = init(password);
    }
    public void submitConfig(android.net.IpConfiguration ipConfiguration) {
        if (mConfig != null) CommonUtil.method(mConfig,"setIpConfiguration",new Object[]{ipConfiguration},new Class[]{android.net.IpConfiguration.class},null);
    }

    public IpConfiguration getIpConfiguration() {
        if (mConfig == null) return null;
        android.net.IpConfiguration ipConfiguration = CommonUtil.getPrivateByName(mConfig, "mIpConfiguration", android.net.IpConfiguration.class);
        return new IpConfiguration(ipConfiguration);
    }

    public WifiConfiguration init(String password) {
        WifiConfiguration config = new WifiConfiguration();
        if (!this.isSaved()) {
            config.SSID = AccessPoint.convertToQuotedString(this.ssid);
        } else {
            config.networkId = this.getConfig().networkId;
        }
        switch (this.getSecurity()) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                if (password.length() != 0) {
                    int length = password.length();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;
            case SECURITY_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                if (password.length() != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;
            case SECURITY_EAP:
                return null;
            default:
                return null;
        }

        return config;
    }

    public class IpConfiguration extends android.net.IpConfiguration {

        public IpConfiguration(android.net.IpConfiguration source) {
            super(source);
        }

        public StaticIpConfiguration goGetStaticIpConfiguration() {
            android.net.StaticIpConfiguration staticIpConfiguration = getStaticIpConfiguration();
            if (staticIpConfiguration == null) return null;
            return new StaticIpConfiguration(staticIpConfiguration);
        }
        public StaticIpConfiguration initStaticIpConfiguration(){
            return new StaticIpConfiguration();
        }
    }

    public class StaticIpConfiguration extends android.net.StaticIpConfiguration {

        public StaticIpConfiguration(){
            super();
        }

        public StaticIpConfiguration(android.net.StaticIpConfiguration source) {
            super(source);
        }

        public String getIpAddress() {
            if (ipAddress == null || ipAddress.getAddress() == null) return "";
            return ipAddress.getAddress().getHostAddress();
        }

        public String getGateWay() {
            if (gateway == null) return "";
            return gateway.getHostAddress();
        }

        public String getNetMask() {
            if (ipAddress == null || ipAddress.getAddress() == null) return "";
            return parseIntToIp(0xFFFFFFFF << (32 - ipAddress.getPrefixLength()));
        }

        public String[] getDnsServers() {
            String[] strs = new String[]{"",""};
            int len = strs.length<dnsServers.size()?strs.length:dnsServers.size();
            for (int i=0;i<len;i++){
                if (dnsServers.get(i)!=null){
                    strs[i] = dnsServers.get(i).getHostAddress();
                }
            }
            return strs;
        }

        private String parseIntToIp(int param) {
            String split = ".";
            return new StringBuilder()
                    .append(param >> 24 & 0xFF)
                    .append(split)
                    .append(param >> 16 & 0xFF)
                    .append(split)
                    .append(param >> 8 & 0xFF)
                    .append(split)
                    .append(param & 0xFF).toString();
        }


    }

}
