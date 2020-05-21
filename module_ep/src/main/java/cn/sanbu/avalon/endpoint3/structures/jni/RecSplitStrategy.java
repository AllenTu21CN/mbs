package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

public class RecSplitStrategy {

    public RecSplitMode mode;
    public long value; //sec or byte

    public RecSplitStrategy(RecSplitMode mode, long value) {
        this.mode = mode;
        this.value = value;
    }

    public RecSplitStrategy(RecSplitStrategy other) {
        mode = other.mode;
        value = other.value;
    }

    public boolean isEqual(RecSplitStrategy other) {
        return other != null && CompareHelper.isEqual(mode, other.mode) &&
                value == other.value;
    }

    public boolean isValid() {
        if (mode == null)
            return false;
        if (mode != RecSplitMode.Single && value <= 0)
            return false;
        return true;
    }
}
