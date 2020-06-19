package cn.sanbu.avalon.endpoint3.structures.jni;

import cn.sanbu.avalon.media.MediaEventId;

public enum EPEvent {
    RecvReqOpenVideoExt(0),
    FileWrittenCompleted(1),
    SourceDecodingStateChanged(MediaEventId.SOURCE_DECODING_STATE_CHANGED.value * -1);

    public final int value;

    EPEvent(int value) {
        this.value = value;
    }

    public static EPEvent fromValue(final int value) {
        for (EPEvent event: EPEvent.values()) {
            if (event.value == value)
                return event;
        }
        return null;
    }
}
