package cn.sanbu.avalon.director.structures;

public class StaticEvent {
    public final String name;
    public final int id;
    public final String value;
    public final String top;
    public final String disable;

    public StaticEvent(String name, int id, String value, String top, String disable) {
        this.name = name;
        this.id = id;
        this.value = value;
        this.top = top;
        this.disable = disable;
    }
}
