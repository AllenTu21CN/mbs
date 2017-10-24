/**
 * Created by Tuyj on 2017/5/31.
 * Wrapper for android.net.EthernetManager
 */

package sanp.avalon.libs.network.ip;

import android.app.Activity;
import android.net.LinkAddress;
import android.provider.Settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import sanp.avalon.libs.base.utils.LogManager;

/**
 * A class representing the IP configuration of the Ethernet network.
 */
public class EthernetManager {

    private final String ETHERNET_SERVICE_NAME = "ethernet";
    /**
     * Whether to use static IP and other static network attributes.
     * @hide
     * Set to 1 for true and 0 for false.
     *
     */
    public static final String ETHERNET_USE_STATIC_IP = "ethernet_use_static_ip";

    /**
     * The static IP address.
     * Example: "192.168.1.51"
     * @hide
     */
    public static final String ETHERNET_STATIC_IP = "ethernet_static_ip";

    /**
     * If using static IP, the gateway's IP address.
     * Example: "192.168.1.1"
     * @hide
     *
     */

    public static final String ETHERNET_STATIC_GATEWAY = "ethernet_static_gateway";

    /**
     * If using static IP, the net mask.
     * Example: "192.168.1.1"
     * @hide
     */

    public static final String ETHERNET_STATIC_NETMASK = "ethernet_static_netmask";

    /**
     * If using static IP, the dns1
     * Example: "192.168.1.1"
     * @hide
     */

    public static final String ETHERNET_STATIC_DNS1 = "ethernet_static_dns1";

    /**
     * If using static IP, the dns2.
     * @hide
     * Example: "192.168.1.1"
     */
    public static final String ETHERNET_STATIC_DNS2 = "ethernet_static_dns2";

    public static final int ETHER_STATE_DISCONNECTED = 0;

    public static final int ETHER_STATE_CONNECTING = 1;

    public static final int ETHER_STATE_CONNECTED = 2;

    public static final String ETHERNET_STATE_CHANGED_ACTION = "android.net.ethernet.ETHERNET_STATE_CHANGED";

    public static final String EXTRA_ETHERNET_STATE = "ethernet_state";


    static private EthernetManager mEthernetManager = null;

    static public EthernetManager createInstance(Activity context) throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        if (mEthernetManager == null) {
            synchronized (EthernetManager.class) {
                if (mEthernetManager == null) {
                    mEthernetManager = new EthernetManager(context);
                }
            }
        }
        return mEthernetManager;
    }

    static public EthernetManager getInstance() {
        return mEthernetManager;
    }

    private Activity mContext = null;
    private Class<?> mEthernetManagerClass = null;
    private Class<?> mIpConfigurationClass = null;
    private Class<?> mStaticIpConfigurationClass = null;
    private Class<?> mIpAssignmentEnum = null;
    private Class<?> mLinkAddressClass = null;

    private Method mEtheIsAvailableMethod = null;
    private Method mEtheGetConfigurationMethod = null;
    private Method mEtheSetConfigurationMethod = null;

    private Field mIpCfgIpAssignmentField = null;
    private Field mIpCfgStaticIpConfigurationField = null;

    private Field mSIpCfgIpAddressField = null;
    private Field mSIpCfgGatewayField = null;
    private Field mSIpCfgDnsServersField = null;

    private Method mIpAssignmentValueOfMethod = null;
    private Method mGetEthernetConnectState = null;
