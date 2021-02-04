package cn.lx.mbs.ui.model;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import com.google.gson.Gson;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.lx.mbs.support.structures.Layout;
import cn.lx.mbs.ui.view.Utils;

public class SceneOverlayDataModel {

    public static class Overlay {
        public final static int TYPE_GROUP = 1;
        public final static int TYPE_VIDEO = 2;
        public final static int TYPE_IMAGE = 3;
        public final static int TYPE_TEXT = 4;

        //public String name;
        public int type;
        public boolean isVisiable;
        public boolean isLocked;

        //public Bitmap thumbnailBitmap;

        public Overlay() {
            // TODO:
        }

        public int getType() {
            return type;
        }

        public String getTitle() {
            return "";
        }

        public Bitmap getThumbnail(int width, int height) {
            // TODO: Cache thumbnail bitmap for performance optimization
            final int CELL_SIZE = Utils.PX(5);
            return Utils.generateCheckerBoardBitmap(width, height, CELL_SIZE, CELL_SIZE);
        }

    } // End of class Overlay

    /*public static class GroupOverlay extends Overlay {
        // TODO:
        public List<Overlay> children;

        public GroupOverlay() {
            type = TYPE_GROUP;
        }
    }*/

    public static class VideoOverlay extends Overlay {
        // TODO:
        public int inputChannelId;

        public VideoOverlay() {
            type = TYPE_VIDEO;
        }
    }

    public static class ImageOverlay extends Overlay {
        public String originalFilePath;
        public Bitmap orignalBitmap;    // Cached if original file missing

        public RectF dstRect;
        public float rotateAngle;
        public float opacity;

        public ImageOverlay() {
            type = TYPE_IMAGE;
        }

        @Override
        public String getTitle() {
            if (originalFilePath != null && !originalFilePath.isEmpty()) {
                Path path = Paths.get(originalFilePath);
                return path.getFileName().toString();
            } else {
                return "<No image>";
            }
        }

    }

    public static class TextOverlay extends Overlay {
        public String text;
        public String fontFamily;
        public boolean isBold;
        public boolean isItalic;
        public boolean isUnderlined;
        public android.text.Layout.Alignment alignment;
        public Color textColor;
        public Color backgroundColor;
        public float backgroundOpacity;
        public float backgroundRadius;

        public RectF dstRect;
        public float rotateAngle;
        public Bitmap renderedBitmap;

        public TextOverlay() {
            type = TYPE_TEXT;
        }

        @Override
        public String getTitle() {
            if (text != null && !text.isEmpty()) {
                return text;
            } else {
                return "<No text>";
            }
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

    public void swap(int from, int to) {
        Collections.swap(list, from, to);
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

    public Layout toEpLayout() {
        // TODO:
        Layout layout = new Layout();

        // TODO: Construct ep overlays here

        return layout;
    }
}
