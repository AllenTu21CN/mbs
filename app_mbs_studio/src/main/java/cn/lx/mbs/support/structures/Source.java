package cn.lx.mbs.support.structures;

import com.sanbu.media.AudioFormat;
import com.sanbu.media.InputType;
import com.sanbu.media.Resolution;
import com.sanbu.media.VideoFormat;
import com.sanbu.tools.StringUtil;

import java.util.List;

import cn.lx.mbs.LXConst;

public class Source {
    public final int id;              // 源ID
    public final String name;         // 源名称
    public final InputType type;     // 源类型
    public final String url;          // 源URL
    public final boolean useAudio;    // 是否使用源的音频流

    // for VideoCapture
    public Resolution resolution; // 指定分辨率, RES_UNKNOWN表示自动

    // for RTSP
    public boolean overTCP;       // RTSP源是否启用TCP传输
    public String extraOptions;   // RTSP额外选项

    // for RMSP
    public VideoFormat videoFormat;
    public AudioFormat audioFormat;

    // for File
    public String path;  // 原始的文件路径
    public boolean loop; // 文件源是否循环播放

    // for Caller
    public List<AudioCodec> audioCodecs;

    public static Source buildVideoCapture(String name, String deviceId, Resolution resolution) {
        return buildVideoCapture(-1, name, deviceId, resolution);
    }

    public static Source buildVideoCapture(int id, String name, String deviceId, Resolution resolution) {
        if (StringUtil.isEmpty(deviceId) || resolution == null)
            throw new RuntimeException("invalid params");

        String url = InputType.VideoCapture.prefix + deviceId;
        Source source = new Source(id, name, InputType.VideoCapture, url);
        source.resolution = resolution;
        return source;
    }

    public static Source buildAudioCapture(String name, int deviceId) {
        if (deviceId < 0)
            throw new RuntimeException("invalid params");

        String url = InputType.AudioCapture.prefix + deviceId;
        return new Source(-1, name, InputType.AudioCapture, url);
    }

    public static Source buildRTSP(String name, String url, boolean useAudio,
                                   boolean rtspOverTCP, String extraOptions) {
        return buildRTSP(-1, name, url, useAudio, rtspOverTCP, extraOptions);
    }

    public static Source buildRTSP(int id, String name, String url, boolean useAudio,
                                   boolean rtspOverTCP, String extraOptions) {
        if (StringUtil.isEmpty(url) || !url.startsWith(InputType.RTSP.prefix))
            throw new RuntimeException("invalid params");

        Source source = new Source(id, name, InputType.RTSP, url, useAudio);
        source.overTCP = rtspOverTCP;
        source.extraOptions = extraOptions;
        return source;
    }

    public static Source buildRTMP(String name, String url) {
        return buildRTMP(-1, name, url);
    }

    public static Source buildRTMP(int id, String name, String url) {
        if (StringUtil.isEmpty(url) || !url.startsWith(InputType.RTMP.prefix))
            throw new RuntimeException("invalid params");

        return new Source(id, name, InputType.RTMP, url);
    }

    public static Source buildRMSP(String name, String url,
                                   VideoFormat videoFormat, AudioFormat audioFormat) {
        return buildRMSP(-1, name, url, videoFormat, audioFormat);
    }

    public static Source buildRMSP(int id, String name, String url,
                                   VideoFormat videoFormat, AudioFormat audioFormat) {
        if (StringUtil.isEmpty(url) || !url.startsWith(InputType.RMSP.prefix))
            throw new RuntimeException("invalid params");

        Source source = new Source(id, name, InputType.RMSP, url);
        source.videoFormat = videoFormat;
        source.audioFormat = audioFormat;
        return source;
    }

    public static Source buildFile(String name, String path, boolean loop) {
        return buildFile(-1, name, path, loop);
    }

    public static Source buildFile(int id, String name, String path, boolean loop) {
        if (StringUtil.isEmpty(path) || !path.startsWith("/"))
            throw new RuntimeException("invalid params");

        String url = InputType.File.prefix + path;
        Source source = new Source(id, name, InputType.File, url);
        source.path = path;
        source.loop = loop;
        return source;
    }

    public static Source buildCaller(String name, String url) {
        return buildCaller(name, url, LXConst.DEFAULT_CALLING_AUDIO_CODECS);
    }

    public static Source buildCaller(String name, String url, List<AudioCodec> audioCodecs) {
        return buildCaller(-1, name, url, audioCodecs);
    }

    public static Source buildCaller(int id, String name, String url, List<AudioCodec> audioCodecs) {
        if (StringUtil.isEmpty(url) ||
                InputType.fromUrl(url) != InputType.Caller)
            throw new RuntimeException("invalid params");

        Source source = new Source(id, name, InputType.Caller, url);
        source.audioCodecs = audioCodecs;
        return source;
    }

    public static Source copy(int id, Source other) {
        return new Source(id, other.name, other.type, other.url,
                other.useAudio, other.resolution,
                other.overTCP, other.extraOptions,
                other.videoFormat, other.audioFormat,
                other.path, other.loop, other.audioCodecs);
    }

    private Source(int id, String name, InputType type, String url) {
        this(id, name, type, url, true);
    }

    private Source(int id, String name, InputType type, String url, boolean useAudio) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.useAudio = useAudio;
    }

    public Source(int id, String name, InputType type, String url, boolean useAudio,
                  Resolution resolution, boolean overTCP, String extraOptions,
                  VideoFormat videoFormat, AudioFormat audioFormat, String path,
                  boolean loop, List<AudioCodec> audioCodecs) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.useAudio = useAudio;
        this.resolution = resolution;
        this.overTCP = overTCP;
        this.extraOptions = extraOptions;
        this.videoFormat = videoFormat;
        this.audioFormat = audioFormat;
        this.path = path;
        this.loop = loop;
        this.audioCodecs = audioCodecs;
    }
}
