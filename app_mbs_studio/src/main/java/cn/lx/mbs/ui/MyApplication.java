package cn.lx.mbs.ui;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import cn.lx.mbs.Events;
import cn.lx.mbs.LXConst;
import cn.lx.mbs.support.MBS;
import cn.lx.mbs.support.structures.StreamState;
import cn.lx.mbs.ui.service.NetworkReceiver;
import com.sanbu.base.BaseEvents;
import com.sanbu.base.NetType;
import com.sanbu.tools.DBUtil;
import com.sanbu.tools.EventPub;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.NetworkUtil;
import com.sanbu.tools.Pending;
import com.sanbu.tools.SPUtil;
import com.sanbu.tools.ToastUtil;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    private static Context gContext;
    private static Handler gMainHandler;
    private static boolean gReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.w(LXConst.TAG, TAG, "onCreate");
        gContext = getApplicationContext();
        init();
    }

    public static Context getContext() {
        return gContext;
    }

    public static boolean isReady() {
        return gReady;
    }

    public static void exit() {
        try {
            // release all events
            releaseEvent();

            // stop services
            stopServices();

            // release MBS
            MBS.getInstance().release();

            // release event pub
            // EventPub.getDefaultPub().release();

            // release handler
            Pending.clean(gMainHandler);
            gMainHandler = null;
        } catch (Exception e) {
            LogUtil.e(LXConst.TAG, TAG, "exit app error", e);
        }

        LogUtil.w(LXConst.TAG, TAG, "exit app !!!");
        System.exit(0);
    }

    private static boolean init() {
        // create main handler
        gMainHandler = new Handler();

        // init log util
        LogUtil.setLogLevel(LXConst.LOG_UTIL_ENABLED_OUTPUT_LEVEL);

        // init TSX200 environment
        MBS.initEnv(gContext);

        // init toast util
        ToastUtil.init(gContext);

        // init db util
        DBUtil.init(gContext);

        // init sp util
        SPUtil.init(gContext, LXConst.SP_PATH);

        // init event pub
        EventPub.getDefaultPub().init();

        // wait for network ready
        waitNetworkReady(gContext);

        // init MBS
        MBS.getInstance().init(gContext);

        // start services
        startServices();

        // init events
        initEvent();

        gReady = true;
        return true;
    }

    private static void startServices() {
        // register NetworkReceiver
        NetworkReceiver.register(gContext);
    }

    private static void stopServices() {
        // unregister NetworkReceiver
        NetworkReceiver.unregister();
    }

    private static void waitNetworkReady(Context context) {
        for (int i = 0 ; i < 5 ; ++i) {
            if (NetworkUtil.getActiveType(context) != NetType.None)
                return;
            LogUtil.w(LXConst.TAG, TAG,"network is not ready, sleep 1s then check again");
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }

        ToastUtil.show("??????:?????????????????????", true);
    }

    private static void initEvent() {
        EventPub.getDefaultPub().subscribe(BaseEvents.USER_HINT, TAG, (evtId, arg1, arg2, obj) -> {
            gMainHandler.postDelayed(() -> ToastUtil.show((String) obj, arg1 != 0), 50);
            return false;
        });

        EventPub.getDefaultPub().subscribe(BaseEvents.RESTART_APP, TAG, (evtId, arg1, arg2, obj) -> {
            if (obj != null)
                gMainHandler.post(() -> ToastUtil.show((String) obj, true));
            gMainHandler.postDelayed(MyApplication::exit, 500);
            return true;
        });

        EventPub.getDefaultPub().subscribe(Events.STREAM_STATE_CHANGED , TAG, (evtId, arg1, arg2, obj) -> {
            StreamState state = (StreamState) obj;
            String message = String.format("???????????????: %s, %s, %s",
                    state.channelId.name(), state.type.name, state.ready);

            gMainHandler.postDelayed(() -> ToastUtil.show(message, false), 50);
            return false;
        });
    }

    private static void releaseEvent() {
        EventPub.getDefaultPub().unsubscribe(BaseEvents.USER_HINT, TAG);
        EventPub.getDefaultPub().unsubscribe(BaseEvents.RESTART_APP, TAG);
        EventPub.getDefaultPub().unsubscribe(Events.STREAM_STATE_CHANGED, TAG);
        EventPub.getDefaultPub().syncPending();
    }
}
