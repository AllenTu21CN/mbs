package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.media.CodecType;

public class NetStatistics {
    public static class Audio {
        public CodecType codec; // 协议:PCMA/AAC/...
        public String codec2;   // 协议:G.711A
        public int bitrate;     // 码率:64000(bps)

        public Audio(CodecType codec, int bitrate) {
            this.codec = codec;
            this.bitrate = bitrate;
            update();
        }

        public void update() {
            codec2 = codec.name2;
        }
    }

    public static class AudioTx extends Audio {
        public AudioTx(CodecType codec, int bitrate) {
            super(codec, bitrate);
        }
    }

    public static class AudioRx extends Audio {
        public long loss;       // 丢包数
        public int jitter;      // 抖动

        public AudioRx(CodecType codec, int bitrate, long loss, int jitter) {
            super(codec, bitrate);
            this.loss = loss;
            this.jitter = jitter;
        }
    }

    public static class Video {
        public CodecType codec;     // 协议:H264/H265/...
        public String codec2;       // 协议:H.264
        public String resolution;   // 分辨率: 1920x1088
        public float framerate;     // 帧率: 30.0
        public int bitrate;         // 码率:2048000(bps)

        private String profile;     // Profile: baseline

        public Video(CodecType codec, String profile,
                     String resolution, float framerate, int bitrate) {
            this.codec = codec;
            this.profile = profile;
            this.resolution = resolution;
            this.framerate = framerate;
            this.bitrate = bitrate;
            update();
        }

        public void update() {
            codec2 = codec.name2;
        }
    }

    public static class VideoTx extends Video {
        public long resend;         // 重传包数(暂时无法获取)

        public VideoTx(CodecType codec, String profile, String resolution,
                       float framerate, int bitrate, long resend) {
            super(codec, profile, resolution, framerate, bitrate);
            this.resend = resend;
        }
    }

    public static class VideoRx extends Video {
        public long loss;           // 丢包数
        public int jitter;          // 抖动

        public VideoRx(CodecType codec, String profile, String resolution,
                       float framerate, int bitrate, long loss, int jitter) {
            super(codec, profile, resolution, framerate, bitrate);
            this.loss = loss;
            this.jitter = jitter;
        }
    }
}
