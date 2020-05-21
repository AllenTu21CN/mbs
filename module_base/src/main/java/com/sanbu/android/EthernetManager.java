/**
 * Created by Tuyj on 2017/5/31.
 * Wrapper for android.net.EthernetManager
 */

package com.sanbu.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.LinkAddress;
import android.provider.Settings;

import com.sanbu.tools.LogUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class representing the IP configuration of the Ethernet network.
 */
public class EthernetManager {

    private final String ETHERNET_SERVICE_NAME = "ethernet";
    /**
     * Whether to use static IP and other static network attributes.
     *
     * @hide Set to 1 for true and 0 for false.
     */
    public static final String ETHERNET_USE_STATIC_IP = "ethernet_use_static_ip";

    /**
     * The static IP address.
     * Example: "192.168.1.51"
     *
     * @hide
     */
    public static final String ETHERNET_STATIC_IP = "ethernet_static_ip";

    /**
     * If using static IP, the gateway's IP address.
     * Example: "192.168.1.1"
     *
     * @hide
     */

    public static final String ETHERNET_STATIC_GATEWAY = "ethernet_static_gateway";

    /**
     * If using static IP, the net mask.
     * Example: "192.168.1.1"
     *
     * @hide
     */

    public static final String ETHERNET_STATIC_NETMASK = "ethernet_static_netmask";

    /**
     * If using static IP, the dns1
     * Example: "192.168.1.1"
     *
     * @hide
     */

    public static final String ETHERNET_STATIC_DNS1 = "ethernet_static_dns1";

    /**
     * If using static IP, the dns2.
     *
     * @hide Example: "192.168.1.1"
     */
    public static final String ETHERNET_STATIC_DNS2 = "ethernet_static_dns2";

    public static final int ETHER_STATE_DISCONNECTED = 0;

    public static final int ETHER_STATE_CONNECTING = 1;

    public static final int ETHER_STATE_CONNECTED = 2;

    public static final String ETHERNET_STATE_CHANGED_ACTION = "android.net.ethernet.ETHERNET_STATE_CHANGED";

    public static final String EXTRA_ETHERNET_STATE = "ethernet_state";


    static private EthernetManager mEthernetManager = null;

    public static EthernetManager getInstance(Context context) {
        if (mEthernetManager == null) {
            synchronized (EthernetManager.class) {
                if (mEthernetManager == null) {
                    mEthernetManager = new EthernetManager(context);
                }
            }
        }
        return mEthernetManager;
    }

    private Context mContext;
    private boolean mIsAPIReady;
    private Class<?> mEthernetManagerClass;
    private Class<?> mIpConfigurationClass;
    private Class<?> mStaticIpConfigurationClass;
    private Class<?> mIpAssignmentEnum;
    private Class<?> mLinkAddressClass;

    private Method mEtheIsAvailableMethod;
    private Method mEtheGetConfigurationMethod;
    private Method mEtheSetConfigurationMethod;
    private Method mGetNetMask;

    private Field mIpCfgIpAssignmentField;
    private Field mIpCfgStaticIpConfigurationField;

    private Field mSIpCfgIpAddressField;
    private Field mSIpCfgGatewayField;
    private Field mSIpCfgDnsServersField;

    private Method mIpAssignmentValueOfMethod;
    private Method mGetEthernetConnectState;
    /*
    private Method setEthernetEnabledMethod;
    private Method getEthernetIfaceStateMethod;
    */
    private Method mArrayListAddAllMethod;

    private Constructor mLinkAddressConstructor;

    public EthernetManager(Context context) {
        mContext = context;
        try {
            mEthernetManagerClass = Class.forName("android.net.EthernetManager");
            mIpConfigurationClass = Class.forName("android.net.IpConfiguration");
            mIpAssignmentEnum = Class.forName("android.net.IpConfiguration$IpAssignment");
            mStaticIpConfigurationClass = Class.forName("android.net.StaticIpConfiguration");
            mLinkAddressClass = Class.forName("android.net.LinkAddress");

            mEtheIsAvailableMethod = mEthernetManagerClass.getMethod("isAvailable");
            mEtheGetConfigurationMethod = mEthernetManagerClass.getMethod("getConfiguration");
            mEtheSetConfigurationMethod = mEthernetManagerClass.getMethod("setConfiguration", mIpConfigurationClass);
            mIpCfgIpAssignmentField = mIpConfigurationClass.getField("ipAssignment");
            mIpCfgStaticIpConfigurationField = mIpConfigurationClass.getField("staticIpConfiguration");
            mSIpCfgIpAddressField = mStaticIpConfigurationClass.getField("ipAddress");
            mSIpCfgGatewayField = mStaticIpConfigurationClass.getField("gateway");
            mSIpCfgDnsServersField = mStaticIpConfigurationClass.getField("dnsServers");

            mIpAssignmentValueOfMethod = mIpAssignmentEnum.getMethod("valueOf", String.class);
            mArrayListAddAllMethod = ArrayList.class.getMethod("addAll", Collection.class);
            mLinkAddressConstructor = mLinkAddressClass.getConstructor(InetAddress.class, int.class);

            // mGetEthernetConnectState = mEthernetManagerClass.getMethod("getEthernetConnectState");
            // mGetNetMask=mEthernetManagerClass.getMethod("getNetmask");
            // getEthernetIfaceStateMethod = mEthernetManagerClass.getMethod("getEthernetIfaceState");
            mIsAPIReady = true;
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            LogUtil.e("reflectively init failed while create EthernetManager:");
            e.printStackTrace();
            mIsAPIReady = false;
        }
    }

