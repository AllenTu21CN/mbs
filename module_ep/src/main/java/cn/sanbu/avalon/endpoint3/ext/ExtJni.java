package cn.sanbu.avalon.endpoint3.ext;

public class ExtJni {

    private static boolean gInited = false;

    public static void initEnv() {
        synchronized (ExtJni.class) {
            if (gInited)
                return;

            // load jni libraries
            System.loadLibrary("ext_control");

            // init jni environment
            jniEnvInit();

            // flag inited
            gInited = true;
        }
    }

    public static boolean isInited() {
        return gInited;
    }

    // Inits jni environment
    // NOTE: ONLY this method is called ONCE in single instance constuctor function.
    private static native int jniEnvInit();
}
