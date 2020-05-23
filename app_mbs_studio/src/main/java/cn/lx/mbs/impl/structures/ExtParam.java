package cn.lx.mbs.impl.structures;

public class ExtParam {
    public final TSRole videoExtRole;
    public final RoomNameStyle defaultRoomNameStyle;
    public final EPBaseConfig defaultBaseConfig;
    public final EPVideoCapability videoCapability;
    public final EPAudioCapability audioCapability;

    public ExtParam(TSRole videoExtRole, RoomNameStyle defaultRoomNameStyle,
                    EPBaseConfig defaultBaseConfig, EPVideoCapability videoCapability,
                    EPAudioCapability audioCapability) {
        this.videoExtRole = videoExtRole;
        this.defaultRoomNameStyle = defaultRoomNameStyle;
        this.defaultBaseConfig = defaultBaseConfig;
        this.videoCapability = videoCapability;
        this.audioCapability = audioCapability;
    }
}
