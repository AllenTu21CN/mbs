package cn.sanbu.avalon.endpoint3.structures;

import com.sanbu.media.Alignment;
import com.sanbu.media.TextStyle;
import com.sanbu.tools.CompareHelper;

public class Title {
    public String content;
    public Alignment alignment;
    public TextStyle textStyle;
    public boolean visible;

    public static Title buildEmpty() {
        return new Title("", Alignment.UNDEFINED, new TextStyle(), false);
    }

    public Title(String content, Alignment alignment, TextStyle style, boolean visible) {
        this.content = content == null ? "" : content;
        this.alignment = alignment;
        this.textStyle = style;
        this.visible = visible;
    }

    public Title(Title other) {
        this(other.content, other.alignment, other.textStyle, other.visible);
    }

    public boolean isValid() {
        return !visible || (content != null && !content.isEmpty());
    }

    public boolean isEqual(Title other) {
        if (other == null)
            return false;
        return (CompareHelper.isEqual(content, other.content) &&
                CompareHelper.isEqual(alignment, other.alignment) &&
                CompareHelper.isEqual(textStyle, other.textStyle, (src, dst) -> textStyle.isEqual(other.textStyle)) &&
                visible == other.visible);
    }
}
