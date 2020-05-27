package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.H264Profile;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

/**
 * Created by Tuyj on 2018/3/16.
 */

public class VideoFormat {
    public CodecType codec;
    public H264Profile profile;
    public Resolution resolution;
    public int framerate;
    public Bandwidth bandwidth;
    private int key_interval_in_frames;

    public VideoFormat(CodecType codec, H264Profile profile, Resolution resolution,
                       int framerate, Bandwidth bandwidth, int keyIntervalInFrames) {
        this.codec = codec;
        this.profile = profile;
        this.resolution = resolution;
        this.framerate = framerate;
        this.bandwidth = bandwidth;
        this.key_interval_in_frames = keyIntervalInFrames;
    }

    public VideoFormat(VideoFormat other) {
        this(other.codec, other.profile, other.resolution,
                other.framerate, other.bandwidth, other.key_interval_in_frames);
    }

    public boolean isEqual(VideoFormat other) {
        return (CompareHelper.isEqual(codec, other.codec) &&
                CompareHelper.isEqual(profile, other.profile) &&
                CompareHelper.isEqual(resolution, other.resolution) &&
                CompareHelper.isEqual(bandwidth, other.bandwidth) &&
                framerate == other.framerate && key_interval_in_frames == other.key_interval_in_frames);
    }

    public boolean isValid() {
        return codec != null && codec != CodecType.UNKNOWN &&
                resolution != null && resolution != Resolution.RES_UNKNOWN &&
                framerate > 0 && bandwidth != null;
    }

    public int getKeyIntervalInFrames() {
        return key_interval_in_frames;
    }

    public void setKeyIntervalInFrames(int interval) {
        key_interval_in_frames = interval;
    }

    public int getKeyIntervalInSeconds() {
        if (key_interval_in_frames < framerate || framerate < 0)
            throw new RuntimeException("invalid key interval: " + key_interval_in_frames + "/" + framerate);

        return Math.round((float) key_interval_in_frames / (float) framerate);
    }

    public void setKeyIntervalInSeconds(int interval) {
        if (interval < 0 || framerate < 0)
            throw new RuntimeException("invalid framerate: " + interval + "*" + framerate);

        key_interval_in_frames = interval * framerate;
    }
}
