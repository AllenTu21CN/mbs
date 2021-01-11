package com.sanbu.media;

public enum AudioCodec {
    G722_64Kbps(1, "G.722(16KHz/1/64Kbps)", new AudioFormat(CodecType.G722, AudioSamplerate.HZ_16K, 1, Bandwidth._64K, null)),
    G7221C_48Kbps(2, "G.722.1C(32KHz/1/48Kbps)", new AudioFormat(CodecType.G7221C, AudioSamplerate.HZ_32K, 1, Bandwidth._48K, null)),
    G711A(3, "G.711A(8KHz/1/64Kbps)", new AudioFormat(CodecType.PCMA, AudioSamplerate.HZ_8K, 1, Bandwidth._64K, null)),
    G711U(4, "G.711U(8KHz/1/64Kbps)", new AudioFormat(CodecType.PCMU, AudioSamplerate.HZ_8K, 1, Bandwidth._64K, null)),
    AAC(5, "AAC(48KHz/2/128Kbps)", new AudioFormat(CodecType.AAC, AudioSamplerate.HZ_48K, 2, Bandwidth._128K, AACProfile.LC));

    public final int value;
    public final String name;
    public final AudioFormat format;

    AudioCodec(int value, String name, AudioFormat format) {
        this.value = value;
        this.name = name;
        this.format = format;
    }

    public static AudioCodec fromValue(int value) {
        for (AudioCodec codec : values()) {
            if (codec.value == value)
                return codec;
        }
        return null;
    }
}
