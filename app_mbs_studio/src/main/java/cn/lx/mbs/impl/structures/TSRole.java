package cn.lx.mbs.impl.structures;

import cn.sanbu.avalon.endpoint3.structures.EPObjectType;

// 本机教学角色
public enum TSRole {
    None("N/A", false, null, true),

    Teacher("老师全景", true, EPObjectType.Source, true),
    TeacherCloseUp("老师特写", true, EPObjectType.Source, true),
    Student("学生全景", true, EPObjectType.Source, true),
    StudentCloseUp("学生特写", true, EPObjectType.Source, true),
    Blackboard("板书", true, EPObjectType.Source, true),
    Courseware("课件", true, EPObjectType.Source, true),

    Mic("麦克风", true, EPObjectType.Source, true),

    LocalExt("本地辅流", true, EPObjectType.Source, true),

    RemoteSource("远程源", false, EPObjectType.Source, false),
    Caller("互动远端", false, EPObjectType.Caller, false),
    CallerPlus("远端辅流", false, EPObjectType.Caller, false);

    public final String desc;
    public final boolean local;
    public final EPObjectType type;
    public final boolean onlyOne;

    TSRole(String desc, boolean local, EPObjectType type, boolean onlyOne) {
        this.desc = desc;
        this.local = local;
        this.type = type;
        this.onlyOne = onlyOne;
    }
}
