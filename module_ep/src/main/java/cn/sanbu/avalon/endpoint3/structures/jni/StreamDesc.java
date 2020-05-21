package cn.sanbu.avalon.endpoint3.structures.jni;

// description of stream
public class StreamDesc {
    public final DataType type;
    public final String name;
    public final String description;
    public final EPDir direction;

    public StreamDesc(DataType type, String name, String description, EPDir direction) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.direction = direction;
    }

    public StreamDesc(StreamDesc other) {
        this(other.type, other.name, other.description, other.direction);
    }

    public boolean isValid() {
        return type != null && direction != null;
    }
}
