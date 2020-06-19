package cn.lx.mbs.support.structures;

public enum SRState {
    Start("开始"),
    Pause("暂停"),
    Stop("停止");

    public final String desc;

    SRState(String desc) {
        this.desc = desc;
    }
}
