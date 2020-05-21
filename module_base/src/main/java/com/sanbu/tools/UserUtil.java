package com.sanbu.tools;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class UserUtil {

    private static final String TAG = UserUtil.class.getSimpleName();

    public static final int UID_INVALID = -1;

    // copy from include/private/android_filesystem_config.h
    private static final int UID_ROOT    = 0;
    private static final int UID_SYSTEM  = 1000;
    private static final int UID_SHELL   = 2000;

    public static int getCurrentUID(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.uid;
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.w(TAG, "getCurrentUID failed: " + e.getLocalizedMessage());
            return -1;
        }
    }

    public static boolean isValid(int uid) {
        return uid >= 0;
    }

    public static boolean isRoot(int uid) {
        return uid == UID_ROOT;
    }

    public static boolean isSystem(int uid) {
        return uid == UID_SYSTEM;
    }

    public static boolean isShell(int uid) {
        return uid == UID_SHELL;
    }
}
