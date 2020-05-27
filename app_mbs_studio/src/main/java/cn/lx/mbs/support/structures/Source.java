package cn.lx.mbs.support.structures;

import com.sanbu.tools.StringUtil;

import cn.sanbu.avalon.endpoint3.structures.CallingUrl;
import cn.sanbu.avalon.endpoint3.structures.Resolution;
import cn.sanbu.avalon.endpoint3.structures.SourceType;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;

public class Source {
    // common
    public final int id;              // 源ID
    public final String name;         // 源名称
    public final SourceType type;     // 源类型
    public final String url;          // 源URL

    // for video capture
    public final Resolution capRes;   // 指定分辨率, RES_UNKNOWN表示自动

    // for rtsp source
    public final boolean rtspOverTCP; // RTSP源是否启用TCP传输
    public final String extraOptions; // RTSP额外选项

    // for file source
    public final boolean loop;        // 文件源是否循环播放

    // for remote source
    // public final String videoFormat;
    // public final String audioFormat;

    // for caller
    public final VideoFormat vFormat;

    public static Source buildVideoCapture(String name, int capId, Resolution capRes) {
        return buildVideoCapture(-1, name, capId, capRes);
    }

    public static Source buildAudioCapture(String name, int capId) {
        return buildAudioCapture(-1, name, capId);
    }

    public static Source buildRTSPSource(String name, String url,
                                         boolean rtspOverTCP, String extraOptions) {
        return buildRTSPSource(-1, name, url, rtspOverTCP, extraOptions);
    }

    public static Source buildRMSPSource(String name, String host, int port) {
        return buildRMSPSource(-1, name, host, port);
    }

    public static Source buildRTMPSource(String name, String url) {
        return buildRTMPSource(-1, name, url);
    }

    public static Source buildFileSource(String name, String path, boolean loop) {
        return buildFileSource(-1, name, path, loop);
    }

    public static Source buildVideoCapture(int id, String name, int capId, Resolution capRes) {
        if (capId < 0 || capRes == null)
            throw new RuntimeException("invalid params");

        SourceType type = SourceType.VideoCapture;
        String url = type.prefix + capId;
        return new Source(id, name, type, url, capRes,
                false, null, false, null);
    }

    public static Source buildAudioCapture(int id, String name, int capId) {
        if (capId < 0)
            throw new RuntimeException("invalid params");

        SourceType type = SourceType.AudioCapture;
        String url = type.prefix + capId;
        return new Source(id, name, type, url,
                null, false, null, false, null);
    }

    public static Source buildRTSPSource(int id, String name, String url,
                                         boolean rtspOverTCP, String extraOptions) {
        if (StringUtil.isEmpty(url) || !url.startsWith(SourceType.RTSP.prefix))
            throw new RuntimeException("invalid params");

        return new Source(id, name, SourceType.RTSP, url, null,
                rtspOverTCP, extraOptions, false, null);
    }

    public static Source buildRMSPSource(int id, String name, String host, int port) {
        if (StringUtil.isEmpty(host) || port <= 0)
            throw new RuntimeException("invalid params");

        SourceType type = SourceType.RMSP;
        String url = type.prefix + host + ":" + port;
        return new Source(id, name, type, url, null,
                false, null, false, null);
    }

    public static Source buildRTMPSource(int id, String name, String url) {
        if (StringUtil.isEmpty(url) || !url.startsWith(SourceType.RTMP.prefix))
            throw new RuntimeException("invalid params");

        return new Source(id, name, SourceType.RTMP, url, null,
                false, null, false, null);
    }

    public static Source buildFileSource(int id, String name, String path, boolean loop) {
        if (StringUtil.isEmpty(path) || !path.startsWith("/"))
            throw new RuntimeException("invalid params");

        SourceType type = SourceType.File;
        String url = type.prefix + path;
        return new Source(id, name, type, url, null,
                false, null, loop, null);
    }

    public static Source buildCaller(int id, String name, CallingUrl url, VideoFormat vFormat) {
        if (!url.isValid())
            throw new RuntimeException("invalid params: " + url.toString());

        return new Source(id, name, SourceType.Caller, url.toString(),
                null, false, null, false, vFormat);
    }

    public static Source copy(int id, Source other) {
        return new Source(id, other.name, other.type,
                other.url, other.capRes, other.rtspOverTCP,
                other.extraOptions, other.loop, other.vFormat);
    }

    public Source(int id, String name, SourceType type, String url, Resolution capRes,
                  boolean rtspOverTCP, String extraOptions, boolean loop,
                  VideoFormat vFormat) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.capRes = capRes;
        this.rtspOverTCP = rtspOverTCP;
        this.extraOptions = extraOptions;
        this.loop = loop;
        this.vFormat = vFormat;
    }
}
