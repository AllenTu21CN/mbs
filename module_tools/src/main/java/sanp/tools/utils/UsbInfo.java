package sanp.tools.utils;

/**
 * Created by Vald on 2017/3/16.
 * USB EventBus信息类
 */

public class UsbInfo {

    /*u盘名*/
    String name;

    /*u盘容量*/
    String size;

    /*u盘已用*/
    String used;

    /*u盘可使用*/
    String freed;

    /*u盘路径*/
    String path;

    public UsbInfo(String name,String size,String used,String freed) {
        this.name = name;
        this.size = size;
        this.used = used;
        this.freed = freed;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUsed() {
        return used;
    }

    public void setUsed(String used) {
        this.used = used;
    }

    public String getFreed() {
        return freed;
    }

    public void setFreed(String freed) {
        this.freed = freed;
    }

    @Override
    public String toString() {
        return "[UsbInfo name=" + name + "," +
                " size=" + size + "," +
                " used=" + used + "," +
                " freed=" + freed + "," +
                " path=" + path + "]";
    }

}
