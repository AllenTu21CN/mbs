package cn.lx.mbs.support.structures;

import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.jni.EPDir;
import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;

public class AVStream extends StreamDesc {
    public final int id;
    public final Object format;

    // 被解码或编码
    private boolean processed;
    // 解码器(source)id
    private int decId;

    public AVStream(int id, DataType type, String name, String description, EPDir direction, Object format) {
        super(type, name, description, direction);
        this.id = id;
        this.format = format;
        this.processed = false;
        this.decId = -1;
    }

    public AVStream(int id, StreamDesc other, Object format) {
        super(other);
        this.id = id;
        this.format = format;
        this.processed = false;
        this.decId = -1;
    }

    public boolean isProcessed() {
        return processed;
    }

    public int getDecId() {
        return decId;
    }

    public void onDecoding(int decId) {
        this.processed = true;
        this.decId = decId;
    }

    public void onEncoding() {
        this.processed = true;
    }

    public void onStopped() {
        processed = false;
        decId = -1;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && id >= 0;
    }
}
