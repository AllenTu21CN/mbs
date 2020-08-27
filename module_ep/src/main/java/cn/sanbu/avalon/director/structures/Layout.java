package cn.sanbu.avalon.director.structures;

import com.sanbu.tools.CompareHelper;

import java.util.List;

public class Layout {

    public static class Cell {
        public String role;
        public int objId;
        public Object data;

        public Cell(String role, int objId) {
            this.role = role;
            this.objId = objId;
        }

        public boolean isEqual(Cell other) {
            return CompareHelper.isEqual(this, other, (src, dst) ->
                    CompareHelper.isEqual(role, other.role) && objId == other.objId);
        }
    }

    public int targetId;
    public String layoutName;
    public List<Cell> required;
    public List<Cell> optional;

    public Layout(int targetId, String layoutName, List<Cell> required, List<Cell> optional) {
        this.targetId = targetId;
        this.layoutName = layoutName;
        this.required = required;
        this.optional = optional;
    }
}
