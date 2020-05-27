package cn.lx.mbs.support.structures;

import com.sanbu.tools.CompareHelper;

import java.io.File;

import cn.sanbu.avalon.endpoint3.structures.RecFileFormat;
import cn.sanbu.avalon.endpoint3.structures.jni.RecSplitMode;

// 录制属性
public class RecProp {
    public String filePath;          // 录制路径
    public RecFileFormat fileFormat; // 录制格式
    public String nameFormat;        // 文件名格式
    public RecSplitMode splitMode;   // 分段方式
    public long splitValue;          // 分段值 (字节/秒)

    public RecProp(String filePath, RecFileFormat fileFormat,
                   String nameFormat, RecSplitMode splitMode, long splitValue) {
        this.filePath = filePath.replace(" ", "_");
        this.fileFormat = fileFormat;
        this.nameFormat = nameFormat.replace(" ", "_");
        this.splitMode = splitMode;
        this.splitValue = splitValue;
    }

    public RecProp(RecProp other) {
        this(other.filePath, other.fileFormat, other.nameFormat, other.splitMode, other.splitValue);
    }

    public boolean isValid() {
        return (filePath != null &&
                fileFormat != null &&
                nameFormat != null &&
                splitMode != null
        );
    }

    public boolean isEqual(RecProp other) {
        return CompareHelper.isEqual(filePath, other.filePath) &&
                CompareHelper.isEqual(fileFormat, other.fileFormat) &&
                CompareHelper.isEqual(nameFormat, other.nameFormat) &&
                CompareHelper.isEqual(splitMode, other.splitMode) &&
                CompareHelper.isEqual(splitValue, other.splitValue);
    }

    public String toUrl(String namePrefix) {
        String separator = filePath.endsWith(File.separator) ? "" : File.separator;
        return "file://" + filePath + separator + namePrefix + fileFormat.suffix;
    }
}
