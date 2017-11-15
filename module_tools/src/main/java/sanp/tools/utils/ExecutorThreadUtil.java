package sanp.tools.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhangxd on 2017/7/26.
 */

public class ExecutorThreadUtil {

    public static void submit(Runnable runnable){
        ExecutorService mExecutorService= Executors.newCachedThreadPool();
        mExecutorService.submit(runnable);
        mExecutorService.shutdown();
    }
}
