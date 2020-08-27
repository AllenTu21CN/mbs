package cn.sanbu.avalon.endpoint3.structures.jni;

import android.media.MediaCodecInfo;

public enum AVCLevel {
    UNSPECIFIED("N/A", 0, -1),
    _1("1", 10, MediaCodecInfo.CodecProfileLevel.AVCLevel1),
    _1b("1b", 9, MediaCodecInfo.CodecProfileLevel.AVCLevel1b),
    _1_1("1.1", 11, MediaCodecInfo.CodecProfileLevel.AVCLevel11),
    _1_2("1.2", 12, MediaCodecInfo.CodecProfileLevel.AVCLevel12),
    _1_3("1.3", 13, MediaCodecInfo.CodecProfileLevel.AVCLevel13),
    _2("2", 20, MediaCodecInfo.CodecProfileLevel.AVCLevel2),
    _2_1("2.1", 21, MediaCodecInfo.CodecProfileLevel.AVCLevel21),
    _2_2("2.2", 22, MediaCodecInfo.CodecProfileLevel.AVCLevel22),
    _3("3", 30, MediaCodecInfo.CodecProfileLevel.AVCLevel3),
    _3_1("3.1", 31, MediaCodecInfo.CodecProfileLevel.AVCLevel31),
    _3_2("3.2", 32, MediaCodecInfo.CodecProfileLevel.AVCLevel32),
    _4("4", 40, MediaCodecInfo.CodecProfileLevel.AVCLevel4),
    _4_1("4.1", 41, MediaCodecInfo.CodecProfileLevel.AVCLevel41),
    _4_2("4.2", 42, MediaCodecInfo.CodecProfileLevel.AVCLevel42),
    _5("5", 50, MediaCodecInfo.CodecProfileLevel.AVCLevel5),
    _5_1("5.1", 51, MediaCodecInfo.CodecProfileLevel.AVCLevel51),
    _5_2("5.2", 52, MediaCodecInfo.CodecProfileLevel.AVCLevel52);

    public String name;
    public int idc;         // used for SDP
    public int android;     // used for MediaCodec of android

    AVCLevel(String name, int idc, int android) {
        this.name = name;
        this.idc = idc;
        this.android = android;
    }

    public static AVCLevel fromIndex(int index) {
        AVCLevel[] levels = values();
        if (index < 0 || index >= levels.length)
            return UNSPECIFIED;
        return levels[index];
    }

    public static AVCLevel fromName(String name) {
        for (AVCLevel level: values()) {
            if (level.name.equals(name))
                return level;
        }
        return UNSPECIFIED;
    }

    public static AVCLevel fromIDC(int idc) {
        for (AVCLevel level: values()) {
            if (level.idc == idc)
                return level;
        }
        return UNSPECIFIED;
    }

    public static AVCLevel fromAndroidValue(int value) {
        for (AVCLevel level: values()) {
            if (level.android == value)
                return level;
        }
        return UNSPECIFIED;
    }
}
