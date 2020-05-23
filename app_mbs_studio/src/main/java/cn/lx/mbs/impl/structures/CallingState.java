package cn.lx.mbs.impl.structures;

public enum CallingState {
    // None(0),
    Idle(1),
    Calling(2),
    Established(3);

    public final int value;

    CallingState(int value) {
        this.value = value;
    }

    public static CallingState fromValue(int value) {
        for (CallingState state: CallingState.values()) {
            if (value == state.value)
                return state;
        }
        return Idle;
    }
}
