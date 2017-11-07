package sanp.tools.utils;

import android.os.Build;

/**
 * Created by Tuyj on 2017/7/31.
 */

public class PlatformUtils {
    static public final boolean TestingNonNativePlatform = true;

    public enum Type {
        PLATFORM_IS_PHONE,
        PLATFORM_IS_TV,
        PLATFORM_IS_PAD,
    };

    static public Type type() {
        String model = Build.MODEL;
        if(model.startsWith("firefly") || model.startsWith("rk3399") || model.startsWith("rk3288")) {
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
}
