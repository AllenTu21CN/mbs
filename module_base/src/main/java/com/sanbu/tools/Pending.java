package com.sanbu.tools;

import android.os.Handler;

import com.sanbu.base.BaseError;

public class Pending {

    private static final long SYNC_TIMEOUT_MS = 2000;

    public static int sync(Handler handler) {
        return sync(handler, SYNC_TIMEOUT_MS);
    }

    public static int sync(Handler handler, long timeoutMS) {
        if (handler == null)
            return BaseError.ACTION_ILLEGAL;
        if (handler.getLooper().getThread() == Thread.currentThread())
            return BaseError.ACTION_CANCELED;

        final AsyncResult result = new AsyncResult();
        handler.post(new Runnable() {
            @Override
            public void run() {
                result.notify2(BaseError.SUCCESS);
            }
        });
        int ret = (int) result.wait2(timeoutMS, BaseError.ACTION_TIMEOUT);
        if (ret != BaseError.SUCCESS)
            LogUtil.w("!!! Pending.sync timeout");
        return ret;
    }

    public static int clean(Handler handler) {
        return clean(handler, null);
    }

    public static int clean(Handler handler, Object token) {
        if (handler == null)
            return BaseError.ACTION_ILLEGAL;

        handler.removeCallbacksAndMessages(token);

        if (handler.getLooper().getThread() == Thread.currentThread())
            return BaseError.SUCCESS;

        return sync(handler);
    }
}
