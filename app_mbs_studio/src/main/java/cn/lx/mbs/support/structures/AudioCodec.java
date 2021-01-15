package cn.lx.mbs.support.structures;

import com.sanbu.media.AACProfile;
import com.sanbu.media.AudioSamplerate;
import com.sanbu.media.Bandwidth;
import com.sanbu.media.AudioFormat;
import com.sanbu.media.CodecType;

// 业务场景
public enum AudioCodec {
    G722_64Kbps("G.722(16KHz/1/64Kbps)", new AudioFormat(CodecType.G722, AudioSamplerate.HZ_16K, 1, Bandwidth._64K, null)),
    G7221C_48Kbps("G.722.1C(32KHz/1/48Kbps)", new AudioFormat(CodecType.G7221C, AudioSamplerate.HZ_32K, 1, Bandwidth._48K, null)),
    G711A("G.711A(8KHz/1/64Kbps)", new AudioFormat(CodecType.PCMA, AudioSamplerate.HZ_8K, 1, Bandwidth._64K, null)),
    G711U("G.711U(8KHz/1/64Kbps)", new AudioFormat(CodecType.PCMU, AudioSamplerate.HZ_8K, 1, Bandwidth._64K, null)),
    AAC("AAC(48KHz/2/128Kbps)", new AudioFormat(CodecType.AAC, AudioSamplerate.HZ_48K, 2, Bandwidth._128K, AACProfile.LC));

    public final String name;
    public final AudioFormat format;

    AudioCodec(String name, AudioFormat format) {
        this.name = name;
        this.format = format;
    }
}
