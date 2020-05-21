package cn.sanbu.avalon.endpoint3.structures.jni;

import cn.sanbu.avalon.endpoint3.structures.H264Profile;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

public class VideoCapability {

    private CodecType codec;
    private String profile;
    private String resolution;
    private int max_frame_rate;
    private int key_frame_interval;

    public VideoCapability(CodecType codec, H264Profile profile, Resolution resolution, int maxFps) {
        this(codec, profile, resolution, maxFps, 10);
    }

    public VideoCapability(CodecType codec, H264Profile profile, Resolution resolution, int maxFps, int keyIntervalInSec) {
        this.codec = codec;
        this.profile = profile.name;
        this.resolution = String.format("%dx%d", resolution.width, resolution.height);
        this.max_frame_rate = maxFps;
        this.key_frame_interval = maxFps * keyIntervalInSec;
    }

    public CodecType getCodec() {
        return codec;
    }

    public void setCodec(CodecType codec) {
        this.codec = codec;
    }

    public H264Profile getProfile() {
        return H264Profile.fromName(profile);
    }

    public void setProfile(H264Profile profile) {
        this.profile = profile.name;
    }

    public Resolution getResolution() {
        String wh[] = resolution.split("x");
        if (wh.length != 2)
            return Resolution.RES_UNKNOWN;
        try {
            return Resolution.fromRes(Integer.valueOf(wh[0]), Integer.valueOf(wh[1]));
        } catch (Exception e) {
            return Resolution.RES_UNKNOWN;
        }
    }

    public void setResolution(Resolution resolution) {
        this.resolution = String.format("%dx%d", resolution.width, resolution.height);
    }

    public int getMaxFramerate() {
        return max_frame_rate;
    }

    public void setMaxFramerate(int maxFps) {
        this.max_frame_rate = maxFps;
    }

    public int getKeyFrameInterval() {
        return key_frame_interval;
    }

    public void setKeyFrameInterval(int inFrames) {
        this.key_frame_interval = inFrames;
    }
}
