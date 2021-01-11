package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.media.Alignment;
import com.sanbu.media.Region;
import com.sanbu.media.TextStyle;
import com.sanbu.tools.CompareHelper;

import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.Title;

public class DisplayOverlay {

    public enum Type {
        stream,
        image,
        custom
    }

    private final Type type;
    // only for stream overlay
    private final Integer source_id;
    // only for image overlay
    private final String image_path;
    // only for custom overlay
    private final String shader;

    // regions for stream or image
    private String src_rect;  // <left:top:width:height>, float: 0.0~1.0
    private String dst_rect;  // <left:top:width:height>, float: 0.0~1.0
    private int z_index;      // (int: 0~9999)
    private float opacity;    // is transparency (float: 0.0~1.0)

    // display name
    private Boolean display_name_visible;
    private String display_name;
    private String display_name_alignment;
    private String display_name_style_sheet;

    // values for custom
    private List<Integer> sources;
    private List<Float> variables;

    private DisplayOverlay(Type type, Integer streamId, String imagePath, String shaderName) {
        this.type = type;
        this.source_id = streamId;
        this.image_path = imagePath;
        this.shader = shaderName;
        this.z_index = 0;
        this.opacity = 0.0f;
    }

    public static DisplayOverlay buildStream(int streamId, Region dst) {
        DisplayOverlay overlay = new DisplayOverlay(Type.stream, streamId, null, null);
        overlay.setSrcRegion(Region.buildFull());
        overlay.setDstRegion(dst);
        return overlay;
    }

    public static DisplayOverlay buildImage(String imagePath, Region dst) {
        DisplayOverlay overlay = new DisplayOverlay(Type.image, null, imagePath, null);
        overlay.setSrcRegion(Region.buildFull());
        overlay.setDstRegion(dst);
        return overlay;
    }

    public static DisplayOverlay buildCustom(String shaderName) {
        return new DisplayOverlay(Type.custom, null, null, shaderName);
    }

    public Type getType() {
        return type;
    }

    public int getStreamId() {
        return source_id == null ? -1 : source_id;
    }

    public String getImagePath() {
        return image_path;
    }

    public String getShaderName() {
        return shader;
    }

    public Region getSrcRegion() {
        return Region.fromString(src_rect);
    }

    public void setSrcRegion(Region region) {
        if (type == Type.custom)
            throw new UnsupportedOperationException("custom overlay non-supports this action");

        this.src_rect = region.toString();
    }

    public Region getDstRegion() {
        return Region.fromString(dst_rect);
    }

    public void setDstRegion(Region region) {
        if (type == Type.custom)
            throw new UnsupportedOperationException("custom overlay non-supports this action");

        this.dst_rect = region.toString();
    }

    public int getZIndex() {
        return z_index;
    }

    public void setZIndex(int index) {
        if (type == Type.custom)
            throw new UnsupportedOperationException("custom overlay non-supports this action");

        this.z_index = index;
    }

    public float getTransparency() {
        return opacity;
    }

    public void setTransparency(float transparency) {
        if (type == Type.custom)
            throw new UnsupportedOperationException("custom overlay non-supports this action");

        this.opacity = transparency;
    }

    public Title getTitle() {
        return display_name_visible == null ? null : new Title(display_name,
                Alignment.fromString(display_name_alignment),
                TextStyle.fromSheet(display_name_style_sheet),
                display_name_visible);
    }

    public DisplayOverlay setTitle(Title title) {
        if (title != null) {
            display_name = title.content;
            display_name_alignment = title.alignment.toString();
            display_name_style_sheet = title.textStyle.toSheet();
            display_name_visible = title.visible;

            if (!display_name_visible)
                display_name = "";
        } else {
            display_name = "";
            display_name_alignment = null;
            display_name_style_sheet = null;
            display_name_visible = false;
        }
        return this;
    }

    public DisplayOverlay setTitle(boolean visible, String content,
                           String alignment, String style_sheet) {
        display_name = content;
        display_name_alignment = alignment;
        display_name_style_sheet = style_sheet;
        display_name_visible = visible;

        if (!display_name_visible)
            display_name = "";
        return this;
    }

    public List<Integer> getCustomStreams() {
        return sources;
    }

    public DisplayOverlay setCustomStreams(List<Integer> streamIds) {
        if (type != Type.custom)
            throw new UnsupportedOperationException("just custom overlay supports this action");

        this.sources = streamIds;
        return this;
    }

    public List<Float> getCustomVariables() {
        return variables;
    }

    public DisplayOverlay setCustomVariables(List<Float> variables) {
        if (type != Type.custom)
            throw new UnsupportedOperationException("just custom overlay supports this action");

        this.variables = variables;
        return this;
    }

    public boolean isEqual(DisplayOverlay other) {
        if (other == null)
            return false;

        return (CompareHelper.isEqual(type, other.type) &&
                CompareHelper.isEqual(source_id, other.source_id) &&
                CompareHelper.isEqual(image_path, other.image_path) &&
                CompareHelper.isEqual(shader, other.shader) &&
                CompareHelper.isEqual(src_rect, other.src_rect) &&
                CompareHelper.isEqual(dst_rect, other.dst_rect) &&
                z_index == other.z_index && opacity == other.opacity &&
                CompareHelper.isEqual(display_name_visible, other.display_name_visible) &&
                CompareHelper.isEqual(display_name, other.display_name) &&
                CompareHelper.isEqual(display_name_alignment, other.display_name_alignment) &&
                CompareHelper.isEqual(display_name_style_sheet, other.display_name_style_sheet) &&
                CompareHelper.isEqual4BaseList(sources, other.sources) &&
                CompareHelper.isEqual4BaseList(variables, other.variables)
        );
    }
}