package cn.lx.mbs.support.structures;

import com.sanbu.base.State;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.EPConst;
import cn.sanbu.avalon.endpoint3.structures.Region;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;

public class Input {
    public final Source config;
    public final ChannelId channelId;

    public int epId;
    public State connState;

    public float volume;
    public boolean muted;

    public Region srcRegion;

    public List<AVStream> aStreams = new LinkedList<>();
    public List<AVStream> vStreams = new LinkedList<>();

    public Input(Source config, ChannelId channelId, int epId,
                 State connState, float volume, boolean muted) {
        this.config = config;
        this.channelId = channelId;
        this.epId = epId;
        this.connState = connState;
        this.volume = volume;
        this.muted = muted;
        this.srcRegion = Region.buildFull();
    }

    public AVStream removeStream(DataType type, int streamId) {
        List<AVStream> streams = type == DataType.AUDIO ? aStreams : vStreams;
        Iterator<AVStream> it = streams.iterator();
        while (it.hasNext()) {
            AVStream stream = it.next();
            if (streamId == stream.id) {
                it.remove();
                return stream;
            }
        }

        return null;
    }

    public List<Integer> getDecodingIds(DataType type) {
        List<AVStream> streams = type == DataType.AUDIO ? aStreams : vStreams;
        List<Integer> ids = new LinkedList<>();
        for (AVStream stream: streams) {
            if (stream.isDecReady())
                ids.add(stream.getDecId());
        }
        return ids;
    }

    public AVStream getDecodedAudioStream() {
        for (AVStream stream: aStreams) {
            if (stream.isDecReady())
                return stream;
        }

        return null;
    }

    public AVStream findStreamByDecId(DataType type, int decId) {
        List<AVStream> streams = type == DataType.AUDIO ? aStreams : vStreams;
        for (AVStream stream: streams) {
            if (stream.getDecId() == decId)
                return stream;
        }
        return null;
    }
}
