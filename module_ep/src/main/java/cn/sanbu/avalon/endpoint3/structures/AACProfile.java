package cn.sanbu.avalon.endpoint3.structures;

public enum AACProfile {
    UNSPECIFIED(0, "Unspecified"),
    LC(1, "LowComplexity"),
    HE(2, "HighEfficiency"),
    HEv2(3, "HighEfficiency v2"),
    LD(4, "LowDelay");

    public final int value;
    public final String desc;

    AACProfile(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static AACProfile fromValue(int value) {
        for (AACProfile profile: AACProfile.values()) {
            if (value == profile.value)
                return profile;
        }
        return UNSPECIFIED;
    }
}
