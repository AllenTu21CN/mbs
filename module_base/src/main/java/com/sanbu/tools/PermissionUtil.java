package com.sanbu.tools;

import android.app.Activity;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.Observable;
import rx.functions.Action1;

public class PermissionUtil {

    public interface Callback {
        void done(boolean granted);
    }

    public static void checkPermissions(final Activity context, String[] permissions, final Callback callback) {
        RxPermissions rxPermissions = new RxPermissions(context);
        Observable<Boolean> b = rxPermissions.request(permissions);
        b.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean granted) {
                callback.done(granted);
            }
        });
    }

    public static boolean isGranted(final Activity context, String[] permissions) {
        RxPermissions rxPermissions = new RxPermissions(context);
        for (String permission: permissions) {
            if (!rxPermissions.isGranted(permission))
                return false;
        }
        return true;
    }
}
