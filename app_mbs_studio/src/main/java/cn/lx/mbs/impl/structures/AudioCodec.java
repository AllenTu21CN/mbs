package cn.lx.mbs.impl.structures;

import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.AudioSamplerate;
import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;

// 业务场景
public enum AudioCodec {
    G722_64Kbps("G.722(16KHz/1/64Kbps)", new AudioFormat(CodecType.G722, AudioSamplerate.HZ_16K, 1, Bandwidth._64K, null)),
    G7221C_48Kbps("G.722.1C(32KHz/1/48Kbps)", new AudioFormat(CodecType.G7221C, AudioSamplerate.HZ_32K, 1, Bandwidth._48K, null)),
    G711A("G.711A(8KHz/1/64Kbps)", new AudioFormat(CodecType.PCMA, AudioSamplerate.HZ_8K, 1, Bandwidth._64K, null)),
    G711U("G.711U(8KHz/1/64Kbps)", new AudioFormat(CodecType.PCMU, AudioSamplerate.HZ_8K, 1, Bandwidth._64K, null));

    public final String name;
    public final AudioFormat format;

    AudioCodec(String name, AudioFormat format) {
        this.name = name;
        this.format = format;
    }
}
