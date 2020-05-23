package cn.lx.mbs.impl.structures;

import java.util.Arrays;
import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.LayoutId;
import cn.sanbu.avalon.endpoint3.structures.Region;

// X200支持的布局
public enum TSLayout {
    // 单分屏
    A("单分屏", LayoutId._1x1, null),

    // 两分屏
    AB("两等分屏", LayoutId._1x2, null),
    AB_IN_RT("右上画中画", LayoutId._2_RT_O, null),
    AB_IN_RB("右下画中画", LayoutId._2_RB_O, null),
    AB_OUT_RT("右上画外画", LayoutId._2_RT_P, null),
    AB_OUT_RB("右下画外画", LayoutId._2_RB_P, null),

    // 三分屏
    ABC("三等分屏(品字)", LayoutId._3_SYMM, null),
    ABC_OUT("1+2", LayoutId._3_RT_P, null),

    // 四分屏
    ABCD("四等分屏", LayoutId._2x2, null),
    ABCD_OUT_LR("左右1+3", LayoutId._4_RT_P, null),
    ABCD_OUT_TB("上下1+3", LayoutId.Custom, Arrays.asList(
            new Region(0.1666f,0.0000f,0.6667f,0.6667f),
            new Region(0.0000f,0.6667f,0.3333f,0.3333f),
            new Region(0.3333f,0.6667f,0.3333f,0.3333f),
            new Region(0.6666f,0.6667f,0.3333f,0.3333f)
    )),

    // 六分屏
    A4F_OUT("1+5", LayoutId.Custom, Arrays.asList(
            new Region(0.0000f,0.0000f,0.6667f,0.6667f),
            new Region(0.6667f,0.0000f,0.3333f,0.3333f),
            new Region(0.6667f,0.3333f,0.3333f,0.3333f),
            new Region(0.0000f,0.6667f,0.3333f,0.3333f),
            new Region(0.3333f,0.6667f,0.3333f,0.3333f),
            new Region(0.6667f,0.6667f,0.3333f,0.3333f)
    ));

    public final String desc;
    public final LayoutId layoutId;
    public final List<Region> layoutDesc;

    TSLayout(String desc, LayoutId layoutId, List<Region> layoutDesc) {
        this.desc = desc;
        this.layoutId = layoutId;
        this.layoutDesc = layoutDesc;
    }

    public List<Region> getLayoutDesc() {
        return layoutDesc == null ? layoutId.layout : layoutDesc;
    }
}
