package cn.sanbu.avalon.endpoint3.ext;

public enum ExtEvent {

    // event type: 0-virtual, 1-tracking, 2-ppt monitor

    // virtual events
    Reset                 (0, 0, new byte[]{(byte) 0x99, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}),

    // tracking events

    TeacherTargetAppear   (1, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x01, (byte) 0x08}),
    TeacherTargetLoss     (2, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x09}),
    TeacherOnPodium       (3, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x03, (byte) 0x0A}),
    TeacherDownPodium     (4, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x04, (byte) 0x0B}),
    TeacherPTZMovement    (5, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x04}),
    TeacherPTZStill       (6, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x03}),
    TeacherWritingBegin   (7, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x05, (byte) 0x0C}),
    TeacherWritingEnd     (8, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x06, (byte) 0x0D}),
    StudentStandUp        (9, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x07, (byte) 0x0E}),
    StudentCloseUp        (10, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x08, (byte) 0x0F}),
    StudentMultiObjects   (11, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x09, (byte) 0x10}),
    StudentSitDown        (12, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x0A, (byte) 0x11}),

    TeacherSingleObject   (13, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x0B, (byte) 0x12}),
    TeacherMultiObjects   (14, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x0C, (byte) 0x13}),

    BlackboardCloseUpInner(15, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x0D, (byte) 0x14}),
    StudentCloseUpInner   (16, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x0E, (byte) 0x15}),
    TeacherCloseUpInner   (17, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x0F, (byte) 0x16}),
    StudentFullViewInner  (18, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x10, (byte) 0x17}),
    TeacherFullViewInner  (19, 1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x11, (byte) 0x18}),

    // ppt monitor events

    CoursewareActive      (20, 2, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x01, (byte) 0x01}),
    CoursewareTimeout     (21, 2, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x01, (byte) 0x02}),
    CoursewareLock        (22, 2, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x01}),
    CoursewareUnlock      (23, 2, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x02});

    /* prefix|bin|suffix */
    public static final byte[] FIXED_PREFIX = {(byte) 0xFE};
    public static final byte[] FIXED_SUFFIX = {(byte) 0xFF};
    public static final int FIXED_DATA_LENGTH = Reset.bin.length;
    public static final int FIXED_FULL_LENGTH = FIXED_DATA_LENGTH + FIXED_PREFIX.length + FIXED_SUFFIX.length;

    public final int id;
    public final int type;
    public final byte[] bin;

    ExtEvent(int id, int type, byte[] bin) {
        this.id = id;
        this.type = type;
        this.bin = bin;
    }

    public static ExtEvent fromBin(byte[] bin, int offset, int length) {
        return fromBin(-1, bin, offset, length);
    }

    public static ExtEvent fromBin(int type, byte[] bin, int offset, int length) {
        if (bin == null)
            return null;
        if (length != FIXED_DATA_LENGTH)
            return null;

        for (ExtEvent event: ExtEvent.values()) {
            if (type >= 0 && event.type != type)
                continue;

            boolean hit = true;
            byte[] pattern = event.bin;
            for (int i = 0 ; i < pattern.length ; ++i) {
                if (pattern[i] != bin[offset + i]) {
                    hit = false;
                    break;
                }
            }

            if (hit)
                return event;
        }
        return null;
    }
}
