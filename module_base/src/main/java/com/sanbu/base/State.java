package com.sanbu.base;

public enum State {
    None(0),
    Doing(-1),
    Done(1);

    public final int value;

    State(int value) {
        this.value = value;
    }

    public static State fromValue(int value) {
        for (State state : values()) {
            if (state.value == value)
                return state;
        }
        return None;
    }
}
