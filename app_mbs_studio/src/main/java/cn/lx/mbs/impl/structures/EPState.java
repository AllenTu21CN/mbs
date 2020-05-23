package cn.lx.mbs.impl.structures;

import com.sanbu.base.State;

public enum EPState {
    Idle("空闲", State.None),

    Outgoing("呼出中", State.Doing),
    Incoming("呼入中", State.Doing),

    Established("正在通话", State.Done);

    public final String desc;
    // None-空闲 Doing-建立过程中 Done-使用中
    public final State flag;

    EPState(String desc, State flag) {
        this.desc = desc;
        this.flag = flag;
    }
}
