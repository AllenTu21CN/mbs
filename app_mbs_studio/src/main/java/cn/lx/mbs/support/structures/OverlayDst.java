package cn.lx.mbs.support.structures;

import com.sanbu.media.Region;
import com.sanbu.tools.CompareHelper;

public class OverlayDst {
    public Region region;
    public int zIndex;         //  0~9999
    public float transparency; // 0.0~1.0

    public OverlayDst(Region region) {
        this(region, 0, 0.0f);
    }

    public OverlayDst(Region region, int zIndex, float transparency) {
        this.region = region;
        this.zIndex = zIndex;
        this.transparency = transparency;
    }

    public boolean isEqual(OverlayDst other) {
        return (other != null &&
                CompareHelper.isEqual(region, other.region, (src1, dst1) -> region.isEqual(other.region)) &&
                zIndex == other.zIndex &&
                transparency == other.transparency
        );
    }

    public boolean isValid() {
        return region != null && region.isValid() &&
                zIndex >= 0 && zIndex <= 9999 &&
                transparency >= 0.0f && transparency <= 1.0;
    }
}
