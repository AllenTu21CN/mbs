package cn.sanbu.avalon.endpoint3.structures.jni;

public enum DataType {
    UNKNOWN("unknown", 0, false),
    AUDIO("audio", 1, false),
    VIDEO("video", 2, true),
    VIDEO_EXT("video_ext", 3, true),
    DATA("data", 4, false),
    APPLICATION("application", 5, false),
    SUBTITLE("subtitle", 6, false),
    EXTENSION("extension", 7, false);

    public final String name;
    public final int value;
    public final boolean isVideo;

    DataType(String name, int value, boolean isVideo) {
        this.name = name;
        this.value = value;
        this.isVideo = isVideo;
    }

    public static DataType fromValue(int value) {
        for (DataType type : DataType.values()) {
            if (value == type.value)
                return type;
        }
        return UNKNOWN;
    }

    public static DataType fromName(String name) {
        if (name == null)
            return UNKNOWN;
        for (DataType type : DataType.values()) {
            if (name.equals(type.name))
                return type;
        }
        return UNKNOWN;
    }
}
