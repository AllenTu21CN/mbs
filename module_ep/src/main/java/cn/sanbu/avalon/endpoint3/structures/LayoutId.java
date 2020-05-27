package cn.sanbu.avalon.endpoint3.structures;

import java.util.Arrays;
import java.util.List;

@Deprecated
public enum LayoutId {
    _1x1("1x1", Arrays.asList(new Region(0.0f, 0.0f, 1.0f, 1.0f)), true),

    _1x2("1x2", Arrays.asList(new Region(0.0052083f, 0.2537037f, 0.4916667f, 0.4916667f), new Region(0.5020833f, 0.2537037f, 0.4916667f, 0.4916667f)), true),
    _2_RT_O("2-RT-O", Arrays.asList(new Region(0.0f, 0.0f, 1.0f, 1.0f), new Region(0.6875f, 0.0625f, 0.25f, 0.25f)), true),
    _2_LT_O("2-LT-O", Arrays.asList(new Region(0.0f, 0.0f, 1.0f, 1.0f), new Region(0.0625f, 0.0625f, 0.25f, 0.25f)), true),
    _2_RB_O("2-RB-O", Arrays.asList(new Region(0.0f, 0.0f, 1.0f, 1.0f), new Region(0.6875f, 0.7f, 0.25f, 0.25f)), true),
    _2_LB_O("2-LB-O", Arrays.asList(new Region(0.0f, 0.0f, 1.0f, 1.0f), new Region(0.0625f, 0.7f, 0.25f, 0.25f)), true),
    _2_MT_P("2-MT-P", Arrays.asList(new Region(0.15625f, 0.296875f, 0.6875f, 0.6875f), new Region(0.3671875f, 0.015625f, 0.265625f, 0.265625f)), true),
    _2_MB_P("2-MB-P", Arrays.asList(new Region(0.15625f, 0.015625f, 0.6875f, 0.6875f), new Region(0.3671875f, 0.71875f, 0.265625f, 0.265625f)), true),
    _2_RT_P("2-RT-P", Arrays.asList(new Region(0.0f, 0.1111f, 0.75f, 0.75f), new Region(0.75f, 0.1111f, 0.25f, 0.25f)), false),
    _2_RB_P("2-RB-P", Arrays.asList(new Region(0.0f, 0.1111f, 0.75f, 0.75f), new Region(0.75f, 0.61f, 0.25f, 0.25f)), false),

    _1x3("1x3", Arrays.asList(new Region(0.05f, 0.04f, 0.3f, 0.3f), new Region(0.35f, 0.04f, 0.3f, 0.3f), new Region(0.65f, 0.04f, 0.3f, 0.3f)), true),
    _3_SYMM("3-SYMM", Arrays.asList(
            new Region(0.2583f,0.0093f,0.4833f,0.4833f),
            new Region(0.0115f,0.5093f,0.4833f,0.4833f),
            new Region(0.5052f,0.5093f,0.4833f,0.4833f)
    ), false),
    _3_RT_P("3-RT-P", Arrays.asList(
            new Region(0.0f, 0.1694f, 0.6666f, 0.6620f),
            new Region(0.6766f, 0.1694f, 0.3239f, 0.3240f),
            new Region(0.6766f, 0.5075f, 0.3239f, 0.3240f)
    ), false),

    _2x2("2x2", Arrays.asList(
            new Region(0.0052f,0.0037f,0.4917f,0.4917f), new Region(0.5010f,0.0037f,0.4917f,0.4917f),
            new Region(0.0052f,0.5037f,0.4917f,0.4917f), new Region(0.5010f,0.5037f,0.4917f,0.4917f)
    ), false),
    _4_RT_P("4_RT_P", Arrays.asList(
            new Region(0.0094f, 0.1352f, 0.7353f, 0.7353f),
            new Region(0.7553f, 0.1352f, 0.2353f, 0.2353f),
            new Region(0.7553f, 0.3852f, 0.2353f, 0.2353f),
            new Region(0.7553f, 0.6352f, 0.2353f, 0.2353f)
    ), false),

    Custom("CUSTOM", Arrays.asList(), false);

    public final String name;
    public final List<Region> layout;
    public final boolean origin;

    LayoutId(String name, List<Region> layout, boolean origin) {
        this.name = name;
        this.layout = layout;
        this.origin = origin;
    }

    public static LayoutId fromName(String name) {
        if (name == null)
            return null;

        for (LayoutId id: values()) {
            if (id.name.equals(name))
                return id;
        }
        return null;
    }
}
