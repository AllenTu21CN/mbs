package cn.lx.mbs.impl.structures;

// 业务场景
public enum BIZScene {
    NormalHD("常规互动课"),
    NormalMeeting("常规视频会议");

    public final String desc;

    BIZScene(String desc) {
        this.desc = desc;
    }
}
