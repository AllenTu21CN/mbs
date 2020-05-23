package cn.lx.mbs.impl.structures;

// 导播模式
public enum DirectingType {
    Local("本地显示"),
    LR("直播录制"),
    TX("互动发送");

    public final String name;

    DirectingType(String name) {
        this.name = name;
    }
}
