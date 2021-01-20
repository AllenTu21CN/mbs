package cn.lx.mbs.support.structures;

import android.graphics.Bitmap;

import com.sanbu.media.Region;
import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.StringUtil;

public class OverlaySrc {

    public enum Type {
        Stream,
        Image,
        Bitmap
    }

    public final Type type;
    public Region region;

    // only for stream
    public final ChannelId channel;

    // only for image
    public final String imagePath;

    // only for bitmap
    public final Bitmap bitmap;

    private OverlaySrc(Type type, ChannelId channel, String imagePath, Bitmap bitmap) {
        this.type = type;
        this.channel = channel;
        this.imagePath = imagePath;
        this.bitmap = bitmap;
        this.region = Region.buildFull();
    }

    public boolean isEqual(OverlaySrc other) {
        return (other != null &&
                CompareHelper.isEqual(type, other.type) &&
                CompareHelper.isEqual(region, other.region, (src1, dst) -> region.isEqual(other.region)) &&
                CompareHelper.isEqual(channel, other.channel) &&
                CompareHelper.isEqual(imagePath, other.imagePath) &&
                CompareHelper.isEqual(bitmap, other.bitmap, (src1, dst) -> bitmap == other.bitmap)
        );
    }

    public boolean isValid() {
        return type != null && region != null &&
                !(channel == null && StringUtil.isEmpty(imagePath) && bitmap == null);
    }

    public OverlaySrc setSrcRegion(Region region) {
        this.region = region;
        return this;
    }

    public static OverlaySrc buildEmpty() {
        return new OverlaySrc(Type.Stream, null, null, null);
    }

    public static OverlaySrc buildStream(ChannelId channel) {
        return new OverlaySrc(Type.Stream, channel, null, null);
    }

    public static OverlaySrc buildImage(String imagePath) {
        return new OverlaySrc(Type.Image, null, imagePath, null);
    }

    public static OverlaySrc buildBitmap(Bitmap bitmap) {
        return new OverlaySrc(Type.Bitmap, null, null, bitmap);
    }
}
