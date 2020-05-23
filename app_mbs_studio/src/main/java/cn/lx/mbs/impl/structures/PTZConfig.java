package cn.lx.mbs.impl.structures;

import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.StringUtil;

public class PTZConfig {
    public DevModel model;           // 型号
    public String address;           // 地址
    public Integer port;             // 端口
    public Integer viscaIdOfTeacher; // 老师机位ID
    public Integer viscaIdOfStudent; // 学生机位ID

    public PTZConfig(DevModel model, String address, int port, int viscaIdOfTeacher, int viscaIdOfStudent) {
        this.model = model;
        this.address = address;
        this.port = port;
        this.viscaIdOfTeacher = viscaIdOfTeacher;
        this.viscaIdOfStudent = viscaIdOfStudent;
    }

    public PTZConfig(PTZConfig other) {
        this(other.model, other.address, other.port,
                other.viscaIdOfTeacher, other.viscaIdOfStudent);
    }

    public boolean isValid() {
        return (model != null &&
                !StringUtil.isEmpty(address) &&
                port != null && port > 0 &&
                viscaIdOfTeacher != null && viscaIdOfTeacher >= 0 &&
                viscaIdOfStudent != null && viscaIdOfStudent >= 0
        );
    }

    public boolean isEqual(PTZConfig other) {
        return (CompareHelper.isEqual(model, other.model) &&
                CompareHelper.isEqual(address, other.address) &&
                CompareHelper.isEqual(port, other.port) &&
                CompareHelper.isEqual(viscaIdOfTeacher, other.viscaIdOfTeacher) &&
                CompareHelper.isEqual(viscaIdOfStudent, other.viscaIdOfStudent));
    }

    public int getTargetId(TSRole role) {
        Integer id = null;
        if (role == TSRole.Teacher || role == TSRole.TeacherCloseUp)
            id = viscaIdOfTeacher;
        else if (role == TSRole.Student || role == TSRole.StudentCloseUp)
            id = viscaIdOfStudent;
        return id == null ? -1 : id;
    }
}
