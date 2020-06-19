package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

public class TransitionDesc {

    public final TransitionMode type;   // File name (without suffix)
    public final float duration;        // seconds

    public TransitionDesc(TransitionMode type, float duration) {
        this.type = type;
        this.duration = duration;
    }

    public boolean isEqual(TransitionDesc other) {
        if (other == null)
            return false;
        return (CompareHelper.isEqual(type, other.type) &&
                duration == other.duration);
    }

    public static TransitionDesc buildEmpty() {
        return new TransitionDesc(TransitionMode.Unknown, 0.0f);
    }
}
