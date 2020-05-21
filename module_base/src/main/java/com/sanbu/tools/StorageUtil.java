package com.sanbu.tools;

import android.app.Activity;
import android.content.Context;
import android.os.storage.StorageManager;

import com.sanbu.base.VolumeInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StorageUtil {

    public static List<VolumeInfo> getVolumes(Context context) {
        List<VolumeInfo> result = new ArrayList<>();

        initMethods();
        if (Clazz_VolumeInfo == null)
            return result;

        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Activity.STORAGE_SERVICE);
            List volumes = (List<?>) StorageManager_getVolumes.invoke(storageManager);
            for (Object volume: volumes) {
                if (volume == null)
                    continue;

                String desc = (String) StorageManager_getBestVolumeDescription.invoke(storageManager, volume);
                VolumeInfo info = initVolumeInfo(volume, desc);
                if (info != null)
                    result.add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static VolumeInfo getVolume(Context context, String rootPath) {
        initMethods();
        if (Clazz_VolumeInfo == null)
            return VolumeInfo.buildEmpty();

        if (rootPath.startsWith("file://"))
            rootPath = rootPath.substring("file://".length());

        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Activity.STORAGE_SERVICE);
            List volumes = (List<?>) StorageManager_getVolumes.invoke(storageManager);
            for (Object volume: volumes) {
                if (volume == null)
                    continue;

                String desc = (String) StorageManager_getBestVolumeDescription.invoke(storageManager, volume);
                VolumeInfo info = initVolumeInfo(volume, desc);
                if (info != null && info.getPath() != null && info.getPath().getAbsolutePath().startsWith(rootPath))
                    return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getExternalFilesDir(Context context, String rootPath) {
        if (rootPath.startsWith("file://"))
            rootPath = rootPath.substring("file://".length());

        File[] externalFilesDirs = context.getExternalFilesDirs(null);
        for (File f: externalFilesDirs) {
            String path = f.getAbsolutePath();
            if (path.startsWith(rootPath)) {
                return path;
            }
        }

        return null;
    }

    private static Class<?> Clazz_VolumeInfo = null;
    private static Class<?> Clazz_DiskInfo = null;
    private static Method StorageManager_getBestVolumeDescription = null;
    private static Method StorageManager_getVolumes = null;
    private static Field VolumeInfo_id = null;
    private static Field VolumeInfo_type = null;
    private static Field VolumeInfo_disk = null;
    private static Field VolumeInfo_mountFlags = null;
    private static Field VolumeInfo_mountUserId = null;
    private static Field VolumeInfo_state = null;
    private static Field VolumeInfo_fsType = null;
    private static Field VolumeInfo_path = null;
    private static Field VolumeInfo_internalPath = null;
    private static Field DiskInfo_flags = null;
    private static Field DiskInfo_size = null;

    private static void initMethods() {
        synchronized (StorageUtil.class) {
            if (Clazz_VolumeInfo == null) {
                try {
                    Clazz_VolumeInfo = Class.forName("android.os.storage.VolumeInfo");
                    Clazz_DiskInfo = Class.forName("android.os.storage.DiskInfo");
                    StorageManager_getBestVolumeDescription = StorageManager.class.getMethod("getBestVolumeDescription", Clazz_VolumeInfo);
                    StorageManager_getVolumes = StorageManager.class.getMethod("getVolumes");
                    VolumeInfo_id = Clazz_VolumeInfo.getField("id");
                    VolumeInfo_type = Clazz_VolumeInfo.getField("type");
                    VolumeInfo_disk = Clazz_VolumeInfo.getField("disk");
                    VolumeInfo_mountFlags = Clazz_VolumeInfo.getField("mountFlags");
                    VolumeInfo_mountUserId = Clazz_VolumeInfo.getField("mountUserId");
                    VolumeInfo_state = Clazz_VolumeInfo.getField("state");
                    VolumeInfo_fsType = Clazz_VolumeInfo.getField("fsType");
                    VolumeInfo_path = Clazz_VolumeInfo.getField("path");
                    VolumeInfo_internalPath = Clazz_VolumeInfo.getField("internalPath");
                    DiskInfo_flags = Clazz_DiskInfo.getField("flags");
                    DiskInfo_size = Clazz_DiskInfo.getField("size");
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                    Clazz_VolumeInfo = null;
                    Clazz_DiskInfo = null;
                    StorageManager_getBestVolumeDescription = null;
                    StorageManager_getVolumes = null;
                    VolumeInfo_id = null;
                    VolumeInfo_type = null;
                    VolumeInfo_disk = null;
                    VolumeInfo_mountFlags = null;
                    VolumeInfo_mountUserId = null;
                    VolumeInfo_state = null;
                    VolumeInfo_fsType = null;
                    VolumeInfo_path = null;
                    VolumeInfo_internalPath = null;
                    DiskInfo_flags = null;
                    DiskInfo_size = null;
                }
            }
        }
    }

    private static VolumeInfo initVolumeInfo(Object volumeInfo, String desc) {
        try {
            String id = (String) VolumeInfo_id.get(volumeInfo);
            int type = (int) VolumeInfo_type.get(volumeInfo);
            Object disk = VolumeInfo_disk.get(volumeInfo);
            int mountFlags = (int) VolumeInfo_mountFlags.get(volumeInfo);
            int mountUserId = (int) VolumeInfo_mountUserId.get(volumeInfo);
            int state = (int) VolumeInfo_state.get(volumeInfo);
            String fsType = (String) VolumeInfo_fsType.get(volumeInfo);
            String path = (String) VolumeInfo_path.get(volumeInfo);
            String internalPath = (String) VolumeInfo_internalPath.get(volumeInfo);
            int diskFlags = disk != null ? (int) DiskInfo_flags.get(disk) : 0;
            long diskSize = disk != null ? (long) DiskInfo_size.get(disk) : 0;
            return new VolumeInfo(id, type, mountFlags, mountUserId,
                    state, fsType, path, internalPath, diskFlags, diskSize, desc);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
