package cn.sanbu.avalon.endpoint3.structures.jni;

public enum EPDir {
    None("none"),
    Outgoing("tx"),
    Incoming("rx");

    public final String name;

    EPDir(String name) {
        this.name = name;
    }

    public static EPDir fromName(final String name) {
        if (name == null)
            return null;
        for (EPDir dir: EPDir.values()) {
            if (name.equals(dir.name))
                return dir;
        }
        return null;
    }
}
