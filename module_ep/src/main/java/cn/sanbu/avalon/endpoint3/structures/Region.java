package cn.sanbu.avalon.endpoint3.structures;

public class Region {
    public double x;
    public double y;
    public double width;
    public double height;

    public Region(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Region(Region other) {
        this(other.x, other.y, other.width, other.height);
    }

    public boolean isEqual(Region other) {
        return (x == other.x &&
                y == other.y &&
                width == other.width &&
                height == other.height);
    }

    public boolean isValid() {
        return x >= 0 && y >= 0 && width > 0 && height > 0;
    }

    public boolean isFullSize() {
        return x == 0.0f && y == 0.0f && width == 1.0f && height == 1.0f;
    }

    public String toString() {
        return String.format("%.8f:%.8f:%.8f:%.8f", x, y, width, height);
    }

    public static Region fromString(String region) {
        String[] ps = region.split(":");
        if (ps.length != 4)
            return null;
        return new Region(Float.parseFloat(ps[0]), Float.parseFloat(ps[1]), Float.parseFloat(ps[2]), Float.parseFloat(ps[3]));
    }

    public static Region buildFull() {
        return new Region(0.0f, 0.0f, 1.0f, 1.0f);
    }
}
