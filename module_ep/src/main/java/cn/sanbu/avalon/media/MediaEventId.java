package cn.sanbu.avalon.media;

public enum MediaEventId {
    UNKNOWN(0),
    SOURCE_DECODING_STATE_CHANGED(1),
    AUDIO_VOLUME_REPORT(2);

    public final int value;

    MediaEventId(int value) {
        this.value = value;
    }

    public static MediaEventId fromValue(int value) {
        for (MediaEventId id: values()) {
            if (id.value == value)
                return id;
        }
        return UNKNOWN;
    }
}
