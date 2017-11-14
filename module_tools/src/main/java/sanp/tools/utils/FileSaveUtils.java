package sanp.tools.utils;

import android.content.Context;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Tom on 2017/4/25.
 */

public class FileSaveUtils {

    public static void saveToSDCard(final Context context, final String path, final String name, final int resouce) throws Throwable {
        ExecutorThreadUtil.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    saveToSDCard(context, path, name, resouce, false);
                } catch (Throwable throwable) {
                    LogManager.e("saveToSDCard error " + throwable);
                }
            }
        });
    }

    public static void saveToSDCard(Context context, String path, String name, int resouce, boolean forced) throws Throwable {
        File file = new File(path, name);
        if (file.exists() && !forced)
            return;
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();

        int len;
        byte[] buffer = new byte[10];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        InputStream inStream = context.getResources().openRawResource(resouce);
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);//存入SDCard
        fileOutputStream.write(outStream.toByteArray());
        outStream.close();
        inStream.close();
        fileOutputStream.flush();
        fileOutputStream.close();
    }

}
