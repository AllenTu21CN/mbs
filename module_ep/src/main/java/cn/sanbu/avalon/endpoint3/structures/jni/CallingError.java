package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.network.CallingProtocol;
import com.sanbu.tools.CompareHelper;

public enum CallingError {
    SIP_SC_OK(CallingProtocol.SIP, -114, "SIP_SC_OK", "成功", false),
    SIP_SC_OK2(CallingProtocol.SIP, 0, "SIP_SC_OK", "成功", false),
    SIP_SC_FORBIDDEN(CallingProtocol.SIP, -114, "SIP_SC_FORBIDDEN", "拒绝", false),
    SIP_SC_REQUEST_TIMEOUT(CallingProtocol.SIP, -114, "SIP_SC_REQUEST_TIMEOUT(SIP_SC_TSX_TIMEOUT)", "呼叫超时", true),
    SIP_SC_INTERNAL_SERVER_ERROR(CallingProtocol.SIP, -114, "SIP_SC_INTERNAL_SERVER_ERROR", "地址错误", false),

    H323_OK(CallingProtocol.H323, 0, "Local endpoint application cleared call", "本地挂断", false),
    H323_OK2(CallingProtocol.H323, 0, "Remote endpoint application cleared call", "远程挂断", false),
    H323_REFUSED(CallingProtocol.H323, -114, "Remote endpoint refused call", "被拒绝", false),
    H323_REFUSING(CallingProtocol.H323, -114, "Local endpoint declined to answer call", "拒绝远端", false),
    H323_PORT_UNREACHABLE(CallingProtocol.H323, -114, "The remote party is not running an endpoint", "端口不通", true),
    H323_NETWORK_UNREACHABLE(CallingProtocol.H323, -102, "The remote party host off line", "网络不可达", true),
    H323_INVALID_ADDRESS(CallingProtocol.H323, -103, "Transport connection failed to establish call", "地址错误", false),

    UNKNOWN(CallingProtocol.Unknown, -1, "unknown", "未知的错误", true);

    public final CallingProtocol protocol;
    public final int code;
    public final String reason;
    public final String hint;
    public final boolean retry;

    CallingError(CallingProtocol protocol, int code, String reason, String hint, boolean retry) {
        this.protocol = protocol;
        this.code = code;
        this.reason = reason;
        this.hint = hint;
        this.retry = retry;
    }

    public static CallingError fromCode(int code) {
        for (CallingError error: CallingError.values()) {
            if (code == error.code)
                return error;
        }
        return UNKNOWN;
    }

    public static CallingError fromCode2(CallingProtocol protocol, int code, String reason) {
        for (CallingError error: CallingError.values()) {
            if (code == error.code &&
                    CompareHelper.isEqual(protocol, error.protocol) &&
                    CompareHelper.isEqual(reason, error.reason))
                return error;
        }
        return UNKNOWN;
    }
}
