package com.sanbu.tools;

import android.os.SystemProperties;

import com.sanbu.base.BaseError;
import com.sanbu.base.Result;

public class RKOTA {

    private static final String TAG = RKOTA.class.getSimpleName();

    public static final String PRODUCT_OTA_HOST_PROP = "persist.product.ota.host";
    private static final String USER_OTA_HOST_PROP = "persist.user.ota.host";
    private static final String CURRENT_OTA_HOST_PROP = "ro.product.ota.host";
    private static final String DEFAULT_OTA_HOST = "ota.3bu.cn:2300";

    public static Result init(String defaultHost) {
        String currentHost = SystemProperties.get(CURRENT_OTA_HOST_PROP, null);
        LogUtil.d(TAG, CURRENT_OTA_HOST_PROP + ": " + currentHost);

        String productHost = SystemProperties.get(PRODUCT_OTA_HOST_PROP, null);
        LogUtil.d(TAG, PRODUCT_OTA_HOST_PROP + ": " + productHost);

        String userHost = SystemProperties.get(USER_OTA_HOST_PROP, null);
        LogUtil.d(TAG, USER_OTA_HOST_PROP + ": " + userHost);
        if (userHost == null || userHost.isEmpty())
            userHost = defaultHost == null ? DEFAULT_OTA_HOST : defaultHost;

        if (productHost == null || !userHost.equals(productHost)) {
            String message = "set " + PRODUCT_OTA_HOST_PROP + "to " + userHost;
            try {
                SystemProperties.set(PRODUCT_OTA_HOST_PROP, userHost);
                LogUtil.d(TAG, message);
            } catch (Exception e) {
                message += " error: " + e.getMessage();
                LogUtil.w(TAG, message);
                e.printStackTrace();
                return new Result(BaseError.INTERNAL_ERROR, message, userHost);
            }
        }

        return Result.SUCCESS;
    }

}
