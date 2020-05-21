package cn.sanbu.avalon.endpoint3.director.structures;

public class Event {
    public final int id;

    public int objId;
    public Object data;

    public Event(int id) {
        this.id = id;
        this.objId = -1;
        this.data = null;
    }

    public Event update(int objId, Object data) {
        this.objId = objId;
        this.data = data;
        return this;
    }
}
