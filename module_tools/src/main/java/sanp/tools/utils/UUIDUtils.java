package sanp.tools.utils;

import java.util.UUID;

/**
 * Created by zhangxd on 2017/10/25.
 */

public class UUIDUtils {

    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }
}
