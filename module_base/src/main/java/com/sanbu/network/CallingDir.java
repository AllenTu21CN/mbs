package com.sanbu.network;

public enum CallingDir {
    None(0, "none"),
    Outgoing(1, "tx"),
    Incoming(2, "rx");

    public final int value;
    public final String name;

    CallingDir(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static CallingDir fromName(final String name) {
        if (name == null)
            return null;
        for (CallingDir dir: CallingDir.values()) {
            if (name.equals(dir.name))
                return dir;
        }
        return null;
    }

    public static CallingDir fromValue(int value) {
        for (CallingDir dir : values()) {
            if (dir.value == value)
                return dir;
        }
        return None;
    }
}
