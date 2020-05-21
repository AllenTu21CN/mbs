package cn.sanbu.avalon.endpoint3.structures.jni;

import android.media.MediaFormat;

public enum CodecType {
    UNKNOWN("unknown", 0, DataType.UNKNOWN),

    /* Video Codecs */
    YUV("yuv", 1, DataType.VIDEO),
    H263("h263", 2, DataType.VIDEO),
    H264("h264", 3, DataType.VIDEO),
    H265("h265", 4, DataType.VIDEO),
    MJPEG("mjpeg", 5, DataType.VIDEO),
    VP8("vp8", 6, DataType.VIDEO),
    VP9("vp9", 7, DataType.VIDEO),
    FLV("flv", 8, DataType.VIDEO),

    /* Audio Codecs */
    PCM("pcm", 9, DataType.AUDIO),
    AAC("aac", 10, DataType.AUDIO),
    PCMA("g711a", 11, DataType.AUDIO),
    PCMU("g711u", 12, DataType.AUDIO),
    G722("g722", 13, DataType.AUDIO),
    G7221("g7221", 14, DataType.AUDIO),
    G7221C("g7221c", 15, DataType.AUDIO),
    G7231("g7231", 16, DataType.AUDIO),
    G729("g729", 17, DataType.AUDIO),
    ILBC("ilbc", 18, DataType.AUDIO),
    ISAC("isac", 19, DataType.AUDIO),
    OPUS("opus", 20, DataType.AUDIO),
    MP3("mp3", 21, DataType.AUDIO);

    public final String name;
    public final int value;
    public final DataType type;

    CodecType(String name, int value, DataType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String toMime() {
        if (this == H264)
            return MediaFormat.MIMETYPE_VIDEO_AVC;
        else if (this == H265)
            return MediaFormat.MIMETYPE_VIDEO_HEVC;
        else if (this == AAC)
            return MediaFormat.MIMETYPE_AUDIO_AAC;
        else if (this == PCMA)
            return MediaFormat.MIMETYPE_AUDIO_G711_ALAW;
        else if (this == PCMU)
            return MediaFormat.MIMETYPE_AUDIO_G711_MLAW;
        else
            return "N/A";
    }

    public static CodecType fromValue(int value) {
        for (CodecType type: CodecType.values()) {
            if (value == type.value)
                return type;
        }
        return UNKNOWN;
    }

    public static CodecType fromName(String name) {
        if (name == null)
            return UNKNOWN;
        name = name.toLowerCase();
        for (CodecType type: CodecType.values()) {
            if (name.equals(type.name))
                return type;
        }

        if (name.equals("pcma"))
            return PCMA;
        else if (name.equals("pcmu"))
            return PCMU;
        else
            return UNKNOWN;
    }

    public static CodecType fromMime(String mime) {
        if (mime == null)
            return UNKNOWN;
        if (mime.equals(MediaFormat.MIMETYPE_VIDEO_AVC))
            return H264;
        else if (mime.equals(MediaFormat.MIMETYPE_VIDEO_HEVC))
            return H265;
        else if (mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC))
            return AAC;
        else if (mime.equals(MediaFormat.MIMETYPE_AUDIO_G711_ALAW))
            return PCMA;
        else if (mime.equals(MediaFormat.MIMETYPE_AUDIO_G711_MLAW))
            return PCMU;
        else
            return UNKNOWN;
    }
}