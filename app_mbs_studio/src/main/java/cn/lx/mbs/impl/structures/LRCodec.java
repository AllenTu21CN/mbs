package cn.lx.mbs.impl.structures;

import cn.lx.mbs.impl.core.CoreUtils;
import com.sanbu.tools.CompareHelper;

import cn.sanbu.avalon.endpoint3.structures.AACProfile;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.AudioSamplerate;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.H264Profile;
import cn.sanbu.avalon.endpoint3.structures.Resolution;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;

// 直播录制编码参数
public class LRCodec {
    public CodecType vCodec;            // 视频编码格式
    public H264Profile vProfile;        // 视频编码Profile
    public Resolution vResolution;      // 视频分辨率
    public int vFramerate;              // 视频帧率
    public Bandwidth vBandwidth;        // 视频码率(带宽)
    public int vIFrameInterval;         // 视频I帧间隔(秒)

    public CodecType aCodec;            // 音频编码格式
    public AACProfile aacProfile;       // AAC编码Profile
    public AudioSamplerate aSamplerate; // 音频采样率
    public int aChannels;               // 音频声道数
    public Bandwidth aBandwidth;        // 音频码率(带宽)

    public LRCodec(CodecType vCodec, H264Profile vProfile,
                   Resolution vResolution, int vFramerate,
                   Bandwidth vBandwidth, int vIFrameInterval,
                   CodecType aCodec, AACProfile aacProfile,
                   AudioSamplerate aSamplerate, int aChannels, Bandwidth aBandwidth) {
        this.vCodec = vCodec;
        this.vProfile = vProfile;
        this.vResolution = vResolution;
        this.vFramerate = vFramerate;
        this.vBandwidth = vBandwidth;
        this.vIFrameInterval = vIFrameInterval;
        this.aCodec = aCodec;
        this.aacProfile = aacProfile;
        this.aSamplerate = aSamplerate;
        this.aChannels = aChannels;
        this.aBandwidth = aBandwidth;
    }

    public LRCodec(VideoFormat video, AudioFormat audio) {
        this(video.codec, video.profile, video.resolution, video.framerate, video.bandwidth,
                CoreUtils.trans2IFrameIntervalInSecond(video.key_interval_in_frames, video.framerate),
                audio.codec, audio.profile, audio.samplerate, audio.channels, audio.bandwidth);
    }

    public LRCodec(LRCodec other) {
        this(other.vCodec, other.vProfile, other.vResolution, other.vFramerate,
                other.vBandwidth, other.vIFrameInterval, other.aCodec, other.aacProfile,
                other.aSamplerate, other.aChannels, other.aBandwidth);
    }

    public boolean isEqual(LRCodec other) {
        return CompareHelper.isEqual(vCodec, other.vCodec) &&
                CompareHelper.isEqual(vProfile, other.vProfile) &&
                CompareHelper.isEqual(vResolution, other.vResolution) &&
                CompareHelper.isEqual(vFramerate, other.vFramerate) &&
                CompareHelper.isEqual(vBandwidth, other.vBandwidth) &&
                CompareHelper.isEqual(vIFrameInterval, other.vIFrameInterval) &&
                CompareHelper.isEqual(aCodec, other.aCodec) &&
                CompareHelper.isEqual(aacProfile, other.aacProfile) &&
                CompareHelper.isEqual(aSamplerate, other.aSamplerate) &&
                CompareHelper.isEqual(aChannels, other.aChannels) &&
                CompareHelper.isEqual(aBandwidth, other.aBandwidth);
    }

    public VideoFormat getVideoFormat() {
        return new VideoFormat(vCodec, vProfile, vResolution, vFramerate, vBandwidth,
                CoreUtils.trans2IFrameIntervalInFrame(vIFrameInterval, vFramerate));
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(aCodec, aSamplerate, aChannels, aBandwidth, aacProfile);
    }
}
