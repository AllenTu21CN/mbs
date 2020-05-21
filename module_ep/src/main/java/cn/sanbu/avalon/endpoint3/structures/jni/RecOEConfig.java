package cn.sanbu.avalon.endpoint3.structures.jni;

// opening and ending for recording
public class RecOEConfig {
    public static class Item {
        public final String resource_path;
        public final int duration_sec;

        public Item(String resource_path, int duration_sec) {
            this.resource_path = resource_path;
            this.duration_sec = duration_sec;
        }
    }

    public final Item opening;
    public final Item ending;

    public RecOEConfig(Item opening, Item ending) {
        this.opening = opening;
        this.ending = ending;
    }
}
