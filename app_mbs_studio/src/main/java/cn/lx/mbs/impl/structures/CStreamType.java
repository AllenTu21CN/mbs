package cn.lx.mbs.impl.structures;

// 视频会议流类型
public enum CStreamType {
    VideoMain("主流"),
    VideoExt("辅流");

    public final String desc;

    CStreamType(String desc) {
        this.desc = desc;
    }
}
