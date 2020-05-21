package cn.sanbu.avalon.endpoint3.structures;

import com.sanbu.tools.CompareHelper;

public class DisplayCell {
    // required
    public final boolean isStream;
    public final int index;
    // optional
    public float transparency;
    public Region srcRegion;

    // for stream
    public int streamId;
    public Title title;

    // for image
    public String imagePath;

    private DisplayCell(boolean isStream, int index, float transparency, Region srcRegion,
                        int streamId, Title title, String imagePath) {
        this.isStream = isStream;
        this.index = index;
        this.transparency = transparency;
        this.srcRegion = srcRegion;
        this.streamId = streamId;
        this.title = title;
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

                streamId == other.streamId &&
                CompareHelper.isEqual(title, other.title, (src, dst) -> title.isEqual(other.title)) &&

                CompareHelper.isEqual(imagePath, other.imagePath);
    }

    public static DisplayCell buildStream(int index, int streamId) {
        return new DisplayCell(true, index, 0.0f, Region.buildFull(), streamId, null, null);
    }

    public static DisplayCell buildStream(int index, int streamId, Title title) {
        return new DisplayCell(true, index, 0.0f, Region.buildFull(), streamId, title, null);
    }

    public static DisplayCell buildImage(int index, String imagePath) {
        return new DisplayCell(false, index, 0.0f, Region.buildFull(), -1, null, imagePath);
    }
}
