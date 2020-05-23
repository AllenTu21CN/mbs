package cn.lx.mbs.impl.structures;

import com.google.gson.JsonObject;
import cn.lx.mbs.impl.base.DIRTableMgr;

import java.util.List;
import java.util.Map;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

public class TemplateInfo extends MeetingParam {

    public String user;    // 所有者
    public String desc;    // 模板信息
    public Map<DirectingType, String/*path*/> directingTables;  // 导播表(路径)

    public TemplateInfo(String name, String user, String desc, String number, List<TSRole> sources,
                        List<CallingParam> remotes, Bandwidth bandwidth, Resolution resolution,
                        int framerate, CodecType vCodec, boolean defaultInDiscussion, boolean defaultVideoExt,
                        boolean defaultRoomName, boolean defaultLocalLive, boolean defaultLocalRecording, boolean defaultTracking,
                        Map<DirectingType, DirectingMode> defaultDirectingMode,
                        Map<DirectingType, String> directingTables) {
        super(name, number, sources, remotes, bandwidth, resolution, framerate, vCodec,
                defaultInDiscussion, defaultVideoExt, defaultRoomName, defaultLocalLive,
                defaultLocalRecording, defaultTracking, defaultDirectingMode, null);
        this.user = user;
        this.desc = desc;
        this.directingTables = directingTables;
    }

    public void loadDirectingContent() {
        if (directingTables == null)
            return;

        for (Map.Entry<DirectingType, String> entry: directingTables.entrySet()) {
            String path = entry.getValue();
            JsonObject content = DIRTableMgr.getDirectingContent(path);
            setDirectingContent(entry.getKey(), content);
        }
    }
}
