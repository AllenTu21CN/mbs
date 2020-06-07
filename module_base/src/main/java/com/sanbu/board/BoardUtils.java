package com.sanbu.board;

import android.os.Build;
import android.os.SystemProperties;

import java.lang.reflect.Method;

/**
 * Created by Tuyj on 2017/7/31.
 */

public class BoardUtils {
    public enum Type {
        PLATFORM_IS_PHONE,
        PLATFORM_IS_TV,
        PLATFORM_IS_PAD,
    }

    static public Type type() {
        String model = Build.MODEL;
        if(Rockchip.is3BUVersion()) {
            return Type.PLATFORM_IS_TV;
        } else if (model.contains("PAD") || model.contains("pad")) {
            return Type.PLATFORM_IS_PAD;
        } else {
            return Type.PLATFORM_IS_PHONE;
        }
    }

    static public boolean isProductNativeSupported() {
        return type() == Type.PLATFORM_IS_TV;
    }

    static public boolean isSupporting4KEncoding() {
        if (Qualcomm.isVT6105())
            return true;
        else
            return false;
    }

    static public boolean isSupporting4KDecoding() {
        if (Qualcomm.isVT6105())
            return true;
        else
            return false;
    }

    static public boolean isSupporting4KCapture() {
        if (Qualcomm.isVT6105())
            return true;
        else
            return false;
    }

    static public boolean isSupportingKVM() {
        if (Qualcomm.isVT6105())
            return true;
        else
            return false;
    }

    static public boolean UsingGLLinear() {
        if (Qualcomm.isVT6105())
            return true;
        else
            return false;
    }

    static public String getHWSerialNumber() {
        String SN = SystemProperties.get("ro.serialno", null);
        if (SN == null)
            SN = getHWSerialNumberByReflect();
        if (SN == null)
            SN = "N/A";
        return SN;
    }

    private static String getHWSerialNumberByReflect() {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class);
            return (String) get.invoke(clazz, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
