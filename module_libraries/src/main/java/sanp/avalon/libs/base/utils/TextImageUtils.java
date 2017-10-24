package sanp.avalon.libs.base.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;

import sanp.avalon.libs.R;

/**
 * 将字符串生成图片工具类
 * Created by Tom on 2017/2/13.
 */

public class TextImageUtils {

    /* 字号选择*/
    public static int TEXT_SIZE_16;
    public static int TEXT_SIZE_18;
    public static int TEXT_SIZE_20;

    /*设置的字体大小*/
    private int textSize;

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public TextImageUtils(Context context) {
        textSize = (int) context.getResources().getDimension(R.dimen.text_size_22);
        TEXT_SIZE_18 = (int) context.getResources().getDimension(R.dimen.text_size_24);
    }

    /**
     * @param path 图片保存路径
     * @param data 要转成图片的字符串
     * @param num  一行显示的字数
     */
    public Bitmap writeImage(String path, String data, int num) {
        Bitmap bitmap = null;
        try {
            int height = 100;
            int width = data.length() * textSize + 80;
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.BLUE);   //背景颜色
            Paint p = new Paint();
            p.setColor(Color.WHITE);   //画笔颜色
            p.setTextSize(textSize);         //画笔粗细
            p.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(data, 40, 50 + (textSize / 2), p); // 绘制文本；文字距离左边距；文字顶部距离上边距；画笔；
            //将Bitmap保存为png图片
            File file = new File(path);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            LogManager.d("保存成功——" + path);
        } catch (Exception e) {
            LogManager.e("保存失败——" + path);
           LogManager.e(e);
        }
        return bitmap;
    }
}