    public boolean isAPIReady() {
        return mIsAPIReady;
    }

    /**
     * Indicates whether the system currently has one or more
     * Ethernet interfaces.
     */
    public boolean isAvailable() throws InvocationTargetException, IllegalAccessException {
        Object ethernetService = getEthernetService();
        try {
            return (boolean) mEtheIsAvailableMethod.invoke(ethernetService);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            LogUtil.e("reflectively invoke `EthernetManager.isAvailable` failed");
            throw e;
        }
    }

    /**
     * Get Ethernet configuration.
     *
     * @return the Ethernet Configuration, contained in {@link IpCfg}.
     */
    public IpCfg getConfiguration() throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Object ethernetService = getEthernetService();
        IpCfg cfg = new IpCfg();
        Object ipCfg = null;
        String ipAssignment = null;
        Object staticIpConfiguration = null;
        LinkAddress ipAddress = null;
        InetAddress gateway = null;
        ArrayList<InetAddress> dnsServers = null;

        try {
            ipCfg = mEtheGetConfigurationMethod.invoke(ethernetService);
            ipAssignment = ((Enum) mIpCfgIpAssignmentField.get(ipCfg)).toString();
            if (ipAssignment.equals("UNASSIGNED")) {
                LogUtil.e("IpConfiguration is not set");
                return null;
            }
            if (ipAssignment.equals("DHCP")) {
                cfg.ipMode = IpCfg.IpMode.DHCP;
                return cfg;
            }

            staticIpConfiguration = mIpCfgStaticIpConfigurationField.get(ipCfg);
            ipAddress = (LinkAddress) mSIpCfgIpAddressField.get(staticIpConfiguration);
            gateway = (InetAddress) mSIpCfgGatewayField.get(staticIpConfiguration);
            dnsServers = (ArrayList<InetAddress>) mSIpCfgDnsServersField.get(staticIpConfiguration);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            LogUtil.e("reflectively invoke failed while getConfiguration");
            throw e;
        }

        cfg.ipMode = IpCfg.IpMode.STATIC;
        cfg.staticIpConfiguration = new StaticIpConfiguration();
        cfg.staticIpConfiguration.ip = ipAddress.getAddress().getHostAddress();
        cfg.staticIpConfiguration.mask = Ipv4Mask2String(ipAddress.getPrefixLength());
        cfg.staticIpConfiguration.gateway = gateway.getHostAddress();
        if (dnsServers.size() > 0)
            cfg.staticIpConfiguration.dns = dnsServers.get(0).getHostAddress();
        if (dnsServers.size() > 1)
            cfg.staticIpConfiguration.dns2 = dnsServers.get(1).getHostAddress();
        return cfg;
    }

    /**
     * Set Ethernet configuration.
     */
    public void setConfiguration(IpCfg config) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Object ethernetService = getEthernetService();
        Object ipCfg = null;
        Object ipAssignment = null;
        Object staticIpConfiguration = null;

        try {
            ipCfg = mIpConfigurationClass.newInstance();
    /*        if (getEthernetIfaceStateMethod.invoke(ethernetService).equals(0)){
                setEthernetEnabledMethod.invoke(ethernetService,true);
            }*/

            if (config.ipMode == IpCfg.IpMode.STATIC) {
                Object ipAddress = null;
                InetAddress address = null;
                InetAddress gateway = null;
                ArrayList<InetAddress> dnsServers = null;
                int mask;

                try {
                    address = InetAddress.getByName(config.staticIpConfiguration.ip);
                    mask = Ipv4String2Mask(config.staticIpConfiguration.mask);
                    gateway = InetAddress.getByName(config.staticIpConfiguration.gateway);
                    dnsServers = new ArrayList<>();
                    dnsServers.add(InetAddress.getByName(config.staticIpConfiguration.dns));
                } catch (UnknownHostException e) {
                    throw new RuntimeException("invalid IpCfg");
                }

                ipAddress = mLinkAddressConstructor.newInstance(address, mask);
                staticIpConfiguration = mStaticIpConfigurationClass.newInstance();
                mSIpCfgIpAddressField.set(staticIpConfiguration, ipAddress);
                mSIpCfgGatewayField.set(staticIpConfiguration, gateway);
                Object dnss = mSIpCfgDnsServersField.get(staticIpConfiguration);
                mArrayListAddAllMethod.invoke(dnss, dnsServers);

                ipAssignment = mIpAssignmentValueOfMethod.invoke(null, "STATIC");
                Settings.System.putInt(mContext.getContentResolver(), EthernetManager.ETHERNET_USE_STATIC_IP, 1);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_IP, config.staticIpConfiguration.ip);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_GATEWAY, config.staticIpConfiguration.gateway);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_NETMASK, config.staticIpConfiguration.mask);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_DNS1, config.staticIpConfiguration.dns);
            } else if (config.ipMode == IpCfg.IpMode.DHCP) {
                ipAssignment = mIpAssignmentValueOfMethod.invoke(null, "DHCP");
                Settings.System.putInt(mContext.getContentResolver(), EthernetManager.ETHERNET_USE_STATIC_IP, 0);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_IP, null);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_GATEWAY, null);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_NETMASK, null);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_DNS1, null);
                Settings.System.putString(mContext.getContentResolver(), EthernetManager.ETHERNET_STATIC_DNS2, null);
            } else {
                throw new RuntimeException("Unkonw mode");
            }
            Settings.System.putInt(mContext.getContentResolver(), "ethernet_sanbu", 1);
            mIpCfgIpAssignmentField.set(ipCfg, ipAssignment);
            mIpCfgStaticIpConfigurationField.set(ipCfg, staticIpConfiguration);
            mEtheSetConfigurationMethod.invoke(ethernetService, ipCfg);
