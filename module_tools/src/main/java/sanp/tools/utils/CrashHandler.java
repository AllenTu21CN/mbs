package sanp.tools.utils;

import android.app.Activity;
import android.os.Looper;

/**
 * Created by Tom on 2017/5/3.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler";
    private static CrashHandler INSTANCE = new CrashHandler();
    private Activity mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init(Activity ctx) {
        mContext = ctx;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable ex) {
        LogManager.e(String.format("-----异常捕获[msg:%s]:", ex.getMessage()));
        ex.printStackTrace();
        mContext.finish();
        System.exit(0);
    }
}