package com.sanbu.board;

import android.os.Build;

public class HuaWei {
    public static boolean isProduct() {
        return Build.BRAND.toUpperCase().contains("HUAWEI");
    }
}
