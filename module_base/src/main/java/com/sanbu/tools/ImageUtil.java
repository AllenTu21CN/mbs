package com.sanbu.tools;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by zhangxd on 2019/2/26.
 */

public class ImageUtil {

    /**
     * @param bitmap the origin bitmap
     * @param w      the width of new Bitmap
     * @param h      the height of new Bitmap
     * @return new bitmap
     */
    public static Bitmap createBitmapInSize(Bitmap bitmap, int w, int h) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                matrix, true);
        return newBmp;
    }
}
