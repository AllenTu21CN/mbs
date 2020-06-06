package cn.lx.mbs.ui.model;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cn.lx.mbs.ui.view.Utils;

public class SceneOverlayDataModel {

    public static class Overlay {
        public final static int TYPE_GROUP = 1;
        public final static int TYPE_VIDEO = 2;
        public final static int TYPE_IMAGE = 3;
        public final static int TYPE_TEXT = 4;

        public String name;
        public int type;
        public boolean isVisiable;
        public boolean isLocked;

        public RectF srcRect;
        public RectF dstRect;
        public float rotateAngle;
        public float opacity;

        public Bitmap thumbnailBitmap;

        public Overlay() {
            // TODO:
        }

        public void updateThumbnail(int width, int height) {
            // Draw checkerborad
            final int CELL_SIZE = Utils.PX(5);
            thumbnailBitmap = Utils.generateCheckerBoardBitmap(width, height, CELL_SIZE, CELL_SIZE);

            // TODO: Draw content on top
        }

    } // End of class Overlay

    public static class GroupOverlay extends Overlay {
        // TODO:
        public List<Overlay> children;

        public GroupOverlay() {
            type = TYPE_GROUP;
        }
    }

    public static class VideoOverlay extends Overlay {
        // TODO:
        public int inputChannelId;

        public VideoOverlay() {
            type = TYPE_VIDEO;
        }
    }

    public static class ImageOverlay extends Overlay {
        // TODO:
        public String filePath;
        public Bitmap orignalBitmap;

        public ImageOverlay() {
            type = TYPE_IMAGE;
        }
    }

    public static class TextOverlay extends Overlay {
        // TODO:
        public String text;
        public float textSize;
        public String font;
        public int backgroundColor;
        public float backgroundOpacity;
        public int[] paddings;
        public Bitmap renderedBitmap;

        public TextOverlay() {
            type = TYPE_TEXT;
        }
    }

    private List<Overlay> list = new LinkedList<>();

    public SceneOverlayDataModel() { }

    public Overlay getItem(int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int size() {
        return list.size();
    }

    public void add(Overlay item) {
        list.add(item);
    }

    public void remove(int index) {
        list.remove(index);
    }

    public void clear() {
        list.clear();
    }

    public boolean fromJson(String json) {
        Gson gson = new Gson();

        Overlay[] data = gson.fromJson(json, Overlay[].class);
        if (data != null) {
            list.clear();
            list = Arrays.asList(data);
            return true;
        }

        return false;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(toArray());
    }

    public Overlay[] toArray() {
        return list.toArray(new Overlay[list.size()]);
    }
}
