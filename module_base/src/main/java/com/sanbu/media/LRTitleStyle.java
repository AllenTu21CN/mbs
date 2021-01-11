package com.sanbu.media;

import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.StringUtil;

// 直播录制标题配置
public class LRTitleStyle {
    public String contentFormat;    // 标题格式
    private String fontFamily;      // 字体
    public FontSize fontSize;       // 字号
    public String fontColor;        // 字体颜色RGB码
    public String bgColor;          // 背景颜色RGB码
    public Integer bgTransparency;  // 背景颜色透明度 0~100%
    public Alignment alignment;     // 标题位置

    public LRTitleStyle(String contentFormat, String fontFamily, FontSize fontSize,
                        String fontColor, String bgColor, int bgTransparency,
                        Alignment alignment) {
        this.contentFormat = contentFormat;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.fontColor = fontColor;
        this.bgColor = bgColor;
        this.bgTransparency = bgTransparency;
        this.alignment = alignment;
    }

    public LRTitleStyle(LRTitleStyle other) {
        this(other.contentFormat, other.fontFamily, other.fontSize, other.fontColor, other.bgColor, other.bgTransparency, other.alignment);
    }

    public boolean isEqual(LRTitleStyle other) {
        return CompareHelper.isEqual(contentFormat, other.contentFormat) &&
                CompareHelper.isEqual(fontFamily, other.fontFamily) &&
                CompareHelper.isEqual(fontSize, other.fontSize) &&
                CompareHelper.isEqual(fontColor, other.fontColor) &&
                CompareHelper.isEqual(bgColor, other.bgColor) &&
                CompareHelper.isEqual(bgTransparency, other.bgTransparency) &&
                CompareHelper.isEqual(alignment, other.alignment);
    }

    public boolean isEmpty() {
        return StringUtil.isEmpty(contentFormat);
    }

    public String getFontFamily() {
        return StringUtil.isEmpty(fontFamily) || fontFamily.equals("default") ? null : fontFamily;
    }

    public String getFontSizeName() {
        return fontSize != null ? fontSize.name() : null;
    }

    public Integer getFontColor() {
        if (StringUtil.isEmpty(fontColor))
            return null;
        else
            return TextStyle.parseColor(fontColor);
    }

    public Integer getBgColor() {
        if (StringUtil.isEmpty(bgColor))
            return null;
        else
            return TextStyle.parseColor(bgColor);
    }

    public Integer getBgAlpha() {
        if (bgTransparency == null || bgTransparency > 100 || bgTransparency < 0)
            return null;

        int unTrans = 100 - bgTransparency;
        return 0xff * unTrans / 100;
    }

    public String getBgAlphaHex() {
        Integer tmp = getBgAlpha();
        if (tmp == null)
            return "FF";
        else
            return String.format("%02X", (byte) ((int) tmp));
    }

    public void setBgAlphaHex(String strHex) {
        int value = Integer.parseInt(strHex, 16);
        if (value >= 0 && value <= 255)
            bgTransparency = 100 - (value * 100 / 255);
    }

    public void update(LRTitleStyle other) {
        if (!StringUtil.isEmpty(other.contentFormat))
            contentFormat = other.contentFormat;
        if (!StringUtil.isEmpty(other.fontFamily))
            fontFamily = other.fontFamily;
        if (other.fontSize != null)
            fontSize = other.fontSize;
        if (!StringUtil.isEmpty(other.fontColor))
            fontColor = other.fontColor;
        if (!StringUtil.isEmpty(other.bgColor))
            bgColor = other.bgColor;
        if (other.bgTransparency != null && other.bgTransparency >= 0)
            bgTransparency = other.bgTransparency;
        if (other.alignment != null && other.alignment != Alignment.UNDEFINED)
            alignment = other.alignment;
    }
}
