package com.sanbu.tools;

import android.content.Context;

import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Runnable3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileSaveUtil {

    public enum Action {
        Forced, // force to override the file
        Simple, // give up writing if there is a file with the same name
        Smart,  // give up writing if there is a file with the same name and same MD5
    }

    public static final int CODE_EXISTED = 1;
    public static final int CODE_COPIED  = 0;
    public static final int CODE_ERROR   = -1;

    public static void saveResToStorage(final Context context, final int fromRes, final String toFilename, final Action action, final Callback callback) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = saveResToStorage(context, fromRes, toFilename, action);
                    if (callback != null)
                        callback.done(new Result(ret, null));
                } catch (Throwable throwable) {
                    String message = "saveResToStorage[" + toFilename + "] error: " + throwable.getMessage();
                    LogUtil.w(message);
                    if (callback != null)
                        callback.done(new Result(CODE_ERROR, message, throwable));
                }
            }
        });
        executorService.shutdown();
    }

    public static int saveResToStorage(final Context context, final int fromRes, String toFilename, Action action) {
        Runnable3 getInputMD5 = new Runnable3() {
            @Override
            public Result run() {
                String md5 = MD5Util.getResourceMD5(context, fromRes);
                return Result.buildSuccess(md5);
            }
        };

        Runnable3 getInputStream = new Runnable3() {
            @Override
            public Result run() {
                try {
                    InputStream inStream = context.getResources().openRawResource(fromRes);
                    return Result.buildSuccess(inStream);
                } catch (Throwable throwable) {
                    return new Result(BaseError.INTERNAL_ERROR, null, throwable);
                }
            }
        };

        return saveToStorage(getInputMD5, getInputStream, toFilename, action);
    }

    public static void saveAssetToStorage(final Context context, final String assetFile, final String toFilename, final Action action, final Callback callback) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = saveAssetToStorage(context, assetFile, toFilename, action);
                    if (callback != null)
                        callback.done(new Result(ret, null));
                } catch (Throwable throwable) {
                    String message = "saveAssetToStorage[" + toFilename + "] error: " + throwable.getMessage();
                    LogUtil.w(message);
                    if (callback != null)
                        callback.done(new Result(CODE_ERROR, message, throwable));
                }
            }
        });
        executorService.shutdown();
    }

    public static int saveAssetToStorage(final Context context, final String assetFile, String toFilename, Action action) {
        Runnable3 getInputMD5 = new Runnable3() {
            @Override
            public Result run() {
                String md5 = MD5Util.getAssetMD5(context, assetFile);
                return Result.buildSuccess(md5);
            }
        };

        Runnable3 getInputStream = new Runnable3() {
            @Override
            public Result run() {
                try {
                    InputStream inStream = context.getAssets().open(assetFile);
                    return Result.buildSuccess(inStream);
                } catch (IOException e) {
                    return new Result(BaseError.INTERNAL_ERROR, null, e);
                }
            }
        };

        return saveToStorage(getInputMD5, getInputStream, toFilename, action);
    }

    public static void asyncCopyAssets(final Context context, final String assetPath, final String dir, final Callback callback) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = AssetUtils.copy(context, assetPath, dir);
                    if (callback != null)
                        callback.done(new Result(ret, null));
                } catch (Throwable throwable) {
                    String message = "asyncCopyAssets[" + assetPath + "/" + dir + "] error: " + throwable.getMessage();
                    LogUtil.w(message);
                    if (callback != null)
                        callback.done(new Result(-1, message, throwable));
                }
            }
        });
        executorService.shutdown();
    }

    private static int saveToStorage(Runnable3 getInputMD5, Runnable3 getInputStream, String toFilename, Action action) {
        int code;
        OutputStream outStream = null;
        InputStream inStream = null;

        try {
            File file = new File(toFilename);
            if (file.exists()) {
                if (action == Action.Simple) {
                    return CODE_EXISTED;
                } else if (action == Action.Smart) {
                    String oldMD5 = MD5Util.getFileMD5(file);
                    if (oldMD5 == null) {
                        LogUtil.e("get old md5 failed");
                        return CODE_ERROR;
                    }

                    Result result = getInputMD5.run();
                    if (!result.isSuccessful() || result.data == null) {
                        LogUtil.e("get new md5 failed: " + result.getMessage());
                        return CODE_ERROR;
                    }
                    String nowMD5 = (String) result.data;

                    if (oldMD5.equals(nowMD5))
                        return CODE_EXISTED;
                } else {
                    // forced
                }
            }
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();

            Result result = getInputStream.run();
            if (!result.isSuccessful() || result.data == null) {
                LogUtil.e("get input stream failed: " + result.getMessage());
                return CODE_ERROR;
            }
            inStream = (InputStream) result.data;

            int len;
            byte[] buffer = new byte[1024];
            outStream = new FileOutputStream(file);
            while ((len = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }

            code = CODE_COPIED;
        } catch (Throwable throwable) {
            LogUtil.e("saveResToStorage error", throwable);
            code = CODE_ERROR;
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outStream != null) {
                try {
                    outStream.flush();
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return code;
    }
}
