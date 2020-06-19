package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.Alignment;
import cn.sanbu.avalon.endpoint3.structures.DisplayCell;
import cn.sanbu.avalon.endpoint3.structures.Logo;
import cn.sanbu.avalon.endpoint3.structures.Region;
import cn.sanbu.avalon.endpoint3.structures.TextStyle;
import cn.sanbu.avalon.endpoint3.structures.Title;

// 与VideoEngine::Scene re-config对应
public class DisplayConfig {

    // overlays
    public List<DisplayOverlay> overlays;

    // rgb code, argb code or color name of background
    private String background_color;

    // display name(title)
    private Boolean display_name_visible;
    private String display_name;
    private String display_name_alignment;
    private String display_name_style_sheet;

    // transition
    public TransitionDesc transition;

    @Deprecated
    private Marquee marquee = null;

    @Deprecated
    private Boolean debug_mode = null;

    private DisplayConfig() {

    }

    public DisplayConfig setOverlays(List<Region> layout, List<DisplayCell> cells, Logo logo) {
        overlays = new LinkedList<>();

        for (DisplayCell cell: cells) {
            if (cell.index >= layout.size())
                continue;

            Region dstRegion = layout.get(cell.index);
            DisplayOverlay overlay;
            if (cell.isStream) {
                overlay = new DisplayOverlay.Stream(cell.streamId, dstRegion).setTitle(cell.title);
            } else {
                overlay = new DisplayOverlay.Image(cell.imagePath, dstRegion);
            }

            overlay.setZIndex(cell.index);
            overlay.setTransparency(cell.transparency);
            overlay.setSrcRegion(cell.srcRegion);
            overlays.add(overlay);
        }

        if (logo != null) {
            DisplayOverlay overlay = logo.toOverlay();
            overlay.setZIndex(layout.size() + 1);
            overlays.add(overlay);
        }

        return this;
    }

    public DisplayConfig setBackgroundColor(String codeOrName) {
        background_color = codeOrName;
        return this;
    }

    public String getBackgroundColor() {
        return background_color;
    }

    public Title getTitle() {
        return display_name_visible == null ? null : new Title(display_name,
                Alignment.fromString(display_name_alignment),
                TextStyle.fromSheet(display_name_style_sheet),
                display_name_visible);
    }

    public DisplayConfig setTitle(Title title) {
        display_name = title.content;
        display_name_alignment = title.alignment.toString();
        display_name_style_sheet = title.textStyle.toSheet();
        display_name_visible = title.visible;
        return this;
    }

    public DisplayConfig setTitle(boolean visible, String content,
                                  String alignment, String style_sheet) {
        display_name = content;
        display_name_alignment = alignment;
        display_name_style_sheet = style_sheet;
        display_name_visible = visible;
        return this;
    }

    public TransitionDesc getTransition() {
        return transition;
    }

    public void setTransition(TransitionDesc transition) {
        this.transition = transition;
    }

    public boolean isEqual(DisplayConfig other) {
        if (other == null)
            return false;
        if (!CompareHelper.isEqual(other.background_color, background_color))
            return false;
        if (!CompareHelper.isEqual(other.display_name_visible, display_name_visible))
            return false;
        if (!CompareHelper.isEqual(other.display_name, display_name))
            return false;
        if (!CompareHelper.isEqual(other.display_name_alignment, display_name_alignment))
            return false;
        if (!CompareHelper.isEqual(other.display_name_style_sheet, display_name_style_sheet))
            return false;
        if (!CompareHelper.isEqual(other.transition, transition, (src, dst) -> transition.isEqual(other.transition)))
            return false;
        return CompareHelper.isEqual(overlays, other.overlays, (src, dst) -> {
            if (overlays.size() != other.overlays.size())
                return false;
            for (int i = 0 ; i < overlays.size() ; ++i) {
                DisplayOverlay sc = overlays.get(i);
                DisplayOverlay dc = other.overlays.get(i);
                if (!sc.isEqual(dc))
                    return false;
            }
            return true;
        });
    }

    public boolean isValid() {
        return (overlays != null || background_color != null ||
                display_name_visible != null || transition != null);
    }

    public static DisplayConfig buildOverlays(List<DisplayOverlay> overlays) {
        DisplayConfig config = new DisplayConfig();
        config.overlays = overlays;
        return config;
    }

    public static DisplayConfig buildOverlays(List<Region> layout, List<DisplayCell> cells) {
        return buildOverlays(layout, cells, null);
    }

    public static DisplayConfig buildOverlays(List<Region> layout, List<DisplayCell> cells, Logo logo) {
        return new DisplayConfig().setOverlays(layout, cells, logo);
    }

    public static DisplayConfig buildEmptyOverlays() {
        DisplayConfig config = new DisplayConfig();
        config.overlays = new ArrayList<>();
        return config;
    }

    public static DisplayConfig buildBackgroundColor(String codeOrName) {
        return new DisplayConfig().setBackgroundColor(codeOrName);
    }

    public static DisplayConfig buildTitle(Title title) {
        return new DisplayConfig().setTitle(title);
    }

    public static DisplayConfig buildTransition(TransitionDesc transition) {
        DisplayConfig config = new DisplayConfig();
        config.transition = transition;
        return config;
    }
}
