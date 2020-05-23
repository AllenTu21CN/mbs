package cn.lx.mbs.impl.structures;

public enum AudioChannelId {
    Mic("本地采集"),
    CallingRx("互动接收"),

    Speaker("本地播放"),
    LR("直播录制"),
    CallingTx("互动发送");

    public final String desc;

    AudioChannelId(String desc) {
        this.desc = desc;
    }
}
