package sanp.mp100.test.ui.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import java.util.List;

import sanp.avalon.libs.base.utils.CmdUtils;
import sanp.avalon.libs.base.utils.LogManager;

/**
 * Created by zhangxd on 2017/7/26.
 */

public class WifiManagerUtil {

    private static final String TAG = "WifiManagerUtil";

    private static final String SSID = "3BU-office";

    private static final String PASSWORD = "3bu2017!";

    private static final String INET_ADDR = "inet addr:";

    private static final String BCAST = "Bcast:";

    private static final String MASK = "Mask:";

    private static final String UP = "UP";

    private WifiManager mWifiManager;

    private Context mContext;

    private static WifiManagerUtil instance;

    private NetConnectBrocastReceiver mNetReceiver;

    private ShowNetInfoCallback mCallback;


    public static WifiManagerUtil getInstance() {
        if (instance == null) {
            instance = new WifiManagerUtil();
        }
        return instance;
    }

    public void init(Context context) {
        this.mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(mContext.WIFI_SERVICE);
        mNetReceiver = new NetConnectBrocastReceiver();
        registerNetBrocast();
    }

    public void setCallback(ShowNetInfoCallback callback) {
        this.mCallback = callback;
    }

    public void connectWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        if (!isConnectWifi(mContext)) {
            LogManager.d(TAG, "not connect wifi");
            WifiConfiguration wifiConfiguration = createWifiInfo();
            int wcgID = mWifiManager.addNetwork(wifiConfiguration);
            boolean isConnect = mWifiManager.enableNetwork(wcgID, true);
            LogManager.d(TAG, " connectWifi wcgID " + wcgID + " isConnect: " + isConnect);
        }
    }

    /**
     * @return 网络配置信息
     */
    private WifiConfiguration createWifiInfo() {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        WifiConfiguration tempConfig = isExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }
        config.preSharedKey = "\"" + PASSWORD + "\"";
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        return config;
    }

    /**
     * @param SSID 网络名称
     * @return 是否存在该网络
     */
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            LogManager.d(TAG, "existingConfig: " + existingConfig.SSID);
            if (existingConfig.SSID.equals("/" + SSID + "/")) {
                return existingConfig;
            }
        }
        return null;
    }

    public boolean isConnectWifi(Context mContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.isAvailable() && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    /**
     * 断开网络
     *
     * @param context 上下文
     */
    public void disconnect(Context context) {
        mWifiManager.disconnect();
    }


    private void registerNetBrocast() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetReceiver, mFilter);
    }

    class NetConnectBrocastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (isConnectWifi(mContext)) {
                    LogManager.d(TAG, "onReceive connect wifi success");
                    getNetInfoFromCmd();
                }

            }
        }
    }

    /**
     * 获取ip等信息
     */
    private void getNetInfoFromCmd() {
        String result = "";
        result = CmdUtils.resultExeCmd("busybox ifconfig");
        String[] spiltStr = result.split("wlan0");
        if (spiltStr == null || spiltStr.length <= 0) {
            return;
        }
        String wlanPart = spiltStr[1];
        if (wlanPart != null && wlanPart.contains(INET_ADDR)) {
            //获取ip字段
            int addrIndex = wlanPart.indexOf(INET_ADDR);
            int startIndex = addrIndex + INET_ADDR.length();
            int endIndex = wlanPart.indexOf(BCAST);
            String ip = wlanPart.substring(startIndex, endIndex).replaceAll(" ", "");
            //获取子网掩码字段
            int maskIndex = wlanPart.indexOf(MASK);
            int maskEndIndex = wlanPart.indexOf(UP);
            int maskStartIndex = maskIndex + MASK.length();
            String mask = wlanPart.substring(maskStartIndex, maskEndIndex).replaceAll(" ", "").replaceAll("\n", "");
            mCallback.onShowWifiInfo(ip, mask);
        }
    }

    public void destroy() {
        instance = null;
        if (mNetReceiver != null) {
            mContext.unregisterReceiver(mNetReceiver);
        }
    }

    /**
     * 展示无线网络ip以及子网掩码的接口
     */
    public interface ShowNetInfoCallback {
        void onShowWifiInfo(String ip, String mask);
    }
}
