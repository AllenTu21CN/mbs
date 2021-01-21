package cn.sanbu.avalon.media;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;

public class TextRenderer {

    private static final boolean CALC_PADDING = true;

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
        int padding = CALC_PADDING ? (int)(fontSize / 2.f) : 0;
        String[] lines = text.replace("\r", "").split("\n");
        int width = 0;
        for (String line: lines) {
            int w = (int) (textPaint.measureText(line) + padding + 0.5f); // paddind + round
            width = Math.max(w, width);
        }

        if (width <= 0)
            width = (int) (textPaint.measureText(text) + 0.5f); // round;

        width = Math.min(width, MAX_WIDTH_OF_TEXT);

        // init a multiple-lines layout
        StaticLayout layout = new StaticLayout(text, textPaint, width,
                Layout.Alignment.ALIGN_CENTER, 1.0F, 0.0F, !CALC_PADDING);

        // create a Bitmap with the layout
        int canvasWidth = layout.getWidth();
        int canvasHeight = layout.getHeight();
        Bitmap image = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.TRANSPARENT);

        // create a Canvas
        Canvas canvas = new Canvas(image);

        // draw round-rect
        canvas.drawRoundRect(new RectF(0, 0, canvasWidth, canvasHeight), RADIUS, RADIUS, bgPaint);

        // draw text
        layout.draw(canvas);

        return image;
    }

    public static Bitmap renderTextAsBitmap(String text,
                                            String fontFamily, int textColor,
                                            boolean bold, boolean italic, boolean underlined,
                                            Layout.Alignment alignment,
                                            int bgColor, int bgBorderRadius,
                                            int targetWidth, int targetHeight) {
        int fontStyle;
        if (bold) {
            fontStyle = italic ? Typeface.BOLD_ITALIC : Typeface.BOLD;
        } else {
            fontStyle = italic ? Typeface.ITALIC : Typeface.NORMAL;
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

        if (font == null) {
            return null;
        }

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(font);
        textPaint.setColor(textColor);
        textPaint.setStyle(Paint.Style.FILL);
        if (underlined) {
            textPaint.setFlags(TextPaint.UNDERLINE_TEXT_FLAG);
        }

        if (textColor == Color.TRANSPARENT) {
            textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        float textSize = 48.0f;
        StaticLayout layout;
        while (true) {
            textPaint.setTextSize(textSize);
            String[] lines = text.replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .split("\n");
            float maxWidth = 0.0f;
            for (String line : lines) {
                maxWidth = Math.max(textPaint.measureText(line), maxWidth);
            }

            float desiredTextSize = textSize * (float)targetWidth / (float)maxWidth;
            if (Math.abs(desiredTextSize - textSize) >= 1.0f) {
                textSize = desiredTextSize;
                continue;
            }

            StaticLayout.Builder lb = StaticLayout.Builder.obtain(
                    text, 0, text.length(), textPaint, (int)maxWidth);
            lb.setAlignment(alignment);
            lb.setText(text);
            // TODO: Add line spacing parameters.
            layout = lb.build();
            //if (layout.getHeight() > targetHeight) {
            //    textSize = textSize * (float)targetHeight / (float)(layout.getHeight());
            //    continue;
            //}
            break;
        }

        int canvasWidth = layout.getWidth();
        int canvasHeight = layout.getHeight();

        Bitmap image = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(image);

        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(bgColor);
        canvas.drawRoundRect(new RectF(
                0, 0, canvasWidth, canvasHeight), bgBorderRadius, bgBorderRadius, bgPaint);

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
