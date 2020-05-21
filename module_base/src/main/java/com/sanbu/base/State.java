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
        if (value == None.value)
            return None;
        else if (value == Doing.value)
            return Doing;
        else if (value == Done.value)
            return Done;
        else
            return None;
    }
}
