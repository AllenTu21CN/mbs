package cn.lx.mbs.impl.structures;

// 直播录制启停策略
public enum LRSSStrategy {
    Forbidden("禁用"),
    Always("总是打开"),
    Manual("手动启停"),
    InCalling("呼叫时启动");

    public final String desc;

    LRSSStrategy(String desc) {
        this.desc = desc;
    }
}
