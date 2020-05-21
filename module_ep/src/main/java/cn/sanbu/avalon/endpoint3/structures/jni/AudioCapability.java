package cn.sanbu.avalon.endpoint3.structures.jni;

import cn.sanbu.avalon.endpoint3.structures.AACProfile;
import cn.sanbu.avalon.endpoint3.structures.AudioSamplerate;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;

public class AudioCapability {
    public CodecType codec;
    public AACProfile profile;
    public int sample_rate;
    public int channels;
    public int bitrate;

    public AudioCapability(CodecType codec, AACProfile profile,
                           AudioSamplerate samplerate, int channels, Bandwidth bandwidth) {
        this.codec = codec;
        this.profile = profile;
        this.sample_rate = samplerate.hz;
        this.channels = channels;
        this.bitrate = bandwidth.bps;
    }

    public AudioCapability(AudioFormat format) {
        this(format.codec, format.profile, format.samplerate, format.channels, format.bandwidth);
    }

    public AudioSamplerate getSamplerate() {
        return AudioSamplerate.fromValue(sample_rate);
    }

    public Bandwidth getBandwidth() {
        return Bandwidth.fromValue(bitrate);
    }
}
