package com.sanbu.board;

public class HDMIFormat {
    public int width;
    public int height;
    public float refresh_rate;

    public HDMIFormat() {}

    public HDMIFormat(int w, int h, float r) {
        width = w;
        height = h;
        refresh_rate = r;
    }

    public String toString() {
        return "width:" + width + ", height:" + height + ", refresh_rate:" + refresh_rate;
    }
}
