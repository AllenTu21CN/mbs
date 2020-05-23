package cn.lx.mbs.impl.structures;

public class BasicSummary {
    // 自定义名称
    public String customName;
    // 自定义是否可修改
    public boolean lockCustomName;
    // 应用名称
    public String appName;
    // 序列号
    public String serialNumber;
    // 版本号
    public String versionCode;
    // 是否授权
    public boolean granted;

    public BasicSummary(String customName, boolean lockCustomName, String appName,
                        String serialNumber, String versionCode, boolean granted) {
        this.customName = customName;
        this.lockCustomName = lockCustomName;
        this.appName = appName;
        this.serialNumber = serialNumber;
        this.versionCode = versionCode;
        this.granted = granted;
    }

    public BasicInfo getBasicInfo() {
        return new BasicInfo(customName, lockCustomName);
    }

    public static BasicSummary buildEmpty(String customName) {
        return new BasicSummary(customName, false, "N/A", "N/A", "N/A", false);
    }
}
