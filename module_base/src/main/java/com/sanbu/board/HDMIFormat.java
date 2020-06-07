package com.sanbu.board;

public class HDMIFormat {
    public int width;
    public int height;
    public float refreshRate;

    public HDMIFormat() {
        width = 0;
        height = 0;
        refreshRate = 0;
    }

    public HDMIFormat(int width, int height, float refreshRate) {
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
    }

    public boolean isEqual(HDMIFormat other) {
        return width == other.width && height == other.height && refreshRate == other.refreshRate;
    }

    public String toString() {
        return String.format("width: %d, height: %d, refreshRate: %f", width, height, refreshRate);
    }
}
