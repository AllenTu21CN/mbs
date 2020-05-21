package cn.sanbu.avalon.endpoint3.structures;

public class AudioOutputDevice {
    public final int id;
    public final String url;
    public final String name;
    public final String description;

    public AudioOutputDevice(int id, String url, String name, String description) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.description = description;
    }
}
