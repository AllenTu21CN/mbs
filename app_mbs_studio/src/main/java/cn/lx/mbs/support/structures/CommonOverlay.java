package cn.lx.mbs.support.structures;

import android.graphics.Bitmap;

import com.sanbu.tools.CompareHelper;

import com.sanbu.media.Region;

public class CommonOverlay {

    public OverlaySrc src;
    public OverlayDst dst;

    public CommonOverlay(OverlaySrc src, OverlayDst dst) {
        this.src = src;
        this.dst = dst;
    }

    public boolean isEqual(CommonOverlay other) {
        return (other != null &&
                CompareHelper.isEqual(src, other.src, (src1, dst1) -> src.isEqual(other.src)) &&
                CompareHelper.isEqual(dst, other.dst, (src1, dst1) -> dst.isEqual(other.dst))
        );
    }

    public boolean isValid() {
        return src != null && src.isValid() &&
                dst != null && dst.isValid();
    }

    public static CommonOverlay buildStream(ChannelId channel, Region dst) {
        return new CommonOverlay(OverlaySrc.buildStream(channel), new OverlayDst(dst));
    }

    public static CommonOverlay buildImage(String imagePath, Region dst) {
        return new CommonOverlay(OverlaySrc.buildImage(imagePath), new OverlayDst(dst));
    }

    public static CommonOverlay buildBitmap(Bitmap bitmap, Region dst) {
        return new CommonOverlay(OverlaySrc.buildBitmap(bitmap), new OverlayDst(dst));
    }
}