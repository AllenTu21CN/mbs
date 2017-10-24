package sanp.avalon.libs.media.base;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.util.HashMap;
import java.util.Map;

public abstract class MediaInfo {
    static public int MEDIA_CODEC_UNKNOWN = 0;
    // VIDEO_CODEC_...
    static public int VIDEO_CODEC_GENERIC = 0x10;
    static public int VIDEO_CODEC_H264 = VIDEO_CODEC_GENERIC + 1;
    static public int VIDEO_CODEC_H265 = VIDEO_CODEC_GENERIC + 2;
    // AUDIO_CODEC_...
    static public int AUDIO_CODEC_GENERIC = 0x20;
    static public int AUDIO_CODEC_AAC   = AUDIO_CODEC_GENERIC + 1;
    static public int AUDIO_CODEC_G711A = AUDIO_CODEC_GENERIC + 2;
    static public int AUDIO_CODEC_G711U = AUDIO_CODEC_GENERIC + 3;
    // DATA_CODEC_...
    static public int DATA_CODEC_GENERIC = 0x30;
    static public Map<Integer, String> CodecId2Mime = new HashMap<Integer, String>() {{
        put(VIDEO_CODEC_H264,  MediaFormat.MIMETYPE_VIDEO_AVC);
        put(VIDEO_CODEC_H265,  MediaFormat.MIMETYPE_VIDEO_HEVC);
        put(AUDIO_CODEC_AAC,   MediaFormat.MIMETYPE_AUDIO_AAC);
        put(AUDIO_CODEC_G711A, MediaFormat.MIMETYPE_AUDIO_G711_ALAW);
        put(AUDIO_CODEC_G711U, MediaFormat.MIMETYPE_AUDIO_G711_MLAW);
    }};

    static public int CODEC_PROFILE_UNKNOWN   = 0;
    // AAC_PROFILE_...
    static public int CODEC_PROFILE_AAC_LC    = 1;
    static public int CODEC_PROFILE_AAC_LD    = 2;
    static public int CODEC_PROFILE_AAC_HE    = 3;
    static public int CODEC_PROFILE_AAC_HEV2  = 4;
    static public Map<Integer, Integer> ProfileId2AndroidId = new HashMap<Integer, Integer>() {{
        put(CODEC_PROFILE_UNKNOWN,  -1);
        put(CODEC_PROFILE_AAC_LC,   MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        put(CODEC_PROFILE_AAC_LD,   MediaCodecInfo.CodecProfileLevel.AACObjectLD);
        put(CODEC_PROFILE_AAC_HE,   MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        put(CODEC_PROFILE_AAC_HEV2, MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS);
    }};

    public int id = -1;
    public String mime = "";
    protected void init(int id, String mime) {
        this.id = id;
        this.mime = mime;
    }

    public abstract String typeName();
    public abstract MediaFormat convert();
}

