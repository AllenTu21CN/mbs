package com.sanbu.network;

public enum CallingProtocol {
    Unknown(0, "unknown", "n/a:"),
    H323(1, "h323", "h323:"),
    SIP(2, "sip", "sip:"),
    SBS(3, "sbs", "sbs:");

    public final int value;
    public final String name;
    public final String prefix;

    CallingProtocol(int value, String name, String prefix) {
        this.name = name;
        this.value = value;
        this.prefix = prefix;
    }

    public static CallingProtocol fromValue(int value) {
        for (CallingProtocol protocol : CallingProtocol.values()) {
            if (protocol.value == value)
                return protocol;
        }
        return Unknown;
    }

    public static CallingProtocol fromName(String name) {
        if (name == null)
            return Unknown;
        name = name.toLowerCase();
        for (CallingProtocol protocol : CallingProtocol.values()) {
            if (name.equals(protocol.name))
                return protocol;
        }
        return Unknown;
    }

    public static CallingProtocol fromUrl(String url) {
        if (url == null)
            return Unknown;
        url = url.toLowerCase();
        for (CallingProtocol protocol : CallingProtocol.values()) {
            if (url.startsWith(protocol.prefix))
                return protocol;
        }
        return Unknown;
    }
}
