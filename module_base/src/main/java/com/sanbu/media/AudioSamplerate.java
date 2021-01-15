package com.sanbu.media;

public enum AudioSamplerate {
    HZ_48K(48000, "48K"),
    HZ_44dot1K(44100, "44.1K"),
    HZ_32K(32000, "32K"),
    HZ_16K(16000, "16K"),
    HZ_8K(8000, "8K"),
    HZ_UNKNOWN(0, "unknown");

    public final int hz;
    public final String name;

    AudioSamplerate(int hz, String name) {
        this.hz = hz;
        this.name = name;
    }

    public static AudioSamplerate fromValue(int hz) {
        for (AudioSamplerate samplerate: AudioSamplerate.values()) {
            if (hz == samplerate.hz)
                return samplerate;
        }
        return null;
    }

    public static AudioSamplerate fromName(String name) {
        if (name == null)
            return null;
        name = name.toUpperCase();

        for (AudioSamplerate samplerate: AudioSamplerate.values()) {
            if (name.equals(samplerate.name))
                return samplerate;
        }
        return null;
    }
}
