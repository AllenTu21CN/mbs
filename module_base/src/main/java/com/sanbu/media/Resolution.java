package com.sanbu.media;

import android.util.Size;

public enum Resolution {
    RES_4K("4k", 3840, 2160),
    RES_2K("2k", 2560, 1440),
    RES_1080P("1080p", 1920, 1080),
    RES_720P("720p", 1280, 720),
    RES_576P("576p", 1024, 576),
    RES_540P("540p", 960, 540),
    RES_480P("480p", 856, 480),
    RES_360P("360p", 640, 360),
    RES_4CIF("4cif", 704, 576),
    RES_CIF("cif", 352, 288),
    RES_MINI("mini", 178, 100),
    RES_UNKNOWN("", 0, 0);

    public String name;
    public int width;
    public int height;

    Resolution(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public Size toSize() {
        return new Size(width, height);
    }

    public long area() {
        return width * height;
    }

    static public Resolution fromName(String name) {
        if (name == null)
            return RES_UNKNOWN;
        for (Resolution resolution: Resolution.values()) {
            if (name.equals(resolution.name))
                return resolution;
        }
        return RES_UNKNOWN;
    }

    static public Resolution fromDetail(String detail) {
        if (detail == null)
            return RES_UNKNOWN;
        String[] items = detail.split("x");
        if (items.length != 2)
            return RES_UNKNOWN;

        try {
            return fromRes(Integer.valueOf(items[0]), Integer.valueOf(items[1]));
        } catch (NumberFormatException e) {
            return RES_UNKNOWN;
        }
    }

    static public Resolution fromRes(int width, int height) {
        if (width <= 0 || height <= 0)
            return RES_UNKNOWN;
        for (Resolution resolution: Resolution.values()) {
            if (width == resolution.width &&
                    height == resolution.height)
                return resolution;
        }
        if (width >= RES_4K.width)
            return RES_4K;
        else if (height <= RES_MINI.width)
            return RES_MINI;
        return RES_UNKNOWN;
    }

    static public Resolution fromSize(Size size) {
        return fromRes(size.getWidth(), size.getHeight());
    }
}
