package cn.sanbu.avalon.endpoint3.structures;

public enum SourceType {
    Capture("device://"),
    RTSP("rtsp://");

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
