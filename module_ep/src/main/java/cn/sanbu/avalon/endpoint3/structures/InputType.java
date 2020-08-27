package cn.sanbu.avalon.endpoint3.structures;

public enum InputType {
    VideoCapture("device://video/"),
    AudioCapture("device://audio/"),

    RTSP("rtsp://"),
    RTMP("rtmp://"),
    RMSP("rmsp://"),

    File("file://"),

    Caller(null, "h323:", "sip:", "sbs:");

    public final String prefix;
    public final String[] optional;

    InputType(String prefix, String... optional) {
        this.prefix = prefix;
        this.optional = optional;
    }

    public static InputType fromUrl(String url) {
        if (url == null)
            return null;
        for (InputType type: values()) {
            if (type.prefix != null && url.startsWith(type.prefix))
                return type;
            if (type.optional != null) {
                for (String op: type.optional) {
                    if (url.startsWith(op))
                        return type;
                }
            }
        }
        return null;
    }
}
