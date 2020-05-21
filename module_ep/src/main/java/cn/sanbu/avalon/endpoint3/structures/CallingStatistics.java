package cn.sanbu.avalon.endpoint3.structures;

import cn.sanbu.avalon.endpoint3.structures.jni.NetStatistics;

public class CallingStatistics {
    public String protocol;    // 呼叫信令:"h323"
    public int bandwidth;      // 呼叫带宽:2048000

    public NetStatistics.AudioTx audioTx;    // 音频发送统计
    public NetStatistics.AudioRx audioRx;    // 音频接收统计

    public NetStatistics.VideoTx videoTx;    // 主流发送统计
    public NetStatistics.VideoRx videoRx;    // 主流接收统计

    public NetStatistics.VideoTx videoExtTx; // 辅流发送统计
    public NetStatistics.VideoRx videoExtRx; // 辅流接收统计

    public static CallingStatistics buildEmpty() {
        CallingStatistics statistics = new CallingStatistics();
        statistics.protocol = "N/A";
        statistics.bandwidth = 0;
        return statistics;
    }

    public static CallingStatistics build(cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics statistics) {
        CallingStatistics result = new CallingStatistics();

        cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.Instance instance = statistics.unserialize();
        if (instance.session == null)
            return result;

        result.protocol = instance.session.protocol;
        result.bandwidth = instance.session.bandwidth;

        if (instance.session.audio != null) {
            result.audioRx = instance.session.audio.recv;
            result.audioTx = instance.session.audio.send;
        }

        if (instance.session.video != null) {
            result.videoRx = instance.session.video.recv;
            result.videoTx = instance.session.video.send;
        }

        if (instance.session.video_ext != null) {
            result.videoExtRx = instance.session.video_ext.recv;
            result.videoExtTx = instance.session.video_ext.send;
        }

        if (result.videoTx != null && instance.sinks.video.size() > 0) {
            cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VSS vss = instance.sinks.video.get(0);
            result.videoTx.resolution = vss.impl.resolution;
            result.videoTx.framerate = vss.impl.frame_rate;
            // if (vss.impl.config != null) {
            //     cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec codec = new Gson().fromJson(vss.impl.config,
            //             cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec.class);
            //     if (codec.profile != null)
            //         result.videoTx.profile = codec.profile;
            // }
        }

        if (result.videoRx != null && instance.sources.video.size() > 0) {
            cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VSS vss = instance.sources.video.get(0);
            result.videoRx.resolution = vss.impl.resolution;
            result.videoRx.framerate = vss.impl.frame_rate;
            // if (vss.impl.config != null) {
            //     cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec codec = new Gson().fromJson(vss.impl.config,
            //             cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec.class);
            //     if (codec.profile != null)
            //         result.videoRx.profile = codec.profile;
            // }
        }

        if (result.videoExtTx != null && instance.sinks.video.size() > 1) {
            cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VSS vss = instance.sinks.video.get(1);
            result.videoExtTx.resolution = vss.impl.resolution;
            result.videoExtTx.framerate = vss.impl.frame_rate;
            // if (vss.impl.config != null) {
            //     cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec codec = new Gson().fromJson(vss.impl.config,
            //             cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec.class);
            //     if (codec.profile != null)
            //         result.videoExtTx.profile = codec.profile;
            // }
        }

        if (result.videoExtRx != null && instance.sources.video.size() > 1) {
            cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VSS vss = instance.sources.video.get(1);
            result.videoExtRx.resolution = vss.impl.resolution;
            result.videoExtRx.framerate = vss.impl.frame_rate;
            // if (vss.impl.config != null) {
            //     cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec codec = new Gson().fromJson(vss.impl.config,
            //             cn.sanbu.avalon.endpoint3.structures.jni.CallingStatistics.VideoCodec.class);
            //     if (codec.profile != null)
            //         result.videoExtRx.profile = codec.profile;
            // }
        }

        return result;
    }
}
