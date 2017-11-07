package sanp.avalon.libs.base.utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/4/12.
 */

public class WifiSetUtils {

    private static Context mContext;
    private static int mNetworkId;
    private static WifiManager wifiManager;
    private WifiInfo connectionInfo;

    public static WifiSetUtils getInstance(Context context){
        WifiSetUtils wifiu = new WifiSetUtils();
        wifiu.mContext = context;
        wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        return wifiu;
    }

    private WifiConfiguration getWifiConf()
    {
        WifiConfiguration wifiConfig = null;
        //得到连接的wifi网络
        connectionInfo = wifiManager.getConnectionInfo();

        List<WifiConfiguration> configuredNetworks = wifiManager
                .getConfiguredNetworks();
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.networkId == connectionInfo.getNetworkId()) {
                mNetworkId = conf.networkId;
                wifiConfig = conf;
                break;
            }
        }
        return wifiConfig;
    }

    // 断开指定ID的网络
    public void disconnectWifi() {
        wifiManager.removeNetwork(mNetworkId);
        wifiManager.disableNetwork(mNetworkId);
        wifiManager.disconnect();
    }

    /*public void disconnectWifi(int netId) {
        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();
    }*/
    public int getNetworkId() {

        return (connectionInfo == null) ? 0 : connectionInfo.getNetworkId();
    }
    //断开网路的同时，清空配置信息
    /*public void removeWifi() {
        disconnectWifi();
        wifiManager.removeNetwork(mNetworkId);
    }*/

    public void setIpAddress(InetAddress addr, int prefixLength) throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, NoSuchMethodException,
            ClassNotFoundException, InstantiationException,
            InvocationTargetException{
        WifiConfiguration wifiConf = getWifiConf();
        Object linkProperties = getField(wifiConf, "linkProperties");
        if (linkProperties == null)
            return;
        Class<?> laClass = Class.forName("android.net.LinkAddress");
        Constructor<?> laConstructor = laClass.getConstructor(new Class[] {
                InetAddress.class, int.class });
        Object linkAddress = laConstructor.newInstance(addr, prefixLength);


        ArrayList<Object> mLinkAddresses = (ArrayList<Object>) getDeclaredField(
                linkProperties, "mLinkAddresses");
        mLinkAddresses.clear();
        mLinkAddresses.add(linkAddress);
        wifiManager.updateNetwork(wifiConf);
    }

    public void setGateway(InetAddress gateway)
            throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException,
            InvocationTargetException{
        WifiConfiguration wifiConf = getWifiConf();
        Object linkProperties = getField(wifiConf, "linkProperties");
        if (linkProperties == null)
            return;

        if (android.os.Build.VERSION.SDK_INT >= 14) { // android4.x版本
            Class<?> routeInfoClass = Class.forName("android.net.RouteInfo");
            Constructor<?> routeInfoConstructor = routeInfoClass
                    .getConstructor(new Class[] { InetAddress.class });
            Object routeInfo = routeInfoConstructor.newInstance(gateway);

            ArrayList<Object> mRoutes = (ArrayList<Object>)getDeclaredField(
                    linkProperties, "mRoutes");
            mRoutes.clear();
            mRoutes.add(routeInfo);
        } else { // android3.x版本
            ArrayList<InetAddress> mGateways = (ArrayList<InetAddress>) getDeclaredField(
                    linkProperties, "mGateways");
            mGateways.clear();
            mGateways.add(gateway);
        }
        wifiManager.updateNetwork(wifiConf);
    }

    public void setDNS(InetAddress dns)
            throws SecurityException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, UnknownHostException{
        WifiConfiguration wifiConf = getWifiConf();
        Object linkProperties = getField(wifiConf, "linkProperties");
        if (linkProperties == null)

            return;
        ArrayList<InetAddress> mDnses = (ArrayList<InetAddress>)
                getDeclaredField(linkProperties, "mDnses");
        mDnses.clear(); // 清除原有DNS设置（如果只想增加，不想清除，词句可省略）
        mDnses.add(dns);
        //增加新的DNS
        wifiManager.updateNetwork(wifiConf);
    }


    private void setEnumField(Object obj, String value, String name)throws SecurityException, NoSuchFieldException,IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    private Object getField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }

    private Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        return out;
    }
}
