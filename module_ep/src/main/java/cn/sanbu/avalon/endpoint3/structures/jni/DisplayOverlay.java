package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

import cn.sanbu.avalon.endpoint3.structures.Alignment;
import cn.sanbu.avalon.endpoint3.structures.Region;
import cn.sanbu.avalon.endpoint3.structures.TextStyle;
import cn.sanbu.avalon.endpoint3.structures.Title;

public interface DisplayOverlay {

    boolean isStream();
    Region getSrcRegion();
    void setSrcRegion(Region region);
    Region getDstRegion();
    void setDstRegion(Region region);
    int getZIndex();
    void setZIndex(int index);
    float getTransparency();
    void setTransparency(float transparency);
    boolean isEqual(DisplayOverlay other);

    class Stream implements DisplayOverlay {
        private final String type;
        private final int source_id;

        private String src_rect; // <left:top:width:height>, float: 0.0~1.0
        private String dst_rect; // <left:top:width:height>, float: 0.0~1.0
        private int z_index;     // (int: 0~9999)
        private float opacity;   // is transparency (float: 0.0~1.0)

        // display name
        private Boolean display_name_visible;
        private String display_name;
        private String display_name_alignment;
        private String display_name_style_sheet;

        public Stream(int id, Region dst) {
            this.type = "stream";
            this.source_id = id;
            this.src_rect = Region.buildFull().toString();
            this.dst_rect = dst.toString();
            this.z_index = 0;
            this.opacity = 0.0f;
        }

        @Override
        public boolean isStream() {
            return true;
        }

        @Override
        public Region getSrcRegion() {
            return Region.fromString(src_rect);
        }

        @Override
        public void setSrcRegion(Region region) {
            this.src_rect = region.toString();
        }

        @Override
        public Region getDstRegion() {
            return Region.fromString(dst_rect);
        }

        @Override
        public void setDstRegion(Region region) {
            this.dst_rect = region.toString();
        }

        @Override
        public int getZIndex() {
            return z_index;
        }

        @Override
        public void setZIndex(int index) {
            this.z_index = index;
        }

        @Override
        public float getTransparency() {
            return opacity;
        }

        @Override
        public void setTransparency(float transparency) {
            this.opacity = transparency;
        }

        @Override
        public boolean isEqual(DisplayOverlay other) {
            if (other == null || !other.isStream())
                return false;

            Stream o = (Stream) other;
            return (source_id == o.source_id &&
                    CompareHelper.isEqual(src_rect, o.src_rect) &&
                    CompareHelper.isEqual(dst_rect, o.dst_rect) &&
                    z_index == o.z_index && opacity == o.opacity &&
                    CompareHelper.isEqual(display_name_visible, o.display_name_visible) &&
                    CompareHelper.isEqual(display_name, o.display_name) &&
                    CompareHelper.isEqual(display_name_alignment, o.display_name_alignment) &&
                    CompareHelper.isEqual(display_name_style_sheet, o.display_name_style_sheet)
            );
        }

        public int getStreamId() {
            return source_id;
        }

        public Title getTitle() {
            return display_name_visible == null ? null : new Title(display_name,
                    Alignment.fromString(display_name_alignment),
                    TextStyle.fromSheet(display_name_style_sheet),
                    display_name_visible);
        }

        public Stream setTitle(Title title) {
            if (title != null) {
                display_name = title.content;
                display_name_alignment = title.alignment.toString();
                display_name_style_sheet = title.textStyle.toSheet();
                display_name_visible = title.visible;

                if (!display_name_visible)
                    display_name = "";
            }
            return this;
        }

        public Stream setTitle(boolean visible, String content,
                               String alignment, String style_sheet) {
            display_name = content;
            display_name_alignment = alignment;
            display_name_style_sheet = style_sheet;
            display_name_visible = visible;

            if (!display_name_visible)
                display_name = "";
            return this;
        }
    }

    class Image implements DisplayOverlay {
        private final String type;
        private final String image_path;

        private String src_rect;  // <left:top:width:height>, float: 0.0~1.0
        private String dst_rect;  // <left:top:width:height>, float: 0.0~1.0
        private int z_index;      // (int: 0~9999)
        private float opacity;    // is transparency, (float: 0.0~1.0)

        public Image(String imagePath, Region dst) {
            this.type = "image";
            this.image_path = imagePath;
            this.src_rect = Region.buildFull().toString();
            this.dst_rect = dst.toString();
            this.z_index = 0;
            this.opacity = 0.0f;
        }

        @Override
        public boolean isStream() {
            return false;
        }

        @Override
        public Region getSrcRegion() {
            return Region.fromString(src_rect);
        }

        @Override
        public void setSrcRegion(Region region) {
            this.src_rect = region.toString();
        }

        @Override
        public Region getDstRegion() {
            return Region.fromString(dst_rect);
        }

        @Override
        public void setDstRegion(Region region) {
            this.dst_rect = region.toString();
        }

        @Override
        public int getZIndex() {
            return z_index;
        }

        @Override
        public void setZIndex(int index) {
            this.z_index = index;
        }

        @Override
        public float getTransparency() {
            return opacity;
        }

        @Override
        public void setTransparency(float transparency) {
            this.opacity = transparency;
        }

        @Override
        public boolean isEqual(DisplayOverlay other) {
            if (other == null || other.isStream())
                return false;

            Image o = (Image) other;
            return (CompareHelper.isEqual(image_path, o.image_path) &&
                    CompareHelper.isEqual(src_rect, o.src_rect) &&
                    CompareHelper.isEqual(dst_rect, o.dst_rect) &&
                    z_index == o.z_index && opacity == o.opacity
            );
        }

        public String getImagePath() {
            return image_path;
        }
    }
}