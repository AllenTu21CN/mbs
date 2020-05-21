package cn.sanbu.avalon.endpoint3.structures;

public enum CallingProtocol {
    H323("h323", "h323:"),
    SIP("sip", "sip:"),
    SBS("sbs", "sbs:"),
    Unknown("unknown", "n/a:");

    public final String name;
    public final String prefix;

    CallingProtocol(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public static CallingProtocol fromName(String name) {
        if (name == null)
            return Unknown;
        name = name.toLowerCase();
        for (CallingProtocol protocol: CallingProtocol.values()) {
            if (name.equals(protocol.name))
                return protocol;
        }
        return Unknown;
    }

    public static CallingProtocol fromUrl(String url) {
        if (url == null)
            return Unknown;
        url = url.toLowerCase();
        for (CallingProtocol protocol: CallingProtocol.values()) {
            if (url.startsWith(protocol.prefix))
                return protocol;
        }
        return Unknown;
    }
}
