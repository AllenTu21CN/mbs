package cn.sanbu.avalon.endpoint3.structures;

public enum SourceType {
    VideoCapture("device://video/"),
    AudioCapture("device://audio/"),

    RTSP("rtsp://"),
    RMSP("rmsp://"),
    RTMP("rtmp://"),

    File("file://"),

    Caller("h323|sip|sbs:");

    public final String prefix;

    SourceType(String prefix) {
        this.prefix = prefix;
    }

    public static SourceType fromUrl(String url) {
        if (url == null)
            return null;
        for (SourceType type: values()) {
            if (url.startsWith(type.prefix))
                return type;
        }
        return null;
    }
}
