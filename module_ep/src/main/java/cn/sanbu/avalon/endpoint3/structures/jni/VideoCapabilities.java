package cn.sanbu.avalon.endpoint3.structures.jni;

import java.util.List;

public class VideoCapabilities {

    public static class Capabilities {
        public final List<VideoCapability> video;
        public final List<VideoCapability> video_ext;

        public Capabilities(List<VideoCapability> video, List<VideoCapability> extVideo) {
            this.video = video;
            this.video_ext = extVideo;
        }
    }

    public final Capabilities receive_capabilities;
    public final Capabilities send_capabilities;

    public VideoCapabilities(Capabilities receive, Capabilities send) {
        this.receive_capabilities = receive;
        this.send_capabilities = send;
    }

    public boolean isEqual(VideoCapabilities other) {
        throw new RuntimeException("not implement");
    }

    public static boolean isGreater(VideoCapabilities src, VideoCapabilities dst) {
        return src.send_capabilities.video.get(0).getResolution().width > dst.send_capabilities.video.get(0).getResolution().width ||
            src.receive_capabilities.video.get(0).getResolution().width > dst.receive_capabilities.video.get(0).getResolution().width;
    }
}
