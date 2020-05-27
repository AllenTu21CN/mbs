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
    public final ChannelId inId;
    public final Source config;

    public int epId;
    public State connState;

    public boolean micMuted;
    public float micVolume;

    public Region srcRegion;

    public List<AVStream> aStreams = new LinkedList<>();
    public List<AVStream> vStreams = new LinkedList<>();

    public Input(ChannelId id, Source config) {
        this.inId = id;
        this.config = config;

        this.epId = -1;
        this.connState = State.None;
        this.micMuted = false;
        this.micVolume = EPConst.EP_DEFAULT_AUDIO_VOLUME;
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

    public AVStream getVideoStream() {
        if (vStreams.size() == 0)
            return null;
        return vStreams.get(0);
    }

    public AVStream getAudioStream() {
        if (aStreams.size() == 0)
            return null;
        return aStreams.get(0);
    }

    public VideoFormat getVideoFormat() {
        if (vStreams.size() == 0)
            return null;
        return (VideoFormat) vStreams.get(0).format;
    }

    public AudioFormat getAudioFormat() {
        if (aStreams.size() == 0)
            return null;
        return (AudioFormat) aStreams.get(0).format;
    }
}
