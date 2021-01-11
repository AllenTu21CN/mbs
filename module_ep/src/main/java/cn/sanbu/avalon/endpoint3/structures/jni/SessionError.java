package cn.sanbu.avalon.endpoint3.structures.jni;

public enum SessionError {
    NO_ERROR(0, "ok", "成功"),
    ERR_INVALID_PARAMS(-1, "invalid_params", "无效的参数"),
    ERR_LOGICAL(-2, "logical_error", "逻辑错误"),
    ERR_SYS_FAILED(-3, "sys_error", "系统错误"),
    ERR_INVALID_PEER(-4, "invalid_peer", "无效的远端"),
    ERR_CONNECT_FAILED(-5, "connect_failed", "连接失败"),
    ERR_LOST_CONNECTION(-6, "lost_connection", "连接丢失"),
    ERR_RECONNECT_EXIT(-7, "reconnect_exit", "重连失败"),
    UNKNOWN(-999, "unknown", "未知的错误");

    public final int code;
    public final String name;
    public final String desc;

    SessionError(int code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public static SessionError fromCode(int code) {
        for (SessionError error: SessionError.values()) {
            if (code == error.code)
                return error;
        }
        return UNKNOWN;
    }

    public static SessionError fromName(String name) {
        if (name == null)
            return UNKNOWN;

        for (SessionError error: SessionError.values()) {
            if (error.name.equals(name))
                return error;
        }
        return UNKNOWN;
    }
}
