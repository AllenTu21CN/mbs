package com.sanbu.tools;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetUtils {

    private static final String TAG = AssetUtils.class.getSimpleName();

    private static final int BYTE_BUF_SIZE = 2048;

    /**
     * Copies a file from assets.
     *
     * @param context   application context used to discover assets.
     * @param assetPath the relative file name within assets.
     * @param targetDir the target file name, always over write the existing file.
     * @throws IOException if operation fails.
     */
    public static int copy(Context context, String assetPath, String targetDir) {

        Log.d(TAG, "creating file " + targetDir + " from " + assetPath);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            AssetManager assets = context.getAssets();
            String[] fileArray = assets.list(assetPath);
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
            }
            for (String path : fileArray) {
                File targetFile = new File(targetDir, path);
                if (targetFile.exists()) {
                    return 1;
                }
                targetFile.createNewFile();
                inputStream = assets.open(assetPath + "/" + path);
                Log.d(TAG, "Creating outputstream");
                outputStream = new FileOutputStream(targetFile, false /* append */);
                copy(inputStream, outputStream);
            }
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private static void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[BYTE_BUF_SIZE];
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
        }
    }
}
