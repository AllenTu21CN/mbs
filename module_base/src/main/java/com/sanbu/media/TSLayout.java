package com.sanbu.media;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum TSLayout {
    // 空
    None("空", Collections.EMPTY_LIST),

    // 追加
    Append("追加", Collections.EMPTY_LIST),

    // 单分屏
    A("单分屏", Arrays.asList(
            new Region(0.0f, 0.0f, 1.0f, 1.0f)
    ), Arrays.asList("A")),

    // 两分屏

    AB("两等分屏", Arrays.asList(
            new Region(0.0052083f, 0.2537037f, 0.4916667f, 0.4916667f),
            new Region(0.5020833f, 0.2537037f, 0.4916667f, 0.4916667f)
    ), Arrays.asList("A_B")),

    AB_IN_RT("右上画中画", Arrays.asList(
            new Region(0.0f, 0.0f, 1.0f, 1.0f),
            new Region(0.6875f, 0.0625f, 0.25f, 0.25f)
    ), Arrays.asList("A_B_RT")),

    AB_IN_RB("右下画中画", Arrays.asList(
            new Region(0.0f, 0.0f, 1.0f, 1.0f),
            new Region(0.6875f, 0.7f, 0.25f, 0.25f)
    ), Arrays.asList("A_B_RB")),

    AB_OUT_RT("右上画外画", Arrays.asList(
            new Region(0.0f, 0.1111f, 0.75f, 0.75f),
            new Region(0.75f, 0.1111f, 0.25f, 0.25f)
    ), Arrays.asList("A_B_LT", "A_B_LT2")),

    AB_OUT_RB("右下画外画", Arrays.asList(
            new Region(0.0f, 0.1111f, 0.75f, 0.75f),
            new Region(0.75f, 0.61f, 0.25f, 0.25f)
    ), Arrays.asList("A_B_LB", "A_B_LB2")),

    // 三分屏

    ABC("三等分屏(品字)", Arrays.asList(
            new Region(0.2583f,0.0093f,0.4833f,0.4833f),
            new Region(0.0115f,0.5093f,0.4833f,0.4833f),
            new Region(0.5052f,0.5093f,0.4833f,0.4833f)
    ), Arrays.asList("A_B_C_2")),

    ABC_OUT("1+2", Arrays.asList(
            new Region(0.0f, 0.1694f, 0.6666f, 0.6620f),
            new Region(0.6766f, 0.1694f, 0.3239f, 0.3240f),
            new Region(0.6766f, 0.5075f, 0.3239f, 0.3240f)
    ), Arrays.asList("A_B_C")),

    // 四分屏
    ABCD("四等分屏", Arrays.asList(
            new Region(0.0052f,0.0037f,0.4917f,0.4917f),
            new Region(0.5010f,0.0037f,0.4917f,0.4917f),
            new Region(0.0052f,0.5037f,0.4917f,0.4917f),
            new Region(0.5010f,0.5037f,0.4917f,0.4917f)
    ), Arrays.asList("A_B_C_D")),

    ABCD_OUT_LR("左右1+3", Arrays.asList(
            new Region(0.0094f, 0.1352f, 0.7353f, 0.7353f),
            new Region(0.7553f, 0.1352f, 0.2353f, 0.2353f),
            new Region(0.7553f, 0.3852f, 0.2353f, 0.2353f),
            new Region(0.7553f, 0.6352f, 0.2353f, 0.2353f)
    ), Arrays.asList("A_B_C_D_2")),

    ABCD_OUT_TB("上下1+3", Arrays.asList(
            new Region(0.1666f,0.0000f,0.6667f,0.6667f),
            new Region(0.0000f,0.6667f,0.3333f,0.3333f),
            new Region(0.3333f,0.6667f,0.3333f,0.3333f),
            new Region(0.6666f,0.6667f,0.3333f,0.3333f)
    ), Arrays.asList("A_B_C_D_3")),

    // 六分屏
    A4F_OUT("1+5", Arrays.asList(
            new Region(0.0000f,0.0000f,0.6667f,0.6667f),
            new Region(0.6667f,0.0000f,0.3333f,0.3333f),
            new Region(0.6667f,0.3333f,0.3333f,0.3333f),
            new Region(0.0000f,0.6667f,0.3333f,0.3333f),
            new Region(0.3333f,0.6667f,0.3333f,0.3333f),
            new Region(0.6667f,0.6667f,0.3333f,0.3333f)
    ), Arrays.asList("A_F")),

    AB_C1("我也不知道", Arrays.asList(
            new Region(-1, -1, -1, -1),
            new Region(-1, -1, -1, -1)
    ), null, "vec4 custom(vec2 uv) {\n" +
            "  if (uv.x * 5.0 + uv.y < 3.0) {\n" +
            "    return texture2D(texArray[0], vec2(uv.x + 0.25, uv.y));\n" +
            "  } else {\n" +
            "    return texture2D(texArray[1], vec2(uv.x - 0.25, uv.y));\n" +
            "  }\n" +
            "};");

    public final String desc;
    public final List<Region> regions;
    private final List<String> TS5000Name;
    public final String customProgram;

    TSLayout(String desc, List<Region> regions) {
        this(desc, regions, null);
    }

    TSLayout(String desc, List<Region> regions, List<String> TS5000Name) {
        this(desc, regions, TS5000Name, null);
    }

    TSLayout(String desc, List<Region> regions, List<String> TS5000Name, String customProgram) {
        this.desc = desc;
        this.regions = regions;
        this.TS5000Name = TS5000Name;
        this.customProgram = customProgram;
    }

    public String getTS5000Name() {
        return TS5000Name == null ? name() : TS5000Name.get(0);
    }

    public static TSLayout fromName(String name) {
        if (name == null)
            return None;

        try {
            return TSLayout.valueOf(name);
        } catch (IllegalArgumentException e) {
            return None;
        }
    }

    public static TSLayout fromTS5000Name(String name) {
        if (name == null)
            return None;

        for (TSLayout layout : values()) {
            if (layout.TS5000Name != null && layout.TS5000Name.contains(name))
                return layout;
        }

        return None;
    }
}
