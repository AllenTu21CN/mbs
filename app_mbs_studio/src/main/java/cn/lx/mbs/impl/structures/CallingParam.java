package cn.lx.mbs.impl.structures;

import com.sanbu.tools.StringUtil;

import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.CallingUrl;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

public class CallingParam {

    public String name;              // 对端名称
    public String url;               // 呼叫号码(URL/IP/短号)
    public CallingProtocol protocol; // 呼叫信令(协议)
    public Bandwidth bandwidth;      // 带宽(上限)
    public Resolution resolution;    // 分辨率(上限)
    public int framerate;            // 帧率(上限)
    public List<CodecType> vCodecs;  // 视频格式候选
    public List<AudioCodec> aCodecs; // 音频格式候选

    public CallingParam() {
    }

    public CallingParam(String name, String url, CallingProtocol protocol,
                        Bandwidth bandwidth, Resolution resolution, int framerate,
                        List<CodecType> vCodecs, List<AudioCodec> aCodecs) {
        this.name = name;
        this.url = url;
        this.protocol = protocol;
        this.bandwidth = bandwidth;
        this.resolution = resolution;
        this.framerate = framerate;
        this.vCodecs = vCodecs;
        this.aCodecs = aCodecs;
    }

    public CallingParam(CallingParam other) {
        this(other.name, other.url, other.protocol, other.bandwidth,
                other.resolution, other.framerate, other.vCodecs, other.aCodecs);
    }

    public boolean isValid() {
        return !StringUtil.isEmpty(name) && !StringUtil.isEmpty(url) &&
                protocol != null && protocol != CallingProtocol.Unknown &&
                bandwidth != null && resolution != null && framerate > 0 &&
                vCodecs != null && vCodecs.size() > 0 &&
                aCodecs != null && aCodecs.size() > 0 ;
    }

    public CallingUrl getUrl() {
        CallingUrl curl = CallingUrl.parse(url);
        if (curl == null)
            return null;
        if (curl.protocol == CallingProtocol.Unknown)
            curl.protocol = protocol;
        return curl;
    }

    public String getDescName() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append("<")
                .append(protocol == null ? "null:" : protocol.prefix)
                .append(url).append(">");
        return builder.toString();
    }

    public boolean checkName() {
        if (name != null)
            return true;

        CallingUrl url = getUrl();
        if (url == null)
            return false;

        name = url.username;
        if (name == null)
            name = url.address;
        if (name == null)
            name = "unknown";
        return true;
    }
}