//            mSetEthernetEnabledMethod.invoke(ethernetService,false);
//            mSetEthernetEnabledMethod.invoke(ethernetService,true);

        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            LogUtil.e("reflectively invoke failed  setConfiguration");
            e.printStackTrace();
            throw e;
        } finally {
            Settings.System.putInt(mContext.getContentResolver(), "ethernet_sanbu", 0);
        }

    }

    @Deprecated
    public int getEthernetConnectState() {
        try {
            return (int) mGetEthernetConnectState.invoke(getEthernetService());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return ETHER_STATE_DISCONNECTED;
    }

    @SuppressLint("WrongConstant")
    private Object getEthernetService() {
        return mContext.getSystemService(ETHERNET_SERVICE_NAME);
    }

    static public void logEthe(IpCfg cfg) {
        LogUtil.i("Current Ethernet Configuration mode " + cfg.ipMode.toString());
        if (cfg.staticIpConfiguration != null)
            LogUtil.i(String.format("Ip: %s Mask: %s Gateway: %s DNSs: %s",
                    cfg.staticIpConfiguration.ip,
                    cfg.staticIpConfiguration.mask,
                    cfg.staticIpConfiguration.gateway,
                    cfg.staticIpConfiguration.dns));
    }

    static public String Ipv4Mask2String(int mask) {
        if (mask < 0 || mask > 32)
            throw new RuntimeException("invalid mask. Must be [0,32]");

        byte m[] = new byte[4];
        for (int i = 0; i < 4; ++i) {
            if (mask >= 8) {
                mask -= 8;
                m[i] = (byte) 0xff;
            } else if (mask == 0) {
                m[i] = 0;
            } else {
                m[i] = (byte) (0xff - ((1 << (8 - mask)) - 1));
                mask = 0;
            }
        }
        try {
            return InetAddress.getByAddress(m).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("logica");
        }
    }

    static public int Ipv4String2Mask(String mask) {
        String items[] = mask.split("\\.");
        if (items.length != 4)
            throw new RuntimeException("invalid mask");
        int j = 0;
        for (String item : items) {
            int i = Integer.valueOf(item);
            while (i > 0) {
                if (i % 2 == 1)
                    ++j;
                i = i >> 1;
            }
        }
        return j;
    }

    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     *
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    public String intToInetAddress(int hostAddress) {
        byte[] addressBytes = {(byte) (0xff & hostAddress),
                (byte) (0xff & (hostAddress >> 8)),
                (byte) (0xff & (hostAddress >> 16)),
                (byte) (0xff & (hostAddress >> 24))};

        try {
            return InetAddress.getByAddress(addressBytes).getHostAddress();
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    @Deprecated
    public String getNetMask(){
        try {
            return (String) mGetNetMask.invoke(getEthernetService());
        }catch (IllegalAccessException e){
           LogUtil.e("IllegalAccessException");
        }catch (InvocationTargetException e){
            LogUtil.e("InvocationTargetException");
        }
        return null;
    }

}
