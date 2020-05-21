package cn.sanbu.avalon.endpoint3.structures.jni;

public enum DataType {
    UNKNOWN("unknown",          0),
    AUDIO("audio",              1),
    VIDEO("video",              2),
    VIDEO_EXT("video_ext",      3),
    DATA("data",                4),
    APPLICATION("application",  5),
    SUBTITLE("subtitle",        6),
    EXTENSION("extension",      7);

    public final String name;
    public final int value;

    DataType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static DataType fromValue(int value) {
        for (DataType type: DataType.values()) {
            if (value == type.value)
                return type;
        }
        return UNKNOWN;
    }

    public static DataType fromName(String name) {
        if (name == null)
            return UNKNOWN;
        for (DataType type: DataType.values()) {
            if (name.equals(type.name))
                return type;
        }
        return UNKNOWN;
    }
}
