package cn.lx.mbs.impl.structures;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sanbu.tools.RSAUtil;

public class License {

    private String code;

    @SerializedName("serial_number")
    public final String SN;

    @SerializedName("vaild_date")
    public final long expirationDate;

    @SerializedName("app_type")
    public final String modeCode;

    public License(String code, String SN, long expirationDate, TSMode mode) {
        this.code = code;
        this.SN = SN;
        this.expirationDate = expirationDate;
        this.modeCode = mode.code;
    }

    public static License fromCode(String code) {
        try {
            String json = RSAUtil.decryptByString(code);
            License license = new Gson().fromJson(json, License.class);
            license.code = code;
            return license;
        } catch (Exception e) {
            return null;
        }
    }

    public String getCode() {
        return code;
    }

    public TSMode getTSMode() {
        return TSMode.fromCode(modeCode);
    }

    public boolean inExpirationDate() {
        return expirationDate < 0 || (System.currentTimeMillis() / 1000) < expirationDate;
    }

    public boolean isGranted() {
        return getTSMode().granted && inExpirationDate();
    }
}
