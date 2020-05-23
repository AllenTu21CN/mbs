package cn.lx.mbs.impl.structures;

public enum TSMode {
    None("N/A", false, 0, 0, "未授权"),

    // TS3X00("TS3X00_HDJX", true, 2, 4, "单点互动"),
    // TS5X00("TS5X00_HDJX", true, 2, 6, "单点互动");

    TS3200("TS3200_HDJX", true, 7, 4, "多点互动"),
    TS5200("TS5200_HDJX", true, 7, 6, "多点互动");

    public final String code;       // 模式码
    public final boolean granted;   // (对于互动功能)是否授权
    public final int maxHDCount;    // 最大互动点数
    public final int maxRoleCount;  // 最大角色数
    public final String desc;       // 描述

    TSMode(String code, boolean granted, int maxHDCount, int maxRoleCount, String desc) {
        this.code = code;
        this.granted = granted;
        this.maxHDCount = maxHDCount;
        this.maxRoleCount = maxRoleCount;
        this.desc = desc;
    }

    public static TSMode fromCode(String code) {
        if (code == null)
            return None;

        for (TSMode mode: values()) {
            if (mode.code.equals(code))
                return mode;
        }
        return None;
    }
}
