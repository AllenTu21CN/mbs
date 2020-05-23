package cn.lx.mbs.impl.structures;

import com.google.gson.JsonObject;
import com.sanbu.tools.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.Resolution;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;

public class MeetingParam {
    public final String name;                     // 会议/模板名称
    public final String number;                   // 会议号码

    public final List<TSRole> sources;            // 可用本地信号源(角色)
    public final List<CallingParam> remotes;      // 远程(终端/教室)列表

    public final Bandwidth bandwidth;             // 带宽
    public final Resolution resolution;           // 分辨率(视频质量)
    public final int framerate;                   // 帧率
    public final CodecType vCodec;                // 视频格式

    public final boolean defaultInDiscussion;     // 默认进入讨论模式
    public final boolean defaultVideoExt;         // 默认允许双流
    public final boolean defaultRoomName;         // 默认显示会场名
    public final boolean defaultLocalLive;        // 默认打开本地直播
    public final boolean defaultLocalRecording;   // 默认打开本地录制
    public final boolean defaultTracking;         // 默认打开摄像机跟踪
    public final Map<DirectingType, DirectingMode> defaultDirectingMode;   // 默认导播模式

    // 导播表(内容)
    public Map<DirectingType, Object/*content*/> directingContents = new HashMap<>(3);

    public MeetingParam(String name, String number, List<TSRole> sources, List<CallingParam> remotes,
                        Bandwidth bandwidth, Resolution resolution, int framerate, CodecType vCodec,
                        boolean defaultInDiscussion, boolean defaultVideoExt, boolean defaultRoomName,
                        boolean defaultLocalLive, boolean defaultLocalRecording, boolean defaultTracking,
                        Map<DirectingType, DirectingMode> defaultDirectingMode,
                        Map<DirectingType, Object> directingContents) {
        this.name = name;
        this.number = number;
        this.sources = sources;
        this.remotes = remotes;
        this.bandwidth = bandwidth;
        this.resolution = resolution;
        this.framerate = framerate;
        this.vCodec = vCodec;
        this.defaultInDiscussion = defaultInDiscussion;
        this.defaultVideoExt = defaultVideoExt;
        this.defaultRoomName = defaultRoomName;
        this.defaultLocalLive = defaultLocalLive;
        this.defaultLocalRecording = defaultLocalRecording;
        this.defaultTracking = defaultTracking;
        this.defaultDirectingMode = defaultDirectingMode;
        this.directingContents = new HashMap<>(directingContents);
    }

    public MeetingParam(MeetingParam other) {
        this(other.name, other.number, other.sources, other.remotes,
                other.bandwidth, other.resolution, other.framerate,
                other.vCodec, other.defaultInDiscussion, other.defaultVideoExt,
                other.defaultRoomName, other.defaultLocalLive,
                other.defaultLocalRecording, other.defaultTracking,
                other.defaultDirectingMode, other.directingContents);
    }

    public void setDirectingContent(DirectingType type, String content) {
        if (directingContents == null)
            directingContents = new HashMap<>(3);
        directingContents.put(type, content);
    }

    public void setDirectingContent(DirectingType type, JsonObject content) {
        if (directingContents == null)
            directingContents = new HashMap<>(3);
        directingContents.put(type, content);
    }

    public boolean isValid() {
        return !StringUtil.isEmpty(name) &&
                sources != null && sources.size() > 0 && remotes != null &&
                bandwidth != null && bandwidth != Bandwidth.Unknown &&
                resolution != null && resolution != Resolution.RES_UNKNOWN &&
                framerate > 0 && vCodec != null && vCodec.type == DataType.VIDEO &&
                defaultDirectingMode != null && directingContents != null;
    }
}
