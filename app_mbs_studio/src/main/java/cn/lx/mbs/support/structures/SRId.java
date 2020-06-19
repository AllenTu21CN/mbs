package cn.lx.mbs.support.structures;

public enum SRId {
    Streaming("推流"),
    Recording("录制");

    public final String desc;

    SRId(String desc) {
        this.desc = desc;
    }
}
