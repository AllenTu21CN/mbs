package com.sanbu.board;

import android.os.Build;

public class Qualcomm {
    public static boolean isProduct() {
        String hardware = Build.HARDWARE;
        String manufacturer = Build.MANUFACTURER;
        hardware = hardware.toUpperCase();
        manufacturer = manufacturer.toUpperCase();
        return  (manufacturer.startsWith("QUALCOMM") || manufacturer.startsWith("QCOM") || manufacturer.startsWith("QC") ||
                hardware.startsWith("QUALCOMM") || hardware.startsWith("QCOM") || hardware.startsWith("QC"));
    }

    public static boolean isVT6105() {
        String model = Build.MODEL;
        return model.contains("VT6105");
    }

    public static int fixEncodingHeight(int height) {
        if (height == 1088)
            return 1080;
        else
            return height;
    }
}