/*    private Method setEthernetEnabledMethod = null;
    private Method getEthernetIfaceStateMethod = null;*/
    private Method mArrayListAddAllMethod = null;

    private Constructor mLinkAddressConstructor = null;

    private EthernetManager(Activity context) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
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
            mGetEthernetConnectState = mEthernetManagerClass.getMethod("getEthernetConnectState");

            mIpCfgIpAssignmentField = mIpConfigurationClass.getField("ipAssignment");
            mIpCfgStaticIpConfigurationField = mIpConfigurationClass.getField("staticIpConfiguration");

            mSIpCfgIpAddressField = mStaticIpConfigurationClass.getField("ipAddress");
            mSIpCfgGatewayField = mStaticIpConfigurationClass.getField("gateway");
            mSIpCfgDnsServersField = mStaticIpConfigurationClass.getField("dnsServers");

            mIpAssignmentValueOfMethod = mIpAssignmentEnum.getMethod("valueOf", String.class);
            mArrayListAddAllMethod = ArrayList.class.getMethod("addAll", Collection.class);
            mLinkAddressConstructor = mLinkAddressClass.getConstructor(InetAddress.class, int.class);
            //setEthernetEnabledMethod = mEthernetManagerClass.getMethod("setEthernetEnabled",boolean.class);
            //getEthernetIfaceStateMethod = mEthernetManagerClass.getMethod("getEthernetIfaceState");
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            LogManager.e("reflectively init failed while create EthernetManager");
            throw e;
        }
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
            LogManager.e("reflectively invoke `EthernetManager.isAvailable` failed");
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
                LogManager.e("IpConfiguration is not set");
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
            LogManager.e("reflectively invoke failed while getConfiguration");
            throw e;
        }

        cfg.ipMode = IpCfg.IpMode.STATIC;
        cfg.staticIpConfiguration = new StaticIpConfiguration();
        cfg.staticIpConfiguration.ipAddress = ipAddress.getAddress().getHostAddress();
        cfg.staticIpConfiguration.mask = Ipv4Mask2String(ipAddress.getPrefixLength());
        cfg.staticIpConfiguration.gateway = gateway.getHostAddress();
        for (InetAddress addr : dnsServers)
            cfg.staticIpConfiguration.dnsServers.add(addr.getHostAddress());
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
                    address = InetAddress.getByName(config.staticIpConfiguration.ipAddress);
                    mask = Ipv4String2Mask(config.staticIpConfiguration.mask);
                    gateway = InetAddress.getByName(config.staticIpConfiguration.gateway);
                    dnsServers = new ArrayList<>();
                    for (String dns : config.staticIpConfiguration.dnsServers)
                        dnsServers.add(InetAddress.getByName(dns));
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
                Settings.System.putInt(mContext.getContentResolver(),EthernetManager.ETHERNET_USE_STATIC_IP,1);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_IP,config.staticIpConfiguration.ipAddress);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_GATEWAY,config.staticIpConfiguration.gateway);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_NETMASK,config.staticIpConfiguration.mask);
                if (config.staticIpConfiguration.dnsServers.size() > 0)Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_DNS1,config.staticIpConfiguration.dnsServers.get(0));
                if (config.staticIpConfiguration.dnsServers.size() > 1)Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_DNS2,config.staticIpConfiguration.dnsServers.get(1));
            } else if (config.ipMode == IpCfg.IpMode.DHCP) {
                ipAssignment = mIpAssignmentValueOfMethod.invoke(null, "DHCP");
                Settings.System.putInt(mContext.getContentResolver(),EthernetManager.ETHERNET_USE_STATIC_IP,0);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_IP,null);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_GATEWAY,null);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_NETMASK,null);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_DNS1,null);
                Settings.System.putString(mContext.getContentResolver(),EthernetManager.ETHERNET_STATIC_DNS2,null);
            } else {
                throw new RuntimeException("Unkonw mode");
            }
            mIpCfgIpAssignmentField.set(ipCfg, ipAssignment);
            mIpCfgStaticIpConfigurationField.set(ipCfg, staticIpConfiguration);
            mEtheSetConfigurationMethod.invoke(ethernetService, ipCfg);


        } catch (java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LogManager.e("reflectively invoke failed while setConfiguration");
            throw e;
        }
    }

    public int getEthernetConnectState(){
        try {
            return (int)mGetEthernetConnectState.invoke(getEthernetService());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return ETHER_STATE_DISCONNECTED;
    }

    private Object getEthernetService() {
        return mContext.getSystemService(ETHERNET_SERVICE_NAME);
    }

    static public void logEthe(IpCfg cfg) {
        LogManager.i("Current Ethernet Configuration mode " + cfg.ipMode.toString());
        if (cfg.staticIpConfiguration != null)
            LogManager.i(String.format("Ip: %s Mask: %s Gateway: %s DNSs: %s",
                    cfg.staticIpConfiguration.ipAddress,
                    cfg.staticIpConfiguration.mask,
                    cfg.staticIpConfiguration.gateway,
                    cfg.staticIpConfiguration.dnsServers));
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

}
