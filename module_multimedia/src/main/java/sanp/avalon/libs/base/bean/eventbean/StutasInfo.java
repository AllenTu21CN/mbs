package sanp.avalon.libs.base.bean.eventbean;

/**
 * Created by Tom on 2017/2/25.
 */

public class StutasInfo {
    /*更新*/
    boolean update;
    /*激活*/
    boolean activity;
    /*  网络*/
    boolean network;
    /*注册*/
    boolean regist;
    /*usb接入*/
    boolean usb;

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isActivity() {
        return activity;
    }

    public void setActivity(boolean activity) {
        this.activity = activity;
    }

    public boolean isNetwork() {
        return network;
    }

    public void setNetwork(boolean network) {
        this.network = network;
    }

    public boolean isRegist() {
        return regist;
    }

    public void setRegist(boolean regist) {
        this.regist = regist;
    }

    public boolean isUsb() {
        return usb;
    }

    public void setUsb(boolean usb) {
        this.usb = usb;
    }
}
