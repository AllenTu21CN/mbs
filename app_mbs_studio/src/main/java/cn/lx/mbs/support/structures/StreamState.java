package cn.lx.mbs.support.structures;

import com.sanbu.media.DataType;

public class StreamState {
    public ChannelId channelId;
    public DataType type;
    public boolean ready;

    public StreamState(ChannelId channelId, DataType type, boolean ready) {
        this.channelId = channelId;
        this.type = type;
        this.ready = ready;
    }
}
