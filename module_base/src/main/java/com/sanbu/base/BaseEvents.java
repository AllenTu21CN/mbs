package com.sanbu.base;

import com.sanbu.tools.EventPub;

public class BaseEvents {

    //////////////////// events

    public static final int END = 0x0ff;

    // 应用层提示
    // params: arg1=important[1:true 0:false] obj=message
    public static final int USER_HINT = 0x001;

    // 应用重启
    // params: obj=message(reason)
    public static final int RESTART_APP = 0x002;

    public static EventPub.Event buildEvt4UserHint(String message, boolean important) {
        return new EventPub.Event(USER_HINT, important ? 1 : 0, -1, message);
    }

    public static EventPub.Event buildEvt4RestartAPP() {
        return new EventPub.Event(RESTART_APP);
    }

    public static EventPub.Event buildEvt4RestartAPP(String reason) {
        return new EventPub.Event(RESTART_APP, -1, -1, reason);
    }
}
