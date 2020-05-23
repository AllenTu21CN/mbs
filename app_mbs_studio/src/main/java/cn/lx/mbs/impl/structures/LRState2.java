package cn.lx.mbs.impl.structures;

public class LRState2 {
    public LRState state;
    public long durationTimeMs;

    public LRState2(LRState state, long durationTimeMs) {
        this.state = state;
        this.durationTimeMs = durationTimeMs;
    }
}
