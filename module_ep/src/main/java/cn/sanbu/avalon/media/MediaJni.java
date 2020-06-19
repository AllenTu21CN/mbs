package cn.sanbu.avalon.media;

import android.content.Context;

import com.sanbu.board.BoardSupportClient;

/**
 * Created by huangzy on 2018/8/2.
 */

public class MediaJni {

    public static final String EP_ANDROID_LIB = "ep_android";
    public static final String EP_LEGACY_ANDROID_LIB = "ep_android_box_legacy";

    private static boolean gInited = false;
    private static boolean gEnableVENDK = false;
    private static VideoEngine gVideoEngine = null;

    public static void init(Context context, BoardSupportClient client) {
        init(context, EP_ANDROID_LIB, client);
    }

    public static void init(Context context, final String epLib, BoardSupportClient client) {
        synchronized (MediaJni.class) {
            if (!gInited) {
                CameraHelper.init(context, client);
                gVideoEngine = VideoEngine.allocateInstance(context, gEnableVENDK);
                System.loadLibrary(epLib);
                jniEnvInit(gVideoEngine.getDisplayRefreshRate(), gEnableVENDK);
                gInited = true;
            }
        }
    }

    public static void initEnv(Context context, BoardSupportClient client, boolean showNoSignal) {
        synchronized (MediaJni.class) {
            if (!gInited) {
                CameraHelper.init(context, client);
                gVideoEngine = VideoEngine.allocateInstance(context, gEnableVENDK, showNoSignal);
                jniEnvInit(gVideoEngine.getDisplayRefreshRate(), gEnableVENDK);
                gInited = true;
            }
        }
    }

    public static boolean isInited() {
        return gInited;
    }

    public static VideoEngine getVideoEngine() {
        return gVideoEngine;
    }

    public static void enableVENDK(boolean enable) {
        gEnableVENDK = enable;
    }

    private static native int jniEnvInit(float refreshRate, boolean enable_ndk);

    protected static native int jniOnError(String tag, String msg);
}
