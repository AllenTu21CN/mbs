package sanp.avalon.libs.base.bean.eventbean;

import java.util.List;

/**
 * Created by Vald on 2017/3/16.
 * 蓝牙 EventBus信息类
 */

public class BluetoothInfo {
    //设备列表
    List<String> bluetoothDevices;
    String command;

    public BluetoothInfo(List<String> bluetoothDevices) {
        this.bluetoothDevices = bluetoothDevices;
    }

    public BluetoothInfo(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }



    public List<String> getBluetoothDevices() {
        return bluetoothDevices;
    }

    public void setBluetoothDevices(List<String> bluetoothDevices) {
        this.bluetoothDevices = bluetoothDevices;
    }





}
