package cn.sanbu.avalon.endpoint3.structures.jni;

import com.google.gson.Gson;
import com.sanbu.tools.LogUtil;

import java.util.ArrayList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.EPConst;

public class CallingStatistics {
    public String session;
    public _Sinks sink;
    public _Sources source;

    private static class _Sinks {
        public List<_Sink> audio;
        public List<_Sink> video;
    }

    private static class _Sink {
        public int id;
        public String sink;
    }

    private static class _Sources {
        public List<_Source> audio;
        public List<_Source> video;
    }

    private static class _Source {
        public int id;
        public String source;
    }

    public static class ASS {
        public int id;
        public AudioSS impl;
    }

    public static class VSS {
        public int id;
        public VideoSS impl;
    }

    public static class SS {
        public List<ASS> audio;
        public List<VSS> video;
    }

    public static class Audio {
        public NetStatistics.AudioRx recv;
        public NetStatistics.AudioTx send;
    }

    public static class Video {
        public NetStatistics.VideoRx recv;
        public NetStatistics.VideoTx send;
    }

    public static class Session {
        public int bandwidth;       // 4096000
        public String protocol;     // sip

        public Audio audio;
        public Video video;
        public Video video_ext;
    }

    public static class AudioSS {
        public String codec;    // G7221C
        public int bitrate;     // 48018
        public int channels;    // 1
        public int sample_rate; // 32000
    }

    public static class VideoSS {
        public String codec;      // H264
        public int bitrate;       // 4072121
        public float frame_rate;  // 24.4
        public String resolution; // 1920x1080
        public String config;
    }

    public static class VideoCodec {
        public String profile;    // baseline or null
    }

    public static class Instance {
        public Session session;
        public SS sinks;
        public SS sources;
    }

    public Instance unserialize() {
        Instance instance = new Instance();
        Gson gson = new Gson();

        try {
            instance.session = gson.fromJson(session, Session.class);
            instance.sinks = new SS();
            instance.sources = new SS();

            if (sink == null || sink.audio == null) {
                instance.sinks.audio = new ArrayList<>();
            } else {
                instance.sinks.audio = new ArrayList<>(sink.audio.size());
                for (_Sink sink : sink.audio) {
                    ASS a = new ASS();
                    a.id = sink.id;
                    a.impl = gson.fromJson(sink.sink, AudioSS.class);
                    instance.sinks.audio.add(a);
                }
            }

            if (sink == null || sink.video == null) {
                instance.sinks.video = new ArrayList<>();
            } else {
                instance.sinks.video = new ArrayList<>(sink.video.size());
                for (_Sink sink : sink.video) {
                    VSS v = new VSS();
                    v.id = sink.id;
                    v.impl = gson.fromJson(sink.sink, VideoSS.class);
                    instance.sinks.video.add(v);
                }
            }

            if (source == null || source.audio == null) {
                instance.sources.audio = new ArrayList<>();
            } else {
                instance.sources.audio = new ArrayList<>(source.audio.size());
                for (_Source source : source.audio) {
                    ASS a = new ASS();
                    a.id = source.id;
                    a.impl = gson.fromJson(source.source, AudioSS.class);
                    instance.sources.audio.add(a);
                }
            }

            if (source == null || source.video == null) {
                instance.sources.video = new ArrayList<>();
            } else {
                instance.sources.video = new ArrayList<>(source.video.size());
                for (_Source source : source.video) {
                    VSS v = new VSS();
                    v.id = source.id;
                    v.impl = gson.fromJson(source.source, VideoSS.class);
                    instance.sources.video.add(v);
                }
            }

        } catch (Exception e) {
            LogUtil.d(EPConst.TAG, "invalid statistics: " + e.getMessage());
        }

        return instance;
    }
}
