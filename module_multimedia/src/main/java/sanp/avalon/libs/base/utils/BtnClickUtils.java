package sanp.avalon.libs.base.utils;

/**
 * Created by Tom on 2017/4/1.
 * 短时间内onclik 响应一次
 */

public class BtnClickUtils {
    private static long mLastClickTime = 0;

    private BtnClickUtils() {

    }

    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - mLastClickTime;
        if (800 < timeD) {
            mLastClickTime = time;
            return true;
        }
        return false;
    }
}