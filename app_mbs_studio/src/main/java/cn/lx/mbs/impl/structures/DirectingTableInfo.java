package cn.lx.mbs.impl.structures;

import com.sanbu.tools.StringUtil;

import java.util.List;

public class DirectingTableInfo {
    public String tableName;            // 表名称
    public List<BIZScene> bizSceneList; // 使用场景
    public DirectingType tableType;     // 表类型
    public String filterType;           // 过滤类型: Idle-空闲时/P2P-点对点/Meeting-多点
    public String description;          // 表描述
    public String fileName;             // 文件名(短)
    public String fullPath;             // 文件全路径
    public long fileSize;               // 文件大小(字节)
    public boolean buildIn;            // 是否内建

    public DirectingTableInfo(String tableName, List<BIZScene> bizSceneList,DirectingType tableType, String description,
                              String fileName, String fullPath, long fileSize,boolean buildIn) {
        this.tableName = tableName;
        this.bizSceneList = bizSceneList;
        this.tableType = tableType;
        this.description = description;
        this.fileName = fileName;
        this.fullPath = fullPath;
        this.fileSize = fileSize;
        this.buildIn = buildIn;
    }

    public boolean isValid() {
        return !StringUtil.isEmpty(tableName) && tableType != null &&
                !StringUtil.isEmpty(filterType) && !StringUtil.isEmpty(description);
    }
}
