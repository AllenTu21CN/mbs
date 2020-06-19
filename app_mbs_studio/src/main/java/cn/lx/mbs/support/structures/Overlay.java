package cn.lx.mbs.support.structures;

import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.StringUtil;

import cn.sanbu.avalon.endpoint3.structures.Region;

public abstract class Overlay {

    public final Type type;

    public Region dst;
    public int zIndex;         //  0~9999
    public float transparency; // 0.0~1.0

    private Overlay(Type type, Region dst) {
        this.type = type;
        this.dst = new Region(dst);
        this.zIndex = 0;
        this.transparency = 0.0f;
    }

    public boolean isEqual(Overlay other) {
        return (other != null &&
                CompareHelper.isEqual(type, other.type) &&
                dst.isEqual(other.dst) &&
                zIndex == other.zIndex &&
                transparency == other.transparency
        );
    }

    public boolean isValid() {
        return type != null && dst.isValid() &&
                zIndex >= 0 && zIndex <= 9999 &&
                transparency >= 0.0f && transparency <= 1.0;
    }

    public enum Type {
        Stream,
        Image,
        Bitmap
    }

    public static class Stream extends Overlay {
        public final ChannelId channel;

        public Stream(ChannelId channel, Region dst) {
            super(Type.Stream, dst);
            this.channel = channel;
        }

        public boolean isEqual(Stream other) {
            return super.isEqual(other) && CompareHelper.isEqual(channel, other.channel);
        }

        @Override
        public boolean isValid() {
            return super.isValid() && channel != null;
        }
    }

    public static class Image extends Overlay {
        public final String imagePath;
        public final Region src;

        public Image(String imagePath, Region dst) {
            super(Type.Image, dst);
            this.imagePath = imagePath;
            this.src = Region.buildFull();
        }

        public Image(String imagePath, Region src, Region dst) {
            super(Type.Image, dst);
            this.imagePath = imagePath;
            this.src = src;
        }

        public boolean isEqual(Image other) {
            return super.isEqual(other) &&
                    CompareHelper.isEqual(imagePath, other.imagePath) &&
                    src.isEqual(other.src);
        }

        @Override
        public boolean isValid() {
            return super.isValid() && !StringUtil.isEmpty(imagePath) &&
                    src != null && src.isValid();
        }
    }

    public static class Bitmap extends Overlay {
        public final Bitmap bitmap;

        public Bitmap(Bitmap bitmap, Region dst) {
            super(Type.Bitmap, dst);
            this.bitmap = bitmap;
        }

        public boolean isEqual(Bitmap other) {
            return super.isEqual(other) && bitmap == other.bitmap;
        }

        @Override
        public boolean isValid() {
            return super.isValid() && bitmap != null;
        }
    }
}