package sanp.mp100.test.ui.utils;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * Created by zhangxd on 2017/7/22.
 */

public class StorageUtils {

    public static String getSDName() {
        File path = Environment.getExternalStorageDirectory();
        return path.getName();
    }

    public static long getSDTotalSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long totalSize = blockSize * totalBlocks / 1024 / 1024;
        return totalSize;
    }

    public static long getSDAvailableSize(){
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getAvailableBlocksLong();
        long availableSize = blockSize * totalBlocks / 1024 / 1024;
        return availableSize;
    }

    public static String getRomName(){
        File path = Environment.getDataDirectory();
        return  path.getName();
    }

    public static long getRomTotalSize(){
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long totalSize = blockSize * totalBlocks / 1024 / 1024;
        return totalSize;
    }

    public static long getRomAvailableSize(){
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long availableSize = blockSize * availableBlocks / 1024 / 1024;
        return availableSize;
    }
}
