package cn.sanbu.avalon.endpoint3.ext.camera;

public enum IxNChannelMode {
    Fixed(0),
    AutoInFullView(1),
    AutoInCloseUpView(2);

    public final int id;

    IxNChannelMode(int id) {
        this.id = id;
    }
}
