package cn.lx.mbs.impl.structures;

import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

public class TerminalInfo extends CallingParam {

    public TerminalInfo() {
    }

    public TerminalInfo(String name, String url, CallingProtocol protocol, Bandwidth bandwidth, Resolution resolution, int framerate, List<CodecType> vCodecs, List<AudioCodec> aCodecs) {
        super(name, url, protocol, bandwidth, resolution, framerate, vCodecs, aCodecs);
    }

}
