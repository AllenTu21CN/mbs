package com.sanbu.media;

// 直播录制状态
public enum LRState {
    Start(1, "开始"),
    Pause(-1, "暂停"),
    Stop(0, "停止");

    public final int value;
    public final String desc;

    LRState(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static LRState fromValue(int value) {
        for (LRState state : values()) {
            if (state.value == value)
                return state;
        }
        return Stop;
    }
}
