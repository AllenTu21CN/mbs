package cn.sanbu.avalon.endpoint3.structures.jni;

public class Reconnecting {
    public long count;           // count of retrying, -1 means keep reconnecting any more
    public long min_interval_ms; // min interval of retrying in millisecond
    public long max_interval_ms; // max interval
    public long interval_step_ms;

    public Reconnecting(long count, long min_interval_ms, long max_interval_ms, long interval_step_ms) {
        this.count = count;
        this.min_interval_ms = min_interval_ms;
        this.max_interval_ms = max_interval_ms;
        this.interval_step_ms = interval_step_ms;
    }

    public Reconnecting(Reconnecting other) {
        this(other.count, other.min_interval_ms,
                other.max_interval_ms, other.interval_step_ms);
    }
}
