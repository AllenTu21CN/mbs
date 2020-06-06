package cn.lx.media;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;

public class TextRenderer {

    public static Bitmap renderTextAsBitmap(String text,
                                            String fontFamily, float fontSize,
                                            int fontWeight, boolean fontItalic,
                                            int fontColor, int bgColor) {
        return renderTextAsBitmap(text, fontFamily, fontSize, fontWeight, fontItalic, fontColor, bgColor, 1920);
    }

    public static Bitmap renderTextAsBitmap(String text,
                                            String fontFamily, float fontSize,
                                            int fontWeight, boolean fontItalic,
                                            int fontColor, int bgColor, int widthOfCanvas) {
        final float RADIUS = 5.0f;
        final int MAX_WIDTH_OF_TEXT = widthOfCanvas - (10 * 2);

        // init background paint
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(bgColor);

        // init font
        int fontStyle;
        Typeface font;
        if (fontWeight <= 100) {
            fontStyle = fontItalic ? Typeface.ITALIC : Typeface.NORMAL;
        } else {
            fontStyle = fontItalic ? Typeface.BOLD_ITALIC : Typeface.BOLD;
        }
        if (fontFamily != null && !fontFamily.isEmpty()) {
            if (fontFamily.startsWith("/") && new File(fontFamily).isFile()) {
                font = Typeface.create(Typeface.createFromFile(fontFamily), fontStyle);
            } else {
                font = Typeface.create(fontFamily, fontStyle);
            }
        } else {
            font = Typeface.defaultFromStyle(fontStyle);
        }

        // init text paint
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(font);
        textPaint.setColor(fontColor);
        textPaint.setStyle(Paint.Style.FILL);
        //textPaint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // calculate the max width with all lines
        String[] lines = text.replace("\r", "").split("\n");
        int width = 0;
        for (String line: lines) {
            int w = (int) (textPaint.measureText(line) + 0.5f); // round
            if (w > width)
                width = w;
        }
        if (width <= 0)
            width = (int) (textPaint.measureText(text) + 0.5f); // round;
        if (width > MAX_WIDTH_OF_TEXT)
            width = MAX_WIDTH_OF_TEXT;

        // init a multiple-lines layout
        StaticLayout layout = new StaticLayout(text, textPaint, width,
                Layout.Alignment.ALIGN_CENTER, 1.0F, 0.0F, true);

        // create a Bitmap with the layout
        Bitmap image = Bitmap.createBitmap(layout.getWidth(), layout.getHeight(), Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.TRANSPARENT);

        // create a Canvas
        Canvas canvas = new Canvas(image);

        // draw round-rect
        canvas.drawRoundRect(new RectF(0, 0, layout.getWidth(), layout.getHeight()), RADIUS, RADIUS, bgPaint);

        // draw text
        layout.draw(canvas);

        return image;
    }

    @Deprecated
    public static Bitmap renderTextAsBitmap_bak(String text,
                                             String fontFamily, float fontSize,
                                             int fontWeight, boolean fontItalic,
                                             int fontColor, int bgColor) {
        final int[] PADDINGS = new int[] { 5, 10, 5, 10 }; // top, right, bottom, left
        final float RADIUS = 5.0f;

        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(bgColor);

        int fontStyle;
        if (fontWeight <= 100) {
            fontStyle = fontItalic ? Typeface.ITALIC : Typeface.NORMAL;
        } else {
            fontStyle = fontItalic ? Typeface.BOLD_ITALIC : Typeface.BOLD;
        }

        Typeface font;
        if (fontFamily != null && !fontFamily.isEmpty()) {
            if (fontFamily.startsWith("/") && new File(fontFamily).isFile()) {
                font = Typeface.create(Typeface.createFromFile(fontFamily), fontStyle);
            } else {
                font = Typeface.create(fontFamily, fontStyle);
            }
        } else {
            font = Typeface.defaultFromStyle(fontStyle);
        }

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(font);
        textPaint.setColor(fontColor);
        textPaint.setStyle(Paint.Style.FILL);
        //textPaint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);

        float baseline = -textPaint.ascent(); // ascent() is negative
        int width = (int) (textPaint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + textPaint.descent() + 0.5f);

        // Adjust with paddings
        width += PADDINGS[3] + PADDINGS[1];     // left + right
        height += PADDINGS[0] + PADDINGS[2];    // top + bottom
        baseline += (float)PADDINGS[0];

        // Allocate image and prepare canvas
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(image);

        // Draw background
        canvas.drawRoundRect(new RectF(0, 0, width, height), RADIUS, RADIUS, bgPaint);

        // Draw text
        canvas.drawText(text, PADDINGS[3], baseline, textPaint);

        return image;
    }
}
