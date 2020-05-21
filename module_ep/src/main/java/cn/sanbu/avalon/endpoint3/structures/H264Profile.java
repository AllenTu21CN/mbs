package cn.sanbu.avalon.endpoint3.structures;

public enum H264Profile {

    Unspecified("N/A", 0),
    CBaseLine("CBP", 1),
    BaseLine("BP", 2),
    Main("MP", 3),
    Extended("XP", 4),
    High("HiP", 5),
    High10("Hi10P", 6),
    High42("Hi422P", 7),
    High44("Hi444PP", 8);

    public final int value;
    public final String name;

    H264Profile(String name, int value) {
        this.value = value;
        this.name = name;
    }

    public static H264Profile fromValue(int value) {
        for (H264Profile profile: H264Profile.values()) {
            if (value == profile.value)
                return profile;
        }
        return Unspecified;
    }

    public static H264Profile fromName(String name) {
        if (name == null)
            return Unspecified;

        for (H264Profile profile: H264Profile.values()) {
            if (name.equals(profile.name))
                return profile;
        }
        return Unspecified;
    }
}
