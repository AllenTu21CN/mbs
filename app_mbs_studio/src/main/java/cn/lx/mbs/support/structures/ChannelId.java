package cn.lx.mbs.support.structures;

public enum ChannelId {
    // for streaming and recording
    Output(false, false),

    MIC0(true, true),
    IN1(true, false),
    IN2(true, false),
    IN3(true, false),
    IN4(true, false);

    public final boolean isRx;
    public final boolean onlyAudio;

    ChannelId(boolean isRx, boolean onlyAudio) {
        this.isRx = isRx;
        this.onlyAudio = onlyAudio;
    }
}
