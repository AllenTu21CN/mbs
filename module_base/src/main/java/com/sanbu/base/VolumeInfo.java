package com.sanbu.base;

import java.io.File;

public class VolumeInfo {

    private final String id;            // public:8,4
    private final int type;             // 0
    private final int state;            // 2
    private final int mountFlags;       // 2
    private final int mountUserId;      // 0
    private final String fsType;        // vfat
    private final String path;          // /storage/B4FE-5315
    private final String internalPath;  // /mnt/media_rw/B4FE-5315
    private final int diskFlags;        // 8
    private final long diskSize;        // 30752000000
    private final String desc;          // TUYJ_GSP1RMCENVO

    public static VolumeInfo buildEmpty() {
        return new VolumeInfo("", TYPE_PUBLIC, 0, 0,
                STATE_UNMOUNTED, "", "", "", 0, 0, "");
    }

    public VolumeInfo(String id, int type, int mountFlags, int mountUserId,
                      int state, String fsType, String path, String internalPath,
                      int diskFlags, long diskSize, String desc) {
        this.id = id;
        this.type = type;
        this.mountFlags = mountFlags;
        this.mountUserId = mountUserId;
        this.state = state;
        this.fsType = fsType;
        this.path = path;
        this.internalPath = internalPath;
        this.diskFlags = diskFlags;
        this.diskSize = diskSize;
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public boolean isPublic() {
        return type == TYPE_PUBLIC;
    }

    public boolean isPrivate() {
        return type == TYPE_PRIVATE;
    }

    public boolean isAvailable() {
        return isMountedReadable();
    }

    public boolean isMountedReadable() {
        return state == STATE_MOUNTED || state == STATE_MOUNTED_READ_ONLY;
    }

    public boolean isMountedWritable() {
        return state == STATE_MOUNTED;
    }

    public int getMountUserId() {
        return mountUserId;
    }

    public boolean isPrimary() {
        return (mountFlags & MOUNT_FLAG_PRIMARY) != 0;
    }

    public boolean isPrimaryPhysical() {
        return isPrimary() && (type == TYPE_PUBLIC);
    }

    public boolean isVisible() {
        return (mountFlags & MOUNT_FLAG_VISIBLE) != 0;
    }

    public boolean isVisibleForRead(int userId) {
        if (type == TYPE_PUBLIC) {
            if (isPrimary() && mountUserId != userId) {
                // Primary physical is only visible to single user
                return false;
            } else {
                return isVisible();
            }
        } else if (type == TYPE_EMULATED) {
            return isVisible();
        } else {
            return false;
        }
    }

    public boolean isVisibleForWrite(int userId) {
        if (type == TYPE_PUBLIC && mountUserId == userId) {
            return isVisible();
        } else if (type == TYPE_EMULATED) {
            return isVisible();
        } else {
            return false;
        }
    }

    public File getPath() {
        return (path != null) ? new File(path) : null;
    }

    public File getInternalPath() {
        return (internalPath != null) ? new File(internalPath) : null;
    }

    public File getPathForUser(int userId) {
        if (path == null) {
            return null;
        } else if (type == TYPE_PUBLIC) {
            return new File(path);
        } else if (type == TYPE_EMULATED) {
            return new File(path, Integer.toString(userId));
        } else {
            return null;
        }
    }

    public File getInternalPathForUser(int userId) {
        if (type == TYPE_PUBLIC) {
            // TODO: plumb through cleaner path from vold
            return new File(path.replace("/storage/", "/mnt/media_rw/"));
        } else {
            return getPathForUser(userId);
        }
    }

    public boolean isSd() {
        return (diskFlags & DISK_FLAG_SD) != 0;
    }

    public boolean isUsb() {
        return (diskFlags & DISK_FLAG_USB) != 0;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public String getDescription() {
        return desc;
    }

    public boolean isCurrentVolume(String target) {
        final String prefix = "file://";
        if (target.startsWith(prefix))
            target = target.substring(prefix.length());
        if (path != null && !path.isEmpty()) {
            if (target.startsWith(path))
                return true;
        }
        if (internalPath != null && !internalPath.isEmpty()) {
            if (target.startsWith(internalPath))
                return true;
        }
        return false;
    }

    public boolean isEqual(VolumeInfo other) {
        if (other == null)
            return false;
        if (isCurrentVolume(other.path))
            return true;
        if (isCurrentVolume(other.internalPath))
            return true;
        return false;
    }

    private static final int TYPE_PUBLIC = 0;
    private static final int TYPE_PRIVATE = 1;
    private static final int TYPE_EMULATED = 2;
    private static final int TYPE_ASEC = 3;
    private static final int TYPE_OBB = 4;

    private static final int STATE_UNMOUNTED = 0;
    private static final int STATE_CHECKING = 1;
    private static final int STATE_MOUNTED = 2;
    private static final int STATE_MOUNTED_READ_ONLY = 3;
    private static final int STATE_FORMATTING = 4;
    private static final int STATE_EJECTING = 5;
    private static final int STATE_UNMOUNTABLE = 6;
    private static final int STATE_REMOVED = 7;
    private static final int STATE_BAD_REMOVAL = 8;

    private static final int MOUNT_FLAG_PRIMARY = 1 << 0;
    private static final int MOUNT_FLAG_VISIBLE = 1 << 1;

    private static final int DISK_FLAG_ADOPTABLE = 1 << 0;
    private static final int DISK_FLAG_DEFAULT_PRIMARY = 1 << 1;
    private static final int DISK_FLAG_SD = 1 << 2;
    private static final int DISK_FLAG_USB = 1 << 3;
}
