package com.sanbu.tools;

public class AsyncResult {

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
}
