package cn.lx.mbs.impl.structures;

import com.sanbu.tools.StringUtil;

import cn.sanbu.avalon.endpoint3.structures.Resolution;
import cn.sanbu.avalon.endpoint3.structures.SourceType;

public class Source {
    // common
    public final String name;             // 源名称
    public final SourceId id;             // 源ID, 例: IPC1,HDMI-IN1
    public final String url;              // 源URL, 例: rtsp://...  or device://0
    public final SourceType type;         // 源类型

    // for rtsp source
    public final boolean rtspOverTCP;     // RTSP源是否启用TCP传输

    // for hdmi source
    public final Resolution hdmiRes;      // 指定HDMI分辨率, RES_UNKNOWN表示自动

    public static Source buildRTSPSource(SourceId id, String url, boolean overTCP) {
        return new Source("网络摄像机-" + id.tsName, id, url, overTCP, Resolution.RES_UNKNOWN);
    }

    public static Source buildRTSPSource(SourceId id, String url, boolean overTCP, String tag) {
        String attach = StringUtil.isEmpty(tag) ? "" : ("(" + tag + ")");
        return new Source("网络摄像机-" + id.tsName + attach,
                id, url, overTCP, Resolution.RES_UNKNOWN);
    }

    public static Source buildHDMISource(SourceId id, String url, Resolution resolution) {
        return new Source("本地信号源-" + id.tsName, id, url, false, resolution);
    }

    public static Source buildMICSource(SourceId id, String url) {
        return new Source("本地信号源-" + id.tsName, id, url, false, null);
    }

    private Source(String name, SourceId id, String url, boolean rtspOverTCP, Resolution hdmiRes) {
        this.name = name;
        this.id = id;
        this.url = url;
        this.type = SourceType.fromUrl(url);
        this.rtspOverTCP = rtspOverTCP;
        this.hdmiRes = hdmiRes;
        if (type == null)
            throw new RuntimeException("invalid source url: " + url);
    }
}
