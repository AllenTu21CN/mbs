package cn.sanbu.avalon.endpoint3.structures.jni;

public enum RecSplitMode {
    Single("不分割"),
    ByDuration("按时间分割(秒)"),
    BySize("按大小分割(字节)");

    public final String desc;

    RecSplitMode(String desc) {
        this.desc = desc;
    }
}
