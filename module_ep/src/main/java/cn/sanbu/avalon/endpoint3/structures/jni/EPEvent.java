package cn.sanbu.avalon.endpoint3.structures.jni;

public enum EPEvent {
    Unknown(-999),
    RecvReqOpenVideoExt(0),
    FileWrittenCompleted(1),
    OutputStatusChanged(2);

    public final int value;

    EPEvent(int value) {
        this.value = value;
    }

    public static EPEvent fromValue(final int value) {
        for (EPEvent event: EPEvent.values()) {
            if (event.value == value)
                return event;
        }
        return Unknown;
    }
}
