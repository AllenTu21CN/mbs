package sanp.mp100.utils;

import java.io.IOException;

import sanp.tools.utils.ExecutorThreadUtil;
import sanp.tools.utils.LogManager;

/**
 * Created by zhangxd on 2017/7/25.
 */

public class PingNetUtil {

    private static final String TAG = "PingNetUtil";

    private PingIpCallback mCallback;

    private static PingNetUtil instance;

    public static PingNetUtil getInstance() {
        if (instance == null) {
            instance = new PingNetUtil();
        }
        return instance;
    }

    public void setPingCallback(PingIpCallback callback) {
        mCallback = callback;
    }

    public void pingIpConnect(final String ipString) {
        ExecutorThreadUtil.submit(new PingIpRunnable(ipString));
    }

    public interface PingIpCallback {
        void onPingSuccess();

        void onPingFail();
    }

    class PingIpRunnable implements Runnable {
        private String ipString;

        PingIpRunnable(String ipStr) {
            ipString = ipStr;
        }

        @Override
        public void run() {
            try {
                Process p = Runtime.getRuntime().exec("ping -c 1 -w 5 " + ipString);
                int status = p.waitFor();
                if (status == 0) {
                    mCallback.onPingSuccess();
                } else {
                    mCallback.onPingFail();
                }
            } catch (IOException e) {
                LogManager.e(TAG, e);
            } catch (InterruptedException e) {
                LogManager.e(TAG, e);
            }
        }
    }
}
