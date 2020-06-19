package cn.lx.mbs.ui.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import cn.lx.mbs.LXConst;
import cn.lx.mbs.support.MBS;
import com.sanbu.tools.LogUtil;

/**
 * Monitor network state change broadcasting
 */
public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkReceiver.class.getSimpleName();

    private static Context gContext;
    private static NetworkReceiver gInstance;

    public static void register(Context context) {
        if (gInstance == null) {
            synchronized (NetworkReceiver.class) {
                if (gInstance == null) {
                    gContext = context;
                    gInstance = new NetworkReceiver();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                    intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
                    context.registerReceiver(gInstance, intentFilter);
                }
            }
        }
    }

    public static void unregister() {
        synchronized (NetworkReceiver.class) {
            if (gContext != null && gInstance != null) {
                gContext.unregisterReceiver(gInstance);
                gContext = null;
                gInstance = null;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            LogUtil.i(LXConst.TAG, TAG, "NETWORK_STATE_CHANGED");
            MBS.getInstance().onNetworkChanged();
        }
    }
}
