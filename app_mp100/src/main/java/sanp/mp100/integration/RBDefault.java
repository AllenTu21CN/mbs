package sanp.mp100.integration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.mpx.mc.ScreenLayout;
import sanp.mp100.integration.RBUtil.Role;
import sanp.mp100.integration.RBUtil.Content;

/**
 * Created by Tuyj on 2017/10/28.
 */

public class RBDefault {
    public static Map<String, Content> SupportingContents = new HashMap<String, Content>() {{
        put("单分屏", new Content("单分屏", ScreenLayout.LayoutMode.SYMMETRICAL, 1, new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard, Role.StudentFullView, Role.StudentFeature, Role.Courseware));
        }}));

        put("老师+学生(等分屏)", new Content("老师+学生(等分屏)", ScreenLayout.LayoutMode.SYMMETRICAL, 2,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+课件(等分屏)", new Content("老师+课件(等分屏)", ScreenLayout.LayoutMode.SYMMETRICAL, 2,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.Courseware));
        }}));
        put("学生+课件(等分屏)", new Content("学生+课件(等分屏)", ScreenLayout.LayoutMode.SYMMETRICAL, 2,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
            put(1, Arrays.asList(Role.Courseware));
        }}));

        put("三等分屏", new Content("三等分屏", ScreenLayout.LayoutMode.SYMMETRICAL, 3,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
            put(2, Arrays.asList(Role.Courseware));
        }}));

        put("老师+学生(画中画-左上)", new Content("老师+学生(画中画-左上)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+学生(画中画-右上)", new Content("老师+学生(画中画-右上)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(2, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+学生(画中画-左下)", new Content("老师+学生(画中画-左下)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(3, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+学生(画中画-右下)", new Content("老师+学生(画中画-右下)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(4, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));

        put("老师+课件(画中画-左上)", new Content("老师+课件(画中画-左上)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.Courseware));
        }}));
        put("老师+课件(画中画-右上)", new Content("老师+课件(画中画-右上)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(2, Arrays.asList(Role.Courseware));
        }}));
        put("老师+课件(画中画-左下)", new Content("老师+课件(画中画-左下)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(3, Arrays.asList(Role.Courseware));
        }}));
        put("老师+课件(画中画-右下)", new Content("老师+课件(画中画-右下)", ScreenLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(4, Arrays.asList(Role.Courseware));
        }}));
    }};

    public enum Layout {
        Single(0x0100, "单分屏"),
        TwoSymTS(0x0200, "老师+学生(等分屏)"),
        TwoSymTC(0x0201, "老师+课件(等分屏)"),
        TwoSymSC(0x0202, "学生+课件(等分屏)"),
        TwoAsymTSLT(0x0203, "老师+学生(画中画-左上)"),
        TwoAsymTSRT(0x0204, "老师+学生(画中画-右上)"),
        TwoAsymTSLB(0x0205, "老师+学生(画中画-左下)"),
        TwoAsymTSRB(0x0206, "老师+学生(画中画-右下)"),
        TwoAsymTCLT(0x0207, "老师+课件(画中画-左上)"),
        TwoAsymTCRT(0x0208, "老师+课件(画中画-右上)"),
        TwoAsymTCLB(0x0209, "老师+课件(画中画-左下)"),
        TwoAsymTCRB(0x020a, "老师+课件(画中画-右下)"),
        ThreeSymTSC(0x0300, "老师+学生+课件(等分屏)");

        private int value;
        private String dsp;
        private Layout(int value, String dsp) {
            this.value = value;
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
        public int toValue() {
            return value;
        }
    }
}
