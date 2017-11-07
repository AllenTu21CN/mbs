package sanp.mp100.test.ui.view;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;

import sanp.tools.utils.LogManager;
import sanp.tools.utils.ScreenUtils;

import sanp.mp100.R;
import sanp.mp100.utils.PingNetUtil;
import sanp.mp100.test.ui.utils.WifiManagerUtil;
import sanp.tools.utils.net.EthernetManager;
import sanp.tools.utils.net.IpCfg;
import sanp.tools.utils.net.StaticIpConfiguration;

/**
 * Created by zhangxd on 2017/7/25.
 */

public class DeviceNetWorkPopup extends PopupWindow implements PingNetUtil.PingIpCallback, WifiManagerUtil.ShowNetInfoCallback {

    private static final String TAG = "DeviceNetWorkPopup";
    private static final String NET_MASK = "255.255.0.0";
    private static final String NET_GATEWAY = "10.1.0.1";
    private static final String NET_DNS = "10.1.0.1";
    private static final int MSG_PING_SUCCESS = 0x01;
    private static final int MSG_PING_FAILURE = 0x02;
    private Context mContext;
    private TextView mConfigMode;
    private TextView mIPAddress;
    private TextView mGateWay;
    //网络掩码
    private TextView mNetMask;
    private TextView mDNS;
    private TextView mNetSpeed;
    private View mView;
    private EthernetManager mEthernetManager;
    private StaticIpConfiguration ipSettings;
    private String[] ipArr = {"10.1.11.8", "10.1.11.18", "10.1.11.28", "10.1.11.38", "10.1.11.48"};
    private int pingNum = 0;
    private boolean hasShowEtherToast;

    public DeviceNetWorkPopup(Context context) {
        mContext = context;
        int width = ScreenUtils.getScreenWidth(mContext) / 2;
        int height = ScreenUtils.getScreenHeight(mContext) / 2;
        setWidth(width);
        setHeight(height);
        initView();
        setContentView(mView);
        showAtLocation(mView, Gravity.CENTER, 0, 0);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PING_SUCCESS:
                    setEtherInfoTxt();
                    Toast.makeText(mContext, mContext.getString(R.string.connect_eth_success), Toast.LENGTH_LONG).show();
                    break;
                case MSG_PING_FAILURE:
                    Toast.makeText(mContext, mContext.getString(R.string.ether_test_toast), Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    private void initView() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.network_info_view, null);
        mConfigMode = (TextView) mView.findViewById(R.id.net_config);
        mIPAddress = (TextView) mView.findViewById(R.id.net_ip);
        mNetMask = (TextView) mView.findViewById(R.id.net_mask);
        mGateWay = (TextView) mView.findViewById(R.id.net_gateway);
        mDNS = (TextView) mView.findViewById(R.id.net_dns);
        mNetSpeed = (TextView) mView.findViewById(R.id.net_speed);
    }

    public DeviceNetWorkPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onPingSuccess() {
        handler.sendEmptyMessage(MSG_PING_SUCCESS);
        LogManager.d(TAG, "pingSuccess");
    }

    @Override
    public void onPingFail() {
        LogManager.d(TAG, "pingFail pingNum: " + pingNum);
        if (!hasShowEtherToast) {
            handler.sendEmptyMessage(MSG_PING_FAILURE);
        }
        hasShowEtherToast = true;
        if (pingNum < ipArr.length) {
            pingNum++;
            setEtherConfig(ipArr[pingNum]);
        }
    }

    /**
     * 查询网络配置信息
     *
     * @param wifi 是否是无线
     */
    public void queryNetInfo(boolean wifi) {
        if (wifi) {
            Toast.makeText(mContext, mContext.getString(R.string.wifi_test_toast), Toast.LENGTH_LONG).show();
            WifiManagerUtil.getInstance().init(mContext.getApplicationContext());
            WifiManagerUtil.getInstance().setCallback(this);
            WifiManagerUtil.getInstance().connectWifi();
        } else {
            PingNetUtil.getInstance().setPingCallback(this);
            initEtherManager();
            setEtherConfig(ipArr[pingNum]);
        }
    }

    public void dismissPopup() {
        dismiss();
        WifiManagerUtil.getInstance().destroy();
    }

    /**
     * 设置有线静态方式网络配置
     */
    private void setEtherConfig(String ipString) {
        IpCfg ipcfg = new IpCfg();
        ipcfg.ipMode = IpCfg.IpMode.STATIC;
        ipSettings = new StaticIpConfiguration();
        ipSettings.ipAddress = ipString;
        ipSettings.dnsServers.add(NET_DNS);
        ipSettings.mask = NET_MASK;
        ipSettings.gateway = NET_GATEWAY;
        ipcfg.staticIpConfiguration = ipSettings;
        try {
            mEthernetManager.setConfiguration(ipcfg);
        } catch (IllegalAccessException e) {
            LogManager.e(TAG, e);
        } catch (InstantiationException e) {
            LogManager.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogManager.e(TAG, e);
        } catch (Exception e) {
            LogManager.e(TAG, e);
        }
        PingNetUtil.getInstance().pingIpConnect(ipString);
    }

    private void initEtherManager() {
        try {
            mEthernetManager = EthernetManager.createInstance((Activity) mContext);
        } catch (NoSuchMethodException e) {
            LogManager.e(TAG, e);
        } catch (ClassNotFoundException e) {
            LogManager.e(TAG, e);
        } catch (NoSuchFieldException e) {
            LogManager.e(TAG, e);
        }
    }

    /**
     * 有线网络下信息显示
     */
    private void setEtherInfoTxt() {
        mConfigMode.setText(mContext.getString(R.string.net_static));
        mIPAddress.setText(ipArr[pingNum]);
        mNetMask.setText(NET_MASK);
        mGateWay.setText(NET_GATEWAY);
        mDNS.setText(NET_DNS);
    }


    @Override
    public void onShowWifiInfo(String ip, String mask) {
        Toast.makeText(mContext, mContext.getString(R.string.connect_wlan_success), Toast.LENGTH_LONG).show();
        mConfigMode.setText(mContext.getString(R.string.net_static));
        mIPAddress.setText(ip);
        mNetMask.setText(mask);
    }
}
