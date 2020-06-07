package com.sanbu.board;

import android.os.Build;
import android.os.SystemProperties;

/**
 * Created by Tuyj on 2017/12/12.
 */

public class Rockchip {

    private static String gHWVersion;
    private static final String g3BuVersion = SystemProperties.get("ro.sanbu.product.ver", "");

    public static boolean isRK3288() {
        String model = Build.MODEL;
        return (model.startsWith("rk3288") || model.startsWith("firefly"));
    }

    public static boolean isAIO3399() {
        String model = Build.MODEL;
        return (model.endsWith("3399J") || model.startsWith("AIO") || model.equals("rk3399-all"));
    }

    public static boolean is3BUVersion() {
        return Build.MODEL.contains("3399") && !g3BuVersion.isEmpty();
    }

    public static String getAIO3399HWVersion() {
        if (gHWVersion == null) {
            synchronized (Rockchip.class) {
                if (gHWVersion == null)
                    gHWVersion = SystemProperties.get("ro.sanbu.product.hardware.ver", "1.0");
            }
        }
        return gHWVersion;
    }

    public static int fixEncodingHeight(int height) {
        if (height == 1080)
            return 1088;
        else
            return height;
    }
}
