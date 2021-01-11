package cn.lx.mbs.support.structures;

import com.sanbu.media.Region;

import java.util.Arrays;
import java.util.List;

// 内置的布局
public enum LayoutId {
    // 单分屏
    A("单分屏", Arrays.asList(
            new Region(0.0f, 0.0f, 1.0f, 1.0f)
    )),

    // 两分屏
    AB("两等分屏", Arrays.asList(
            new Region(0.0052083f, 0.2537037f, 0.4916667f, 0.4916667f),
            new Region(0.5020833f, 0.2537037f, 0.4916667f, 0.4916667f)
    )),
    AB_IN_RT("右上画中画", Arrays.asList(
            new Region(0.0f, 0.0f, 1.0f, 1.0f),
            new Region(0.6875f, 0.0625f, 0.25f, 0.25f)
    )),
    AB_IN_RB("右下画中画", Arrays.asList(
            new Region(0.0f, 0.0f, 1.0f, 1.0f),
            new Region(0.6875f, 0.7f, 0.25f, 0.25f)
    )),
    AB_OUT_RT("右上画外画", Arrays.asList(
            new Region(0.0f, 0.1111f, 0.75f, 0.75f),
            new Region(0.75f, 0.1111f, 0.25f, 0.25f)
    )),
    AB_OUT_RB("右下画外画", Arrays.asList(
            new Region(0.0f, 0.1111f, 0.75f, 0.75f),
            new Region(0.75f, 0.61f, 0.25f, 0.25f)
    )),

    // 三分屏
    ABC("三等分屏(品字)", Arrays.asList(
            new Region(0.2583f,0.0093f,0.4833f,0.4833f),
            new Region(0.0115f,0.5093f,0.4833f,0.4833f),
            new Region(0.5052f,0.5093f,0.4833f,0.4833f)
    )),
    ABC_OUT("1+2", Arrays.asList(
            new Region(0.0f, 0.1694f, 0.6666f, 0.6620f),
            new Region(0.6766f, 0.1694f, 0.3239f, 0.3240f),
            new Region(0.6766f, 0.5075f, 0.3239f, 0.3240f)
    )),

    // 四分屏
    ABCD("四等分屏", Arrays.asList(
            new Region(0.0052f,0.0037f,0.4917f,0.4917f),
            new Region(0.5010f,0.0037f,0.4917f,0.4917f),
            new Region(0.0052f,0.5037f,0.4917f,0.4917f),
            new Region(0.5010f,0.5037f,0.4917f,0.4917f)
    )),
    ABCD_OUT_LR("左右1+3", Arrays.asList(
            new Region(0.0094f, 0.1352f, 0.7353f, 0.7353f),
            new Region(0.7553f, 0.1352f, 0.2353f, 0.2353f),
            new Region(0.7553f, 0.3852f, 0.2353f, 0.2353f),
            new Region(0.7553f, 0.6352f, 0.2353f, 0.2353f)
    )),
    ABCD_OUT_TB("上下1+3", Arrays.asList(
            new Region(0.1666f,0.0000f,0.6667f,0.6667f),
            new Region(0.0000f,0.6667f,0.3333f,0.3333f),
            new Region(0.3333f,0.6667f,0.3333f,0.3333f),
            new Region(0.6666f,0.6667f,0.3333f,0.3333f)
    )),

    // 六分屏
    A4F_OUT("1+5", Arrays.asList(
            new Region(0.0000f,0.0000f,0.6667f,0.6667f),
            new Region(0.6667f,0.0000f,0.3333f,0.3333f),
            new Region(0.6667f,0.3333f,0.3333f,0.3333f),
            new Region(0.0000f,0.6667f,0.3333f,0.3333f),
            new Region(0.3333f,0.6667f,0.3333f,0.3333f),
            new Region(0.6667f,0.6667f,0.3333f,0.3333f)
    ));

    public final String desc;
    public final List<Region> regions;

    LayoutId(String desc, List<Region> regions) {
        this.desc = desc;
        this.regions = regions;
    }
}
