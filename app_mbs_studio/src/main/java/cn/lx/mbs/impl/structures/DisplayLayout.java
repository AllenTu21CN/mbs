package cn.lx.mbs.impl.structures;

import com.sanbu.tools.CompareHelper;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.EPObjectType;

public class DisplayLayout {

    public static class Cell {
        public final int No;       // 分屏编号
        public final TSRole role;  // 角色
        public final int parentId; // 所属ID(sourceId or caller id)

        public Cell(int no, TSRole role, int parentId) {
            this.No = no;
            this.role = role;
            this.parentId = parentId;
        }

        public Cell(String code, TSRole role, int parentId) {
            this.No = Code2Number(code);
            this.role = role;
            this.parentId = parentId;
        }

        public int index() {
            return No - 1;
        }

        public boolean isValid() {
            if (No <= 0 || role == null)
                return false;
            if (role == TSRole.Mic || role == TSRole.LocalExt)
                return false;
            if (role == TSRole.None || role.type == EPObjectType.Source)
                return true;
            if (role == TSRole.Caller && parentId < 0)
                return false;
            return true;
        }

        public boolean isEqual(Cell other) {
            if (other == null || !CompareHelper.isEqual(No, other.No) ||
                    !CompareHelper.isEqual(role, other.role))
                return false;
            if (!role.onlyOne && !CompareHelper.isEqual(parentId, other.parentId))
                return false;
            return true;
        }

        private static final List<String> gCodes = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

        private static int Code2Number(String code) {
            return gCodes.indexOf(code) + 1;
        }
    }

    public TSLayout layout;   // 布局名称
    public List<Cell> cells;  // 分屏内容

    public DisplayLayout() {

    }

    public DisplayLayout(TSLayout layout, List<Cell> cells) {
        this.layout = layout;
        this.cells = cells;
    }

    public boolean isEmpty() {
        return cells == null || cells.size() == 0;
    }

    public boolean isEqual(DisplayLayout other) {
        if (other == null)
            return false;
        return CompareHelper.isEqual(other.layout, layout) &&
                CompareHelper.isEqual(cells, other.cells, (src, dst) -> {
                    List<Cell> s = (List<Cell>) src;
                    List<Cell> d = (List<Cell>) dst;
                    if (s.size() != d.size())
                        return false;
                    for (int i = 0 ; i < s.size() ; ++i) {
                        Cell sc = s.get(i);
                        Cell dc = d.get(i);
                        if (!sc.isEqual(dc))
                            return false;
                    }
                    return true;
                });
    }

    public static DisplayLayout buildEmpty() {
        return new DisplayLayout(TSLayout.A, new LinkedList<>());
    }
}
