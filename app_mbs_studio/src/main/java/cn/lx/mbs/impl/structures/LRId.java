package cn.lx.mbs.impl.structures;

public enum LRId {
    LocalLive(0, "本地直播"),
    LocalRecording(1, "本地录制"),
    FixedPushing(2, "第三方推流"),
    IntegratedPushing(3, "平台推流"),
    LiveAndPushing(4, "直播推送");  // 本地直播+平台推送

    public final int id;
    public final String desc;

    LRId(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }
}
