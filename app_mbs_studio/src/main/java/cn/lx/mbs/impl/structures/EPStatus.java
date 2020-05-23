package cn.lx.mbs.impl.structures;

import com.sanbu.base.State;

import java.util.HashMap;
import java.util.Map;

public class EPStatus {
    // EP互动状态
    public EPState epState;
    // EP互动持续时间(毫秒)
    public long epDurationMS;
    // EP互动讨论状态
    public boolean epInDiscussion;

    // GK注册状态
    public State gkState;
    // SIP注册状态
    public State sipState;

    // 跟踪开关状态
    public boolean trackingOnOff;

    // 直播录制状态
    public Map<LRId, LRState2> lrState;

    // 导播模式
    public Map<DirectingType, DirectingMode> dirModes;

    public static EPStatus buildIdle() {
        EPStatus status = new EPStatus();
        status.epState = EPState.Idle;
        status.epDurationMS = 0;
        status.epInDiscussion = false;
        status.gkState = State.None;
        status.sipState = State.None;
        status.trackingOnOff = false;
        status.lrState = new HashMap<>();
        status.dirModes = new HashMap<>();
        return status;
    }
}
