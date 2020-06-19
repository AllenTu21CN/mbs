package cn.lx.mbs.support.structures;

import com.sanbu.tools.CompareHelper;

import java.util.List;

public class Layout {
    private List<Overlay> overlays;
    // 背景颜色,支持RGB码/ARGB码/颜色英文名
    private String backgroundColor;

    public Layout() {

    }

    public Layout(List<Overlay> overlays, String backgroundColor) {
        this.overlays = overlays;
        this.backgroundColor = backgroundColor;
    }

    public List<Overlay> getOverlays() {
        return overlays;
    }

    public Layout setOverlays(List<Overlay> overlays) {
        this.overlays = overlays;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public Layout setBackgroundColor(String codeOrName) {
        backgroundColor = codeOrName;
        return this;
    }

    public boolean isEqual(Layout other) {
        if (other == null)
            return false;
        if (!CompareHelper.isEqual(other.backgroundColor, backgroundColor))
            return false;
        return CompareHelper.isEqual(overlays, other.overlays, (src, dst) -> {
            if (overlays.size() != other.overlays.size())
                return false;
            for (int i = 0 ; i < overlays.size() ; ++i) {
                Overlay sc = overlays.get(i);
                Overlay dc = other.overlays.get(i);
                if (!sc.isEqual(dc))
                    return false;
            }
            return true;
        });
    }

    public boolean isValid() {
        return (overlays != null || backgroundColor != null);
    }

    public static Layout buildEmpty() {
        return new Layout();
    }
}
