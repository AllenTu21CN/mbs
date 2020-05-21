package com.sanbu.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.sanbu.android.EthernetManager;
import com.sanbu.android.IpCfg;
import com.sanbu.android.StaticIpConfiguration;
import com.sanbu.base.NetInfo;
import com.sanbu.base.NetType;

import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

public class NetworkUtil {

    private static final String TAG = NetworkUtil.class.getSimpleName();

    public static NetType getActiveType(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null || !info.isConnected())
            return NetType.None;
        else if (info.getType() == ConnectivityManager.TYPE_MOBILE)
            return NetType.Mobile;
        else if (info.getType() == ConnectivityManager.TYPE_WIFI)
            return NetType.WiFi;
        else if (info.getType() == ConnectivityManager.TYPE_ETHERNET)
            return NetType.Ethernet;
        else
            return NetType.None;
    }

    public static boolean IsMobileConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
    }

    public static boolean IsWiFiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    }

    public static boolean IsEthernetConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnected();
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or null
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return null;
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return null;
    }

    /**
     * Get IPv4 address from first non-localhost interface
     *
     * @return address or null
     */
    public static String getLocalIpAddress() {
        return getLocalIpAddress(false);
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param usingIPv6 true=return ipv6, false=return ipv4
     * @return address or null
     */
    public static String getLocalIpAddress(boolean usingIPv6) {
        return getInterfaceAddress(null, usingIPv6);
    }

    public static String getInterfaceAddress(String interfacePrefix, boolean usingIPv6) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (interfacePrefix != null && !intf.getName().startsWith(interfacePrefix))
                    continue;

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (inetAddress.isLoopbackAddress())
                        continue;
                    if ((usingIPv6 && inetAddress instanceof Inet6Address) ||
                            (!usingIPv6 && inetAddress instanceof Inet4Address)) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getActiveLocalIpAddress(Context context) {
        return getActiveLocalIpAddress(context, false);
    }

    public static String getActiveLocalIpAddress(Context context, boolean usingIPv6) {
        NetType type = getActiveType(context);
        if (type == NetType.Mobile) {
            return getLocalIpAddress(usingIPv6);
        } else if (type == NetType.WiFi) {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return Formatter.formatIpAddress(manager.getConnectionInfo().getIpAddress());
        } else if (type == NetType.Ethernet) {
            return getInterfaceAddress("eth", usingIPv6);
        } else {
            return null;
        }
    }

    public static NetInfo getActiveNetInfo(Context context) {
        NetType type = getActiveType(context);
        if (type == NetType.None) {
            return NetInfo.buildEmpty();
        } else if (type == NetType.WiFi) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            String ip = Formatter.formatIpAddress(dhcpInfo.ipAddress);
            String mask = Formatter.formatIpAddress(dhcpInfo.netmask);
            String gateway = Formatter.formatIpAddress(dhcpInfo.gateway);
            String dns1 = Formatter.formatIpAddress(dhcpInfo.dns1);
            String dns2 = Formatter.formatIpAddress(dhcpInfo.dns2);
            return new NetInfo(type, ip, mask, gateway, dns1, dns2);
        } else if (type == NetType.Ethernet) {
            EthernetManager ethManager = EthernetManager.getInstance(context);
            if (ethManager.isAPIReady()) {
                try {
                    IpCfg ipCfg = ethManager.getConfiguration();
                    if (ipCfg.ipMode == IpCfg.IpMode.STATIC) {
                        StaticIpConfiguration cfg = ipCfg.staticIpConfiguration;
                        return new NetInfo(type, cfg.ip, cfg.mask, cfg.gateway, cfg.dns, cfg.dns2);
                    } else {
                        LogUtil.v(TAG, "can not get eth net info from EthernetManager while in dhcp mode, try again with ConnectivityManager");
                    }
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    LogUtil.w(TAG, "get eth net info from EthernetManager failed, try again with ConnectivityManager");
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String ip = null, mask = null, gateway = null, dns1 = null, dns2 = null;

            // try to get net info from ConnectivityManager, BUT gateway maybe not EXACT
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = manager.getActiveNetwork();
            LinkProperties properties = manager.getLinkProperties(network);
            List<LinkAddress> addresses = properties.getLinkAddresses();
            List<RouteInfo> routes = properties.getRoutes();
            List<InetAddress> dnsServers = properties.getDnsServers();

            // select a valid ip and mask from addresses
            while (addresses.size() > 0) {
                for (LinkAddress address : addresses) {
                    if (ip != null || address.getAddress() instanceof Inet4Address) {
                        ip = address.getAddress().getHostAddress();
                        mask = Formatter.formatIpAddress(address.getPrefixLength());
                        break;
                    }
                }
                if (ip == null)
                    ip = ""; // flag to get any valid address
                else
                    break;
            }

            // select a valid gateway from routes
            while (routes.size() > 0) {
                for (RouteInfo route : routes) {
                    if (gateway != null || (route.isDefaultRoute() && route.getGateway() instanceof Inet4Address)) {
                        gateway = route.getGateway().getHostAddress();
                        break;
                    }
                }
                if (gateway == null)
                    gateway = ""; // flag to get any valid gateway
                else
                    break;
            }

            // get dns
            if (dnsServers.size() > 0)
                dns1 = dnsServers.get(0).getHostAddress();
            if (dnsServers.size() > 1)
                dns2 = dnsServers.get(1).getHostAddress();

            return new NetInfo(type, ip, mask, gateway, dns1, dns2);
        } else {
            LogUtil.w(TAG, "current sdk version < M(23), can not get the whole net info from ConnectivityManager");
            String ip = getActiveLocalIpAddress(context);
            return new NetInfo(type, ip, "", "", "", null);
        }
    }

    public static boolean isIpAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        Pattern pattern = Pattern.compile("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}");
        return pattern.matcher(address).matches();
    }

    public static boolean isPort(String port) {
        Pattern pattern = Pattern.compile("([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-5]{2}[0-3][0-5])");
        return pattern.matcher(port).matches();
    }
}
