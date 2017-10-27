package sanp.mp100;

import android.app.Application;

import sanp.mp100.integration.BusinessPlatform;


/**
 * Created by Tuyj on 2017/10/27.
 */

public class MP100Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(){
            @Override
            public void run() {
                initApplication();
            }
        }.start();
    }

    private static Object gLock = new Object();
    private static boolean gInited = false;

    private void initApplication() {
        synchronized (gLock) {
            if(!gInited) {
                if(BusinessPlatform.getInstance().init(this) != 0) {
                    throw new RuntimeException("BusinessPlatform init failed");
                }
                gInited = true;
            }
            gLock.notifyAll();
        }
    }

    static public boolean isInited() {
        return gInited;
    }

    static public void waitUntilInited() throws InterruptedException {
        synchronized (gLock) {
            while(!gInited)
                gLock.wait();
        }
    }

    static public boolean waitInited(long timeoutMs) throws InterruptedException {
        synchronized (gLock) {
            if(!gInited && timeoutMs > 0)
                gLock.wait(timeoutMs);
            return gInited;
        }
    }
}
