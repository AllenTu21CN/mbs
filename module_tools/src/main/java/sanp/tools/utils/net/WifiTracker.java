package sanp.tools.utils.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by huang on 2017/6/20.
 */

public class WifiTracker {

    private static String TAG = "WifiTracker";

    private WifiManager mWifiManager;

    private ConnectivityManager mConnectivityManager;

    private Context mContext;

    private final AtomicBoolean mConnected = new AtomicBoolean(false);
    //注册广播事件
    private boolean mRegistered;
    private IntentFilter mFilter;

    private List<AccessPoint> accessPoints = new ArrayList<>();

    private WifiTrackerListener mlistener;

    private MainHandler mainHandler;

    private WorkHandler workHandler;

    private long workTime;

    private Timer myTimer;

    public WifiTracker(Context context, WifiTrackerListener listener) {
        mContext = context;
        mlistener = listener;
        // 取得Manager对象
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    public boolean setWifiEnabled(boolean flag) {
        return mWifiManager.setWifiEnabled(flag);
    }

    public boolean getWifiEnabled() {
        return (mWifiManager.isWifiEnabled() || mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_ENABLING);
    }

    /**
     * 更新当前热点信息
     */
    private synchronized void updateAccessPoints() {

        //Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();
        //cachedAccessPoints
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();

        final List<ScanResult> results = filterScanResult();
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
/*        NetworkInterface networkInterface = null;
        try {
            Inet4Address inet4Address = CommonUtil.getPrivateByName(wifiInfo,"mIpAddress",Inet4Address.class);
            if (inet4Address!=null){
                networkInterface = NetworkInterface.getByInetAddress(inet4Address);
            }
        } catch (SocketException e) {
           LogManager.e(e);
        }*/
        String ssid = AccessPoint.removeDoubleQuotes(wifiInfo.getSSID());
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]") || result.capabilities.contains("[WPA-EAP-TKIP]")) {
                    continue;
                }
                AccessPoint accessPoint = null;
                accessPoint = getCachedOrCreate(result, accessPoints);
                if (accessPoint != null) {
                    if (networkInfo != null && wifiInfo != null && accessPoint.getSsid().equals(ssid)) {
                        if (networkInfo.getDetailedState().equals(NetworkInfo.DetailedState.CONNECTED)) {
                            accessPoint.setConnect(AccessPoint.CONNECTED);
                        } else {
                            accessPoint.setConnect(AccessPoint.CONNECTING);
                        }
                        accessPoint.setConnectState(networkInfo.getDetailedState());
                    } else {
                        accessPoint.setConnectState(null);
                        accessPoint.setConnect(AccessPoint.NOCONNECT);
                    }
                    accessPoints.add(accessPoint);
                }
            }
        }
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                AccessPoint accessPoint = getCachedOrCreate(config, accessPoints);
                if (accessPoint != null) {
                    if (accessPoint.getConnect() == AccessPoint.CONNECTED) {
                    }
                    accessPoints.add(accessPoint);
                }
            }
        }
        Collections.sort(accessPoints);
        if (!this.accessPoints.equals(accessPoints)) {
            this.accessPoints = accessPoints;
        }

    }

    private AccessPoint getCachedOrCreate(WifiConfiguration config, List<AccessPoint> cache) {
        final int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (cache.get(i).matches(config)) {
                AccessPoint ret = cache.remove(i);
                ret.loadConfig(config);
                return ret;
            }
        }
        return null;
    }

    private AccessPoint getCachedOrCreate(ScanResult result, List<AccessPoint> cache) {
        final int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (cache.get(i).matches(result)) {
                AccessPoint ret = cache.remove(i);
                ret.update(result);
                return ret;
            }
        }
        return new AccessPoint(mContext, result);
    }


    private List<ScanResult> filterScanResult() {
        List<ScanResult> results = mWifiManager.getScanResults();
        Map<String, ScanResult> removeResultMap = new HashMap<>();
        for (ScanResult newResult : results) {
            if (newResult.SSID == null || newResult.SSID.isEmpty()) {
                removeResultMap.put(newResult.BSSID, newResult);
            }
        }
        results.removeAll(removeResultMap.values());
        return results;
    }


    public List<AccessPoint> getAccessPoints() {
        synchronized (accessPoints) {
            return new ArrayList<>(accessPoints);
        }

    }

    /**
     * 开始扫描
     */
    public void startTracking(long time) {
        if (!mRegistered) {
            workTime = time;
            mContext.registerReceiver(mReceiver, mFilter);
            if (mainHandler == null) mainHandler = new MainHandler(Looper.getMainLooper());
            if (workHandler == null) {
                Thread workThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        workHandler = new WorkHandler();
                        Looper.loop();
                    }
                });
                workThread.setDaemon(true);
                workThread.start();
            }
            if (workTime > 0) {
                (myTimer = new Timer()).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (workHandler != null)
                            workHandler.sendEmptyMessage(WorkHandler.GOTO_WORK);
                    }
                }, 100, workTime);
            }
            mRegistered = true;
        }
    }

    /**
     * 继续扫描
     */
    public void resumeTracking() {
        workHandler.sendEmptyMessage(WorkHandler.RESUME_WORK);

    }

    /**
     * 暂停扫描
     */
    public void pauseTracking() {
        workHandler.sendEmptyMessage(WorkHandler.PAUSE_WORK);
    }

    /**
     * 停止扫描
     */
    public void stopTracking() {
        if (mRegistered) {
            if (myTimer != null) {
                myTimer.cancel();
                myTimer = null;
            }
            if (workHandler != null) {
                workHandler.getLooper().quit();
                mContext.unregisterReceiver(mReceiver);
                workHandler = null;
                mRegistered = false;
            }
        }
    }


    public void forceRefresh() {
        workHandler.sendEmptyMessage(WorkHandler.GOTO_WORK);
    }

    //判断wifi是否连接
    public boolean isConnected() {
        return mConnected.get();
    }


    //接受wifi状态
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //wifi状态改变
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                mainHandler.sendEmptyMessage(MainHandler.WIFI_STATS_CHANGE);
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {

            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                mConnected.set(info.isConnected());
                mainHandler.sendEmptyMessage(MainHandler.WIFI_STATS_CHANGE);
            }
        }
    };

    public WifiManager getWifiManager() {
        return mWifiManager;
    }


    private class WorkHandler extends Handler {
        private AtomicBoolean isPauseWorking = new AtomicBoolean(false);
        public static final int GOTO_WORK = 1;
        public static final int PAUSE_WORK = 2;
        public static final int RESUME_WORK = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GOTO_WORK:
                    if (isPauseWorking.get()) break;
                    this.removeMessages(GOTO_WORK);
                    updateAccessPoints();
                    mainHandler.sendEmptyMessage(MainHandler.WIFI_REFRESH_FINISH);
                    break;
                case PAUSE_WORK:
                    isPauseWorking.set(true);
                    break;
                case RESUME_WORK:
                    isPauseWorking.set(false);
                    break;
            }
        }
    }

    private class MainHandler extends Handler {
        public static final int WIFI_STATS_CHANGE = 1;
        public static final int WIFI_REFRESH_FINISH = 2;

        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (workHandler == null) return;
            switch (msg.what) {
                case WIFI_STATS_CHANGE:
                    workHandler.sendEmptyMessage(WorkHandler.GOTO_WORK);
                    mlistener.onUpdating();
                    break;
                case WIFI_REFRESH_FINISH:
                    mlistener.onUpdate(getAccessPoints());
                    break;
            }
        }
    }

    public interface WifiTrackerListener {
        public void onUpdating();

        public void onUpdate(List<AccessPoint> list);
    }


}

