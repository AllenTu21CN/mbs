package cn.sanbu.avalon.endpoint3.structures;

import android.media.MediaCodecInfo;

public enum H264Profile {

    Unspecified("N/A", 0, -1),
    CBaseLine("CBP", 1, MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline),
    BaseLine("BP", 2, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline),
    Main("MP", 3, MediaCodecInfo.CodecProfileLevel.AVCProfileMain),
    Extended("XP", 4, MediaCodecInfo.CodecProfileLevel.AVCProfileExtended),
    High("HiP", 5, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh),
    High10("Hi10P", 6, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10),
    High42("Hi422P", 7, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422),
    High44("Hi444PP", 8, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444);

    public final int value;
    public final String name;
    public final int android;

    H264Profile(String name, int value, int android) {
        this.value = value;
        this.name = name;
        this.android = android;
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

    public static H264Profile fromAndroidValue(int value) {
        for (H264Profile profile: values()) {
            if (profile.android == value)
                return profile;
        }
        return Unspecified;
    }
}
