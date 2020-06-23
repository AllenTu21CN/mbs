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

    /*
    public void checkPermissions() {
        String[] requiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE };

        List<String> ungrantedPermissions = new LinkedList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ungrantedPermissions.add(permission);
            }
        }

        if (ungrantedPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    ungrantedPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    */
}
