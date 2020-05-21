package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

import cn.sanbu.avalon.endpoint3.structures.AACProfile;
import cn.sanbu.avalon.endpoint3.structures.AudioSamplerate;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;

public class AudioFormat {
    public CodecType codec;
    public AudioSamplerate samplerate;
    public int channels;
    public Bandwidth bandwidth;
    public AACProfile profile;

    public AudioFormat(CodecType codec, AudioSamplerate samplerate, int channels,
                       Bandwidth bandwidth, AACProfile profile) {
        this.codec = codec;
        this.samplerate = samplerate;
        this.channels = channels;
        this.bandwidth = bandwidth;
        this.profile = profile;
    }

    public AudioFormat(AudioFormat other) {
        this(other.codec, other.samplerate, other.channels, other.bandwidth, other.profile);
    }

    public boolean isEqual(AudioFormat other) {
        return (CompareHelper.isEqual(codec, other.codec) &&
                CompareHelper.isEqual(bandwidth, other.bandwidth) &&
                CompareHelper.isEqual(profile, other.profile) &&
                CompareHelper.isEqual(samplerate, other.samplerate) &&
                channels == other.channels);
    }

    public boolean isValid() {
        if (codec == null || codec == CodecType.UNKNOWN)
            return false;
        if (codec == CodecType.AAC && (profile == null || profile == AACProfile.UNSPECIFIED))
            return false;
        return true;
    }
}
