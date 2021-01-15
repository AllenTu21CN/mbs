package com.sanbu.media;

// 音频输入使用策略
public enum AudioRxStrategy {
    Off("总是关闭"),
    On("总是打开"),
    AFV("跟随视频");

    public final String desc;

    AudioRxStrategy(String desc) {
        this.desc = desc;
    }
}
