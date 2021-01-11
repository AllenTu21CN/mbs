package com.sanbu.media;

public enum Alignment {
    UNDEFINED(-1, "N/A", "N/A"),
    POS0(0, "left", "top"),
    POS1(1, "hcenter", "top"),
    POS2(2, "right", "top"),
    POS3(3, "left", "vcenter"),
    POS4(4, "hcenter", "vcenter"),
    POS5(5, "right", "vcenter"),
    POS6(6, "left", "bottom"),
    POS7(7, "hcenter", "bottom"),
    POS8(8, "right", "bottom");

    public final int index;
    public final String horizontal;
    public final String vertical;

    Alignment(int index, String horizontal, String vertical) {
        this.index = index;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public String toString() {
        return horizontal + ":" + vertical;
    }

    public static Alignment fromString(String alignment) {
        if (alignment == null)
            return UNDEFINED;

        String[] ps = alignment.split(":");
        if (ps.length != 2)
            return null;

        for (Alignment alig: Alignment.values()) {
            if (alig.horizontal.equals(ps[0]) && alig.vertical.equals(ps[1]))
                return alig;
        }
        return UNDEFINED;
    }

    public static Alignment fromIndex(int index) {
        for (Alignment alignment: Alignment.values()) {
            if (index == alignment.index)
                return alignment;
        }
        return UNDEFINED;
    }
}
