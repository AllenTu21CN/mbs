package cn.lx.mbs.support.structures;

import cn.sanbu.avalon.endpoint3.structures.jni.DataType;

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
