package sanp.avalon.libs.base.bean.eventbean;

import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by zdh on 2017/3/30.
 * WIFI控制工具类
 */

public class NetInfo {
    // 定义WifiManager对象
    private WifiManager mWifiManager;
    // 定义WifiInfo对象
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表
    private List<ScanResult> mWifiList;
    // 网络连接列表
    private final DhcpInfo dhcpInfo;
    private ConnectivityManager connectionManager;
    private NetworkInfo networkInfo;
    private boolean available;

    // 构造器
    public NetInfo(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);

        //获取网络连接管理者
        connectionManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        networkInfo = connectionManager.getActiveNetworkInfo();
        // 取得WifiInfo对象
        dhcpInfo = mWifiManager.getDhcpInfo();
        mWifiInfo = mWifiManager.getConnectionInfo();
    }

    //获取当前连接wifi的方式，是wifi还是有线
    public int getMode() {
        int type = networkInfo.getType();
        return type;
    }

    //网络管理者判断是否连接
    public boolean getIsCon() {
        if (connectionManager.getActiveNetworkInfo() != null) {
            available = connectionManager.getActiveNetworkInfo().isAvailable();
        }
        if (!available) {

        } else {
            isNetworkAvailable();
        }
        return available;
    }

    // 打开WIFI
    public void openWifi(Context context) {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        } else if (mWifiManager.getWifiState() == 2) {
        } else {
        }
    }

    // 关闭WIFI
    public void closeWifi(Context context) {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        } else if (mWifiManager.getWifiState() == 1) {

        } else if (mWifiManager.getWifiState() == 0) {

        } else {
            Toast.makeText(context, "请重新关闭", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 返回网络状态
     *
     * @return 0:正在关闭 1:已关闭 2:正在开启 3:已开启
     */
    public int checkState() {
        int wifiState = mWifiManager.getWifiState();

        return wifiState;
    }

    //扫描wifi
    public void startScan(Context context) {
        mWifiManager.startScan();
        // 得到扫描结果
        mWifiList = mWifiManager.getScanResults();
        // 得到配置好的网络连接
        if (mWifiList == null) {
            if (mWifiManager.getWifiState() == 3) {
                Toast.makeText(context, "当前区域没有无线网络", Toast.LENGTH_SHORT).show();
            } else if (mWifiManager.getWifiState() == 2) {
                Toast.makeText(context, "WiFi正在开启，请稍后重新点击扫描", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "WiFi没有开启，无法扫描", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 得到网络列表
    public List<ScanResult> getWifiList() {

        return mWifiList;
    }

    //地址，网络掩码，网关，DNS1，
    public String getAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(intToIp(dhcpInfo.ipAddress));

        String s = sb.toString();
        return s;
    }

    public String getNetMask() {
        StringBuilder sb = new StringBuilder();
        sb.append(intToIp(dhcpInfo.netmask));
        String s = sb.toString();
        return s;
    }

    public String getGateway() {
        StringBuilder sb = new StringBuilder();
        sb.append(intToIp(dhcpInfo.gateway));
        String s = sb.toString();
        return s;
    }

    public String getDns1() {
        StringBuilder sb = new StringBuilder();
        sb.append(intToIp(dhcpInfo.dns1));
        String s = sb.toString();
        return s;
    }


    private String intToIp(int paramInt) {

        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."

                + (0xFF & paramInt >> 24);
    }

    public String getSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getSSID();
    }

    // 得到连接的ID
    public int getNetworkId() {

        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }


    //是否连接WIFI
    public boolean isWifiConnected(Context context) {
        NetworkInfo wifiNetworkInfo = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {

            return true;
        }

        return false;
    }

    // 添加一个网络并连接
    public int addNetwork(WifiConfiguration wcg) {
        int wcgID = mWifiManager.addNetwork(wcg);
        mWifiManager.enableNetwork(wcgID, true);
        boolean b = mWifiManager.enableNetwork(wcgID, true);
        return wcgID;
    }

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

    //断开网路的同时，清空配置信息
    public void removeWifi(int netId) {
        mWifiManager.removeNetwork(netId);
        disconnectWifi(netId);

    }
    public Boolean saveConfiguration(){
        return mWifiManager.saveConfiguration();
    }

    public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == 1) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    //判断设备当前的网络连接状态
    public String isNetworkAvailable() {

        NetworkInfo.State state = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).getState();
        String s = state.toString();
        return s;
    }
}