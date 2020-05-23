package cn.lx.mbs.impl.structures;

// 字号
public enum FontSize {
    XS("极小", 0.03333f),
    S("小", 0.04444f),
    M("中", 0.05556f),
    L("大", 0.06667f),
    XL("极大", 0.08889f);

    public final String desc;   // 描述
    public final float factor;  // 字号像素(px) = 画布宽度 * factor

    FontSize(String desc, float factor) {
        this.desc = desc;
        this.factor = factor;
    }

    public int px(int sceneHeight) {
        return (int)(factor * Float.valueOf(sceneHeight));
    }
}
