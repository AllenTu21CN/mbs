package cn.lx.media;

import android.content.Context;

/**
 * Created by huangzy on 2018/8/2.
 */

public class MediaJni {

    public static final String EP_ANDROID_LIB = "ep_android";
    public static final String EP_LEGACY_ANDROID_LIB = "ep_android_legacy";
    public static final String EP_DLXX_ANDROID_LIB = "ep_android_dlxx";

    private static boolean gInited = false;
    private static VideoEngine gVideoEngine = null;

    public static void init(Context context) {
        init(context, EP_ANDROID_LIB);
    }

    public static void init(Context context, final String epLib) {
        synchronized (MediaJni.class) {
            if (!gInited) {
                gVideoEngine = VideoEngine.allocateInstance(context);
                System.loadLibrary(epLib);
                jniEnvInit();
                gInited = true;
            }
        }
    }

    public static void initEnv(Context context) {
        synchronized (MediaJni.class) {
            if (!gInited) {
                gVideoEngine = VideoEngine.allocateInstance(context);
                jniEnvInit();
                gInited = true;
            }
        }
    }

    public static VideoEngine getVideoEngine() {
        return gVideoEngine;
    }

    private static native int jniEnvInit();

    protected static native int jniOnError(String tag, String msg);
}
