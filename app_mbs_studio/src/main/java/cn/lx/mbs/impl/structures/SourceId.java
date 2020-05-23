package cn.lx.mbs.impl.structures;

public enum SourceId {
    None("None"),
    IPC1("IPC1"),
    IPC2("IPC2"),
    IPC3("IPC3"),
    IPC4("IPC4"),
    IPC5("IPC5"),
    HDMI_IN1("HDMI-IN1"),
    HDMI_IN2("HDMI-IN2"),

    MIC("MIC");

    public final String tsName;

    SourceId(String tsName) {
        this.tsName = tsName;
    }

    public static SourceId fromTSName(String tsName) {
        for (SourceId id: values()) {
            if (id.tsName.equals(tsName))
                return id;
        }
        return null;
    }
}
