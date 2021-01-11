package com.sanbu.media;

public enum Bandwidth {
    _32K(32000, "32K", 35200, 29000),
    _48K(48000, "48K", 52800, 43600),
    _64K(64000, "64K", 70400, 58100),
    _96K(96000, "96K", 105600, 87200),
    _128K(128000, "128K", 140800, 116000),
    _192K(192000, "192K", 211200, 174000),
    _256K(256000, "256K", 281600, 232000),
    _384K(384000, "384K", 422400, 349000),
    _512K(512000, "512K", 563200, 465000),
    _768K(768000, "768K", 844800, 698000),
    _1M(1024000, "1M", 1126400, 930000),
    _1dot5M(1536000, "1.5M", 1689600, 1396000),
    _2M(2048000, "2M", 2252800, 1861000),
    _3M(3072000, "3M", 3379200, 2792000),
    _4M(4096000, "4M", 4505600, 3723000),
    _6M(6144000, "6M", 6758400, 5585000),
    _8M(8192000, "8M", 9011200, 7447000),
    _16M(16384000, "16M", 18022400, 14894000),
    Unknown(0, "unknown", 0, 0);

    public final int bps;
    public final String name;
    public final int bpsMaxApprox;
    public final int bpsMinApprox;

    Bandwidth(int bps, String name, int bpsMaxApprox, int bpsMinApprox) {
        this.bps = bps;
        this.name = name;
        this.bpsMaxApprox = bpsMaxApprox;
        this.bpsMinApprox = bpsMinApprox;
    }

    public static Bandwidth fromValue(int bps) {
        for (Bandwidth bandwidth: Bandwidth.values()) {
            if (bps == bandwidth.bps)
                return bandwidth;
        }
        return Unknown;
    }

    public static Bandwidth fromName(String name) {
        if (name == null)
            return Unknown;
        name = name.toUpperCase();

        for (Bandwidth bandwidth: Bandwidth.values()) {
            if (name.equals(bandwidth.name))
                return bandwidth;
        }
        return Unknown;
    }

    public static Bandwidth fromValueApprox(int bps) {
        for (Bandwidth bandwidth: Bandwidth.values()) {
            if (bps <= bandwidth.bpsMaxApprox)
                return bandwidth;
        }
        return Unknown;
    }
}
