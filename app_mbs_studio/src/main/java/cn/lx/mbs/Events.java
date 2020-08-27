package cn.lx.mbs;

import com.sanbu.base.BaseEvents;

public class Events {

    private static final int START = BaseEvents.END + 1;

    //////////////////// basic module events (0x100~0x1ff)
    public static final int BASIC_START = START;

    // 可用网络信息变化
    // params: obj=NetInfo
    public static final int NETWORK_ACTIVE_INFO = BASIC_START;

    //////////////////// core module events (0x200~0x2ff)
    private static final int MBS_START = BASIC_START + 0x100;

    // 输入流状态变化
    // params: obj=StreamState
    public static final int STREAM_STATE_CHANGED = MBS_START;

    // 录制文件就绪
    // params: obj=full path
    public static final int RECORDING_FILE_IS_READY = MBS_START + 1;

    //////////////////// UI module events (0x300~0x3ff)
    private static final int UI_START = MBS_START + 0x100;

    // 推流录制开关
    // params: arg1=0/stopped;1/started;-1:paused
    public static final int SR_SWITCH_CHANGED = UI_START;
}
