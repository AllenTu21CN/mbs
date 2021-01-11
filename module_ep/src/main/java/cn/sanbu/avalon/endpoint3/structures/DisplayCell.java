package cn.sanbu.avalon.endpoint3.structures;

import com.sanbu.media.Region;
import com.sanbu.tools.CompareHelper;

public class DisplayCell {
    // required
    public final boolean isStream;
    public final int index;
    // optional
    public float transparency;
    public Region srcRegion;
    public Title title;

    // for stream
    public int streamId;
    // for image
    public String imagePath;

    private DisplayCell(boolean isStream, int index, float transparency, Region srcRegion,
                        Title title, int streamId, String imagePath) {
        this.isStream = isStream;
        this.index = index;
        this.transparency = transparency;
        this.srcRegion = srcRegion;
        this.title = title;
        this.streamId = streamId;
        this.imagePath = imagePath;
    }

    public void setTransparency(float transparency) {
        this.transparency = transparency;
    }

    public void setSrcRegion(Region srcRegion) {
        this.srcRegion = srcRegion;
    }

    public boolean isEqual(DisplayCell other) {
        if (other == null)
            return false;

        return isStream == other.isStream &&
                index == other.index &&
                transparency == other.transparency &&
                CompareHelper.isEqual(srcRegion, other.srcRegion, (src, dst) -> srcRegion.isEqual(other.srcRegion)) &&
                CompareHelper.isEqual(title, other.title, (src, dst) -> title.isEqual(other.title)) &&
                streamId == other.streamId &&
                CompareHelper.isEqual(imagePath, other.imagePath);
    }

    public static DisplayCell buildStream(int index, int streamId) {
        return new DisplayCell(true, index, 0.0f, Region.buildFull(), null, streamId, null);
    }

    public static DisplayCell buildStream(int index, int streamId, Title title) {
        return new DisplayCell(true, index, 0.0f, Region.buildFull(), title, streamId, null);
    }

    public static DisplayCell buildImage(int index, String imagePath) {
        return new DisplayCell(false, index, 0.0f, Region.buildFull(), null, -1, imagePath);
    }

    public static DisplayCell buildImage(int index, String imagePath, Title title) {
        return new DisplayCell(false, index, 0.0f, Region.buildFull(), title, -1, imagePath);
    }
}
