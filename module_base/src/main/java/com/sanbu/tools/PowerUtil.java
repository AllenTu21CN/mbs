package com.sanbu.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PowerUtil {

    private static final String TAG = PowerUtil.class.getSimpleName();

    private static final String ACTION_REQUEST_SHUTDOWN_N = "android.intent.action.ACTION_REQUEST_SHUTDOWN";
    private static final String ACTION_REQUEST_SHUTDOWN_O = "com.android.internal.intent.action.REQUEST_SHUTDOWN";

    private static final String ACTION_REQUEST_SHUTDOWN = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 ? ACTION_REQUEST_SHUTDOWN_N : ACTION_REQUEST_SHUTDOWN_O;
    private static final String EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM";

    public static void reboot(Context context, boolean rightNow) {
        int uid = UserUtil.getCurrentUID(context);
        if (UserUtil.isSystem(uid)) {
            Intent intent = new Intent(Intent.ACTION_REBOOT);
            intent.putExtra("nowait", rightNow ? 1 : 0);
            intent.putExtra("interval", 1);
            intent.putExtra("window", 0);
            context.sendBroadcast(intent);
        } else {
            LocalLinuxUtil.Result result = LocalLinuxUtil.doCommandWithResult("reboot");
            if (result.code != 0) {
                LogUtil.w(TAG, "reboot failed: " + result.AllToString());

                result = LocalLinuxUtil.doCommandWithResultByRoot("reboot");
                if (result.code != 0)
                    LogUtil.w(TAG, "reboot with root failed: " + result.AllToString());
            }
        }
    }

    public static void shutdown(Context context, boolean shouldConfirm) {

        Intent intent = new Intent(ACTION_REQUEST_SHUTDOWN);
        intent.putExtra(EXTRA_KEY_CONFIRM, shouldConfirm);
        context.startActivity(intent);

        // or
        // LocalLinuxUtil.doCommand("reboot -p");
    }
}
