package cn.lx.mbs.support.structures;

import com.sanbu.media.DataType;
import com.sanbu.network.CallingDir;

import cn.sanbu.avalon.endpoint3.structures.jni.StreamDesc;

public class AVStream extends StreamDesc {
    public final int id;
    public final Object format;

    // 开始编码;解码出有效数据
    private boolean ready;
    // 解码器(source)id
    private int decId;

    public AVStream(int id, DataType type, String name, String description, CallingDir direction, Object format) {
        super(type, name, description, direction);
        this.id = id;
        this.format = format;
        this.ready = false;
        this.decId = -1;
    }

    public AVStream(int id, StreamDesc other, Object format) {
        super(other);
        this.id = id;
        this.format = format;
        this.ready = false;
        this.decId = -1;
    }

    public void onStopped() {
        this.ready = false;
        this.decId = -1;
    }

    public void onEncoding() {
        this.ready = true;
    }

    public boolean isEncoding() {
        return ready;
    }

    public void setDecId(int decId) {
        this.decId = decId;
    }

    public int getDecId() {
        return decId;
    }

    public void setDecReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isDecReady() {
        return decId >= 0 && ready;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && id >= 0;
    }
}
