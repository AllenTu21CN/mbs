package cn.lx.mbs.support.structures;

public enum MixMode {
    Off("总是关闭"),
    On("总是打开"),
    AFV("跟随视频");

    public final String desc;

    MixMode(String desc) {
        this.desc = desc;
    }
}
