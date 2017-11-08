package sanp.javalon.media.base;

public class AVDefines {
    public enum DataType {
        UNKNOWN("unknown"),
        AUDIO("audio"),
        VIDEO("video"),
        VIDEO_EXT("video_ext"),
        SUBTITLE("subtitle"),
        DATA("data"),
        ATTACHMENT("attachment"),
        APPLICATION("application"),
        FECC("fecc");

        private String dsp = "";
        DataType(String dsp) {
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
    }

    public enum DataFlag {
        NONE,
        CODEC_SPECIFIC_DATA,
    }
}
