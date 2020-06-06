package cn.lx.media;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleSheet {
    static final Map<String, int[]> predefinedColors = new HashMap<String, int[]>() {{
                                                                   // #RRGGBBAA
        put("white",       new int[] { 0xff, 0xff, 0xff, 0xff });  // #ffffffff
        put("black",       new int[] { 0x00, 0x00, 0x00, 0xff });  // #000000ff
        put("red",         new int[] { 0xff, 0x00, 0x00, 0xff });  // #ff0000ff
        put("darkRed",     new int[] { 0x80, 0x00, 0x00, 0xff });  // #800000ff
        put("green",       new int[] { 0x00, 0xff, 0x00, 0xff });  // #00ff00ff
        put("darkGreen",   new int[] { 0x00, 0x80, 0x00, 0xff });  // #008000ff
        put("blue",        new int[] { 0x00, 0x00, 0xff, 0xff });  // #0000ffff
        put("darkBlue",    new int[] { 0x00, 0x00, 0x80, 0xff });  // #000080ff
        put("cyan",        new int[] { 0x00, 0xff, 0xff, 0xff });  // #00ffffff
        put("darkCyan",    new int[] { 0x00, 0x80, 0x80, 0xff });  // #008080ff
        put("magenta",     new int[] { 0xff, 0x00, 0xff, 0xff });  // #ff00ffff
        put("darkMagenta", new int[] { 0x80, 0x00, 0x80, 0xff });  // #800080ff
        put("yellow",      new int[] { 0xff, 0xff, 0x00, 0xff });  // #ffff00ff
        put("darkYellow",  new int[] { 0x80, 0x80, 0x00, 0xff });  // #808000ff
        put("gray",        new int[] { 0xa0, 0xa0, 0xa4, 0xff });  // #a0a0a4ff
        put("darkGray",    new int[] { 0x80, 0x80, 0x80, 0xff });  // #808080ff
        put("lightGray",   new int[] { 0xc0, 0xc0, 0xc0, 0xff });  // #c0c0c0ff
        put("transparent", new int[] { 0x00, 0x00, 0x00, 0x00 });  // #00000000
    }};

    private int     mBackgroundColor = Color.argb(0, 0, 0, 0);
    private String  mBackgroundImage = null;
    private int     mFontColor       = Color.argb(255, 255, 255, 255);
    private String  mFontFamily      = null;
    private boolean mFontItalic      = false;
    private int     mFontSize        = 12;
    private int     mFontWeight      = -1;

    public StyleSheet() {

    }

    public StyleSheet(String text) {
        String[] propExps = {
            "((background-color)\\s*:\\s*(#[0-9a-fA-F]{6,8})\\s*;)",
            "((background-image)\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*;)",
            "((font-color)\\s*:\\s*(#[0-9a-fA-F]{6,8})\\s*;)",
            "((font-italic)\\s*:\\s*(true|false)\\s*;)",
            "((font-family)\\s*:\\s*'([^']+)'\\s*;)",
            "((font-size)\\s*:\\s*([0-9]+)px\\s*;)",
            "((font-weight)\\s*:\\s*([0-9]+)\\s*;)",
        };

        for (String exp : propExps) {
            Pattern p = Pattern.compile(exp);
            Matcher m = p.matcher(text);
            if (m.find() && m.groupCount() > 2) {
                String name = m.group(2);
                String value = m.group(3);
                if (name.equalsIgnoreCase("background-color")) {
                    mBackgroundColor = parseColor(value);
                } else if (name.equalsIgnoreCase("background-image")) {
                    mBackgroundImage = value;
                } else if (name.equalsIgnoreCase("font-color")) {
                    mFontColor = parseColor(value);
                } else if (name.equalsIgnoreCase("font-italic")) {
                    mFontItalic = (value.equalsIgnoreCase("true"));
                } else if (name.equalsIgnoreCase("font-family")) {
                    mFontFamily = value;
                } else if (name.equalsIgnoreCase("font-size")) {
                    mFontSize = Integer.parseInt(value);
                } else if (name.equalsIgnoreCase("font-weight")) {
                    mFontWeight = Integer.parseInt(value);
                }
            }
        }
    }

    public int backgroundColor() {
        return mBackgroundColor;
    }

    public String backgroundImage() {
        return mBackgroundImage;
    }

    public int fontColor() {
        return mFontColor;
    }

    public String fontFamily() {
        return mFontFamily;
    }

    public boolean fontItalic() {
        return mFontItalic;
    }

    public int fontSize() {
        return mFontSize;
    }

    public int fontWeight() {
        return mFontWeight;
    }

    private int parseColor(String name) {
        // #RRGGBBAA or #RRGGBB
        String str = "000000FF";
        if ('#' == name.charAt(0) && (7 == name.length() || 9 == name.length())) {
            str = name.substring(1);
            if (str.length() == 6) str += "FF";
        } else {
            for (Map.Entry<String, int[]> entry : predefinedColors.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    int[] c = entry.getValue();
                    return Color.argb(c[3], c[0], c[1], c[2]);
                }
            }
        }

        long d       = Long.parseLong(str, 16);
        int red      = (int)(d >> 24) & 0xFF;
        int green    = (int)(d >> 16) & 0xFF;
        int blue     = (int)(d >> 8) & 0xFF;
        int alpha    = (int)(d) & 0xFF;

        return Color.argb(alpha, red, green, blue);
    }
}
