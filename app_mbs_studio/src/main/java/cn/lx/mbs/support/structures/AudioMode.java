package cn.lx.mbs.support.structures;

public enum AudioMode {
    Off("总是关闭"),
    On("总是打开"),
    AFV("跟随视频");

    public final String desc;

    AudioMode(String desc) {
        this.desc = desc;
    }
}
