package com.sanbu.media;

import android.media.MediaFormat;

public enum CodecType {
    UNKNOWN("unknown", "N/A", 0, DataType.UNKNOWN),

    /* Video Codecs */
    YUV("yuv", "YUV", 1, DataType.VIDEO),
    H263("h263", "H.263", 2, DataType.VIDEO),
    H264("h264", "H.264", 3, DataType.VIDEO),
    H265("h265", "H.265", 4, DataType.VIDEO),
    MJPEG("mjpeg", "MJPEG", 5, DataType.VIDEO),
    VP8("vp8", "VP8", 6, DataType.VIDEO),
    VP9("vp9", "VP9", 7, DataType.VIDEO),
    FLV("flv", "FLV", 8, DataType.VIDEO),

    /* Audio Codecs */
    PCM("pcm", "PCM", 9, DataType.AUDIO),
    AAC("aac", "AAC", 10, DataType.AUDIO),
    PCMA("g711a", "G.711A", 11, DataType.AUDIO),
    PCMU("g711u", "G.711U", 12, DataType.AUDIO),
    G722("g722", "G.722", 13, DataType.AUDIO),
    G7221("g7221", "G.722.1", 14, DataType.AUDIO),
    G7221C("g7221c", "G.722.1AnnexC", 15, DataType.AUDIO),
    G7231("g7231", "G.7231", 16, DataType.AUDIO),
    G729("g729", "G.729", 17, DataType.AUDIO),
    ILBC("ilbc", "ILBC", 18, DataType.AUDIO),
    ISAC("isac", "ISAC", 19, DataType.AUDIO),
    OPUS("opus", "OPUS", 20, DataType.AUDIO),
    MP3("mp3", "MP3", 21, DataType.AUDIO);

    public final String name;
    public final String name2;
    public final int value;
    public final DataType type;

    CodecType(String name, String name2, int value, DataType type) {
        this.name = name;
        this.name2 = name2;
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

        name = name.toUpperCase();
        for (CodecType type: CodecType.values()) {
            if (name.equals(type.name2))
                return type;
        }

        return UNKNOWN;
    }

    public static CodecType fromSelfName(String name) {
        try {
            return CodecType.valueOf(name);
        } catch (Exception e) {
            return UNKNOWN;
        }
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