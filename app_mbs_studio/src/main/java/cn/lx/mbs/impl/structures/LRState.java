package cn.lx.mbs.impl.structures;

// 直播录制状态
public enum LRState {
    Start("开始"),
    Pause("暂停"),
    Stop("停止");

    public final String desc;

    LRState(String desc) {
        this.desc = desc;
    }
}
