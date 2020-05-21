package cn.sanbu.avalon.endpoint3.structures.jni;

import java.util.List;

public class AudioCapabilities {
    public static class Capabilities {
        public final List<AudioCapability> audio;

        public Capabilities(List<AudioCapability> audio) {
            this.audio = audio;
        }
    }

    public final Capabilities receive_capabilities;
    public final Capabilities send_capabilities;

    public AudioCapabilities(Capabilities receive, Capabilities send) {
        this.receive_capabilities = receive;
        this.send_capabilities = send;
    }

    public boolean isEqual(AudioCapabilities other) {
        throw new RuntimeException("not implement");
    }
}
