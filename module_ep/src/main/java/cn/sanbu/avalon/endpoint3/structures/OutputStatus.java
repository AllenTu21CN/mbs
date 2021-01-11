package cn.sanbu.avalon.endpoint3.structures;

public enum OutputStatus {
    OK(0, "文件就绪", "已连接"),
    Failed(-5, "文件打开失败", "连接失败;重连中"),
    Lost(-6, "文件丢失", "连接丢失;重连中"),
    Over(-7, "文件关闭", "连接失败;已放弃"),
    Unknown(-99, "未知", "未知");

    public final int code;
    public final String desc4File;
    public final String desc4Net;

    OutputStatus(int code, String desc4File, String desc4Net) {
        this.code = code;
        this.desc4File = desc4File;
        this.desc4Net = desc4Net;
    }

    public static OutputStatus fromCode(int code) {
        for (OutputStatus status : OutputStatus.values()) {
            if (code == status.code)
                return status;
        }
        return Unknown;
    }
}
