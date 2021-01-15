package com.sanbu.network;

public enum TransProtocol {
    UNSPECIFIED("unspecified", 0),

    TCP("TCP", 1),
    UDP("UDP", 2),

    RTP_UDP("RTP/AVP/UDP", 3),
    RTP_TCP("RTP/AVP/TCP", 4),
    RTP(RTP_UDP.name, RTP_UDP.value);

    public final String name;
    public final int value;

    TransProtocol(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static TransProtocol fromValue(int value) {
        for (TransProtocol protocol: TransProtocol.values()) {
            if (value == protocol.value)
                return protocol;
        }
        return UNSPECIFIED;
    }

    public static TransProtocol fromName(String name) {
        if (name == null)
            return UNSPECIFIED;
        for (TransProtocol protocol: TransProtocol.values()) {
            if (name.equals(protocol.name))
                return protocol;
        }
        return UNSPECIFIED;
    }
}
