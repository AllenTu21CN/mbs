package cn.sanbu.avalon.endpoint3.structures.jni;

import java.util.List;

public class MediaStatistics {

    public Audio audio;
    public Video video;

    public static class Audio {
        public List<Scene> scenes;
        public List<Source> sources;

        public static class Scene {
            public int id;
            public SceneConfig config;
            public List<Sink> sinks;
        }

        public static class SceneConfig {
            public List<Track> tracks;
            public float volume;
        }

        public static class Source {
            public int id;
            public List<Sink> sinks;
            public CodecStats stats;
        }

        public static class Track {
            public int source_id;
            public String type;
            public float volume;
        }

        public static class Sink {
            public int id;
            public CodecStats stats;
        }

        public static class CodecStats {
            public int bitrate;
            public int channels;
            public String codec;
            public int sample_rate;
            public String type;
        }
    }

    public static class Video {
        public List<Scene> scenes;
        public List<Source> sources;

        public static class Scene {
            public int id;
            public SceneConfig config;
            public List<Sink> sinks;
        }

        public static class SceneConfig {
            public float frame_rate;
            public String size;
            public String display_surface_size;
            public String display_name;
            public List<Overlay> overlays;
        }

        public static class Source {
            public int id;
            public List<Sink> sinks;
            public CodecStats stats;
        }

        public static class Overlay {
            public String type;
            public int source_id;
            public String display_name;
            public String dst_rect;
            public String src_rect;
        }

        public static class Sink {
            public int id;
            public CodecStats stats;
        }

        public static class CodecStats {
            public String type;
            public String codec;
            public String resolution;
            public float frame_rate;
            public int bitrate;
            public int device_id;
            public String config;
        }
    }
}
