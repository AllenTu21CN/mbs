package sanp.avalon.libs.base.bean.localbean;

/**
 * Created by Vald on 2017/6/6.
 */

public class EquipmentSystem {
    private String type;            //系统类型
    private String serialnumber;    //序列号
    private String version;         //系统版本
    private String ipaddr;          //ip地址

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSerialnumber() {
        return serialnumber;
    }

    public void setSerialnumber(String serialnumber) {
        this.serialnumber = serialnumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }


}
