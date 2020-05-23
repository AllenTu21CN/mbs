package cn.lx.mbs.impl.core;

import android.os.Handler;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;

public class CoreUtils
{
    public static final String TAG = "ep_mbs";

    ///////////////// util functions

    public static void callbackSuccess(Callback callback) {
        callbackSuccess(null, callback);
    }

    public static void callbackSuccess(Handler handler, Callback callback) {
        callbackResult(handler, callback, Result.SUCCESS);
    }

    public static void callbackResult(Callback callback, Result result) {
        callbackResult(null, callback, result);
    }

    public static void callbackResult(Handler handler, Callback callback, Result result) {
        if (callback == null)
            return;
        if (handler == null)
            callback.done(result);
        else
            handler.post(() -> callback.done(result));
    }
}
