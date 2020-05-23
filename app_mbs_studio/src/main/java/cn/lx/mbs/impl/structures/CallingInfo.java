package cn.lx.mbs.impl.structures;

import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.jni.EPDir;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

public class CallingInfo extends CallingParam {

    public EPDir dir;   // 呼叫方向
    public String date; // 发起时间

    public CallingInfo() {
    }

    public CallingInfo(String name, String url, CallingProtocol protocol,
                       Bandwidth bandwidth, Resolution resolution, int framerate,
                       List<CodecType> vCodecs, List<AudioCodec> aCodecs, EPDir dir, String date) {
        super(name, url, protocol, bandwidth, resolution, framerate, vCodecs, aCodecs);
        this.dir = dir;
        this.date = date;
    }

    public CallingInfo(CallingParam param, EPDir dir, String date) {
        super(param);
        this.dir = dir;
        this.date = date;
    }

    public CallingInfo(CallingInfo other) {
        this(other, other.dir, other.date);
    }
}
