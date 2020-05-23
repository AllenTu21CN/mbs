package cn.lx.mbs.impl.structures;

public enum DisplayId {
    LocalMain("本地主显"),
    LocalSecond("本地扩展"),
    LR("直播录制"),
    TX("互动发送");

    public final String desc;

    DisplayId(String desc) {
        this.desc = desc;
    }
}
