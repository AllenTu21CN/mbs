package cn.lx.mbs.impl.structures;

import com.sanbu.tools.TimeUtil;

public class LicenseSummary {
    // 序列号
    public String serialNumber;
    // 授权状态
    public boolean granted;
    // 应用类型
    public String modeDesc;
    // 授权码
    public String licenseCode;
    // 有效日期
    public String expirationDate;

    public LicenseSummary(String serialNumber, boolean granted,
                          String modeDesc, String licenseCode, String expirationDate) {
        this.serialNumber = serialNumber;
        this.granted = granted;
        this.modeDesc = modeDesc;
        this.licenseCode = licenseCode;
        this.expirationDate = expirationDate;
    }

    public static LicenseSummary buildEmpty(String SN) {
        return new LicenseSummary(SN, false, "未授权", "", "N/A");
    }

    public static LicenseSummary build(License license) {
        TSMode mode = license.getTSMode();
        String date;
        if (mode.granted) {
            if (license.expirationDate > 0) {
                date = TimeUtil.stampToDate(license.expirationDate, TimeUtil.FORMAT_2);
                if (!license.inExpirationDate())
                    date += "(已过期)";
            } else {
                date = "永久";
            }
        } else {
            date = "N/A";
        }

        return new LicenseSummary(license.SN, license.isGranted(), mode.desc, license.getCode(), date);
    }
}
