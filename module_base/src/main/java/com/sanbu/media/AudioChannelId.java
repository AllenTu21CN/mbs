package com.sanbu.media;

public enum AudioChannelId {
    Mic("本地采集", true),
    CallingRx("互动接收", true),

    Speaker("本地播放", false),
    LR("直播录制", false),
    CallingTx("互动发送", false);

    public final String desc;
    public final boolean isInput;

    AudioChannelId(String desc, boolean isInput) {
        this.desc = desc;
        this.isInput = isInput;
    }
}
