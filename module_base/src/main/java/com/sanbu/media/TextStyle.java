package com.sanbu.media;

import android.graphics.Color;
import android.graphics.Paint;

import com.sanbu.tools.CompareHelper;

public class TextStyle {

    private static final String SHEET_NAME_FONT_FAMILY      = "font-family";
    private static final String SHEET_NAME_FONT_SIZE        = "font-size";
    private static final String SHEET_NAME_FONT_COLOR       = "font-color";
    private static final String SHEET_NAME_BACKGROUND_COLOR = "background-color";

    public String font_family;      // 字体
    public String font_size;        // 字号
    public Integer font_color;      // 字体颜色
    public Integer bg_color;        // 字体背景色
    public Integer bg_alpha;        // 字体背景色不透明度
    public Paint.Style fit_style;   // 字体适应
    public Paint.Align layout_align;// 字体对齐
    // ShadowLayer;

    public TextStyle() {

    }

    public TextStyle(String fontFamily, String fontSize, Integer fontColor, Integer bgColor, Integer bgAlpha, Paint.Style fitStyle, Paint.Align layoutAlign) {
        this.font_family = fontFamily;
        this.font_size = fontSize;
        this.font_color = fontColor;
        this.bg_color = bgColor;
        this.bg_alpha = bgAlpha;
        this.fit_style = fitStyle;
        this.layout_align = layoutAlign;
    }

    public TextStyle(TextStyle other) {
        this(other.font_family, other.font_size, other.font_color, other.bg_color, other.bg_alpha, other.fit_style, other.layout_align);
    }

    public boolean isEqual(TextStyle other) {
        if (other == null)
            return false;
        return CompareHelper.isEqual(font_family, other.font_family) &&
                CompareHelper.isEqual(font_size, other.font_size) &&
                CompareHelper.isEqual(font_color, other.font_color) &&
                CompareHelper.isEqual(bg_color, other.bg_color) &&
                CompareHelper.isEqual(bg_alpha, other.bg_alpha) &&
                CompareHelper.isEqual(fit_style, other.fit_style) &&
                CompareHelper.isEqual(layout_align, other.layout_align);
    }

    public String toSheet() {
        // e.g. "font-family:"/storage/emulated/0/ts5000_jplb/resources/ts_fonts/宋体.otf";font-color:#ffffff;font-size:m;background-color:#000000;"

        String sheet = "";
        if (font_family != null)
            sheet += SHEET_NAME_FONT_FAMILY + ": '" + font_family + "';";
        if (font_size != null)
            sheet += SHEET_NAME_FONT_SIZE + ": " + font_size + ";";
        if (font_color != null)
            sheet += SHEET_NAME_FONT_COLOR + ": " + getRGBCode(font_color) + ";";
        if (bg_color != null)
            sheet += SHEET_NAME_BACKGROUND_COLOR + ": " + getRGBACode(bg_color, bg_alpha) + ";";
        // TODO: others
        return sheet;
    }

    public static TextStyle fromSheet(String sheet) {
        TextStyle style = new TextStyle();
        if (sheet == null)
            return style;

        try {
            String[] items = sheet.split(";");
            for (String item: items) {
                String[] kv = item.split(":", 1);
                if (kv.length != 2)
                    continue;
                String key = kv[0].trim();
                String value = kv[1].trim();
                if (key.equals(SHEET_NAME_FONT_FAMILY)) {
                    style.font_family = value;
                } else if (key.equals(SHEET_NAME_FONT_SIZE)) {
                    style.font_size = value;
                } else if (key.equals(SHEET_NAME_FONT_COLOR)) {
                    style.font_color = parseColor(value);
                } else if (key.equals(SHEET_NAME_BACKGROUND_COLOR)) {
                    style.bg_color = parseColor(value);
                    style.bg_alpha = parseAlpha(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return style;
    }

    public static int parseColor(String rgbCodeOrName) throws IllegalArgumentException {
        if (rgbCodeOrName == null)
            throw new IllegalArgumentException("invalid rgbCodeOrName: " + rgbCodeOrName);

        if (rgbCodeOrName.startsWith("#")) {
            int size = rgbCodeOrName.length();
            if (size == 7)
                return Color.parseColor(rgbCodeOrName);
            else if (size == 9)
                return Color.parseColor(rgbCodeOrName.substring(0, size - 2));
            else
                throw new IllegalArgumentException("invalid rgbCodeOrName: " + rgbCodeOrName);
        } else {
            return Color.parseColor(rgbCodeOrName);
        }
    }

    public static Integer parseAlpha(String rgba) throws IllegalArgumentException {
        if (rgba == null || !rgba.startsWith("#"))
            return null;

        int size = rgba.length();
        if (size != 9)
            return null;

        String hex = rgba.substring(size - 2, size);
        return Integer.parseInt(hex, 16);
    }

    public static String getRGBCode(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static String getRGBACode(int color, Integer alpha) {
        alpha = alpha == null ? 0xff : alpha;
        return getRGBCode(color) + String.format("%02X", 0xFF & alpha);
    }
}
