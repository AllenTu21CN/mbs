package cn.lx.mbs.impl.structures;

// 导播模式
public enum DirectingMode {
    FullAuto("全自动", "自动切换布局，自动填充内容"),
    SemiAuto("半自动", "手动切换布局，自动填充内容"),
    Manual("手动", "手动切换布局，手动填充内容");

    public final String name;
    public final String desc;

    DirectingMode(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
