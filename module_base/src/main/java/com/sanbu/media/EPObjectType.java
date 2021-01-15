package com.sanbu.media;

public enum EPObjectType {
    Stream(0),
    Display(1),
    Mixer(2),
    Source(3),
    Caller(4),
    Output(5);

    public final int id;

    EPObjectType(int id) {
        this.id = id;
    }

    public static EPObjectType fromId(int id) {
        for (EPObjectType input: EPObjectType.values()) {
            if (input.id == id)
                return input;
        }
        return null;
    }
}