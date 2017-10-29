package sanp.mp100.integration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.mpx.mc.ScreeLayout;
import sanp.mp100.integration.RBUtil.Role;
import sanp.mp100.integration.RBUtil.Content;

/**
 * Created by Tuyj on 2017/10/28.
 */

public class RBUtilDefault {
    public static Map<String, Content> SupportingContents = new HashMap<String, Content>() {{
        put("单分屏", new Content(ScreeLayout.LayoutMode.SYMMETRICAL, 1, new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard, Role.StudentFullView, Role.StudentFeature, Role.Courseware));
        }}));

        put("老师+学生(等分屏)", new Content(ScreeLayout.LayoutMode.SYMMETRICAL, 2,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+课件(等分屏)", new Content(ScreeLayout.LayoutMode.SYMMETRICAL, 2,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.Courseware));
        }}));
        put("学生+课件(等分屏)", new Content(ScreeLayout.LayoutMode.SYMMETRICAL, 2,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
            put(1, Arrays.asList(Role.Courseware));
        }}));

        put("三等分屏", new Content(ScreeLayout.LayoutMode.SYMMETRICAL, 3,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
            put(2, Arrays.asList(Role.Courseware));
        }}));

        put("老师+学生(画中画-左上)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+学生(画中画-右上)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(2, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+学生(画中画-左下)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(3, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));
        put("老师+学生(画中画-右下)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(4, Arrays.asList(Role.StudentFullView, Role.StudentFeature));
        }}));

        put("老师+课件(画中画-左上)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(1, Arrays.asList(Role.Courseware));
        }}));
        put("老师+课件(画中画-右上)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(2, Arrays.asList(Role.Courseware));
        }}));
        put("老师+课件(画中画-左下)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(3, Arrays.asList(Role.Courseware));
        }}));
        put("老师+课件(画中画-右下)", new Content(ScreeLayout.LayoutMode.ASYMMETRY_OVERLAPPING, 5,  new HashMap<Integer, List<Role>>() {{
            put(0, Arrays.asList(Role.TeacherFullView, Role.TeacherFeature, Role.TeacherBlackboard));
            put(4, Arrays.asList(Role.Courseware));
        }}));
    }};
}
