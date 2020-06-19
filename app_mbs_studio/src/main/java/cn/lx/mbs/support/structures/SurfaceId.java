package cn.lx.mbs.support.structures;

public enum SurfaceId {
    PGM("输出"),
    PVW("预监"),
    IN1("输入#1"),
    IN2("输入#2"),
    IN3("输入#3"),
    IN4("输入#4");

    public final String desc;

    SurfaceId(String desc) {
        this.desc = desc;
    }
}
