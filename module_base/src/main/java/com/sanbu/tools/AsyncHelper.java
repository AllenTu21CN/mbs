package com.sanbu.tools;

import android.os.Handler;

import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Runnable3;

public class AsyncHelper {

    private Object lock = new Object();
    private boolean got = false;
    private Object value = null;

    public void reset() {
        synchronized (lock) {
            got = false;
            value = null;
        }
    }

    // different with Object notify
    public void notify2(Object value) {
        synchronized (lock) {
            this.got = true;
            this.value = value;
            this.lock.notify();
        }
    }

    // different with Object wait
    public Object wait2(long timeoutMs) {
        return wait2(timeoutMs, null);
    }

    public Object wait2(long timeoutMs, Object failedValue) {
        long timeoutPointMs;
        if (timeoutMs > 0)
            timeoutPointMs = System.currentTimeMillis() + timeoutMs;
        else
            timeoutPointMs = -1;

        synchronized (lock) {
            while (!got) {
                try {
                    if (timeoutPointMs > 0) {
                        timeoutMs = timeoutPointMs - System.currentTimeMillis();
                        if (timeoutMs > 0)
                            lock.wait(timeoutMs);
                        else
                            return failedValue;
                    } else {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    return failedValue;
                }
            }
        }
        return value;
    }

    public interface AsyncFunc {
        void call(Callback callback);
    }

    public static Result async2sync(long timeoutMS, AsyncFunc func) {
        final AsyncHelper async = new AsyncHelper();

        func.call(new Callback() {
            @Override
            public void done(Result result) {
                async.notify2(result);
            }
        });

        return (Result) async.wait2(timeoutMS, new Result(BaseError.ACTION_TIMEOUT, "timeout"));
    }

    public static Result async2sync(long timeoutMS, Handler handler, final Runnable runnable) {
        final AsyncHelper async = new AsyncHelper();

        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    async.notify2(Result.SUCCESS);
                }
            });
        } else {
            async.notify2(new Result(BaseError.ACTION_ILLEGAL, "handler is null"));
        }

        return (Result) async.wait2(timeoutMS, new Result(BaseError.ACTION_TIMEOUT, "timeout"));
    }

    public static Result async2sync(long timeoutMS, Handler handler, final Runnable3 runnable) {
        final AsyncHelper async = new AsyncHelper();

        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Result result = runnable.run();
                    async.notify2(result);
                }
            });
        } else {
            async.notify2(new Result(BaseError.ACTION_ILLEGAL, "handler is null"));
        }

        return (Result) async.wait2(timeoutMS, new Result(BaseError.ACTION_TIMEOUT, "timeout"));
    }
}
