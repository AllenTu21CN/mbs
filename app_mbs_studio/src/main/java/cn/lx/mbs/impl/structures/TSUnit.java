package cn.lx.mbs.impl.structures;

import com.sanbu.base.State;
import com.sanbu.tools.StringUtil;

// tsx200的最小控制单元
public class TSUnit {
    public String name;             // 显示名称
    public TSRole role;             // 所属角色
    public int parentId;            // 所属ID(sourceId or caller id)
    public boolean previewAble;     // 是否可预览
    public String previewUrl;       // 预览地址
    public State connState;         // 连接状态(None: 未连接, Doing: 连接中, Done: 已连接)

    // 针对本地角色有效的属性
    public State extState;          // 辅流状态(None: 不支持, Doing: 就绪, Done: 发送中)
    public State lockState;         // 锁定状态(None: 不支持, Doing: 就绪, Done: 锁定中)

    // 针对Caller/RemoteSource有效的属性
    public boolean inSpeaking;      // 是否在发言
    public boolean micMuted;        // 是否麦克风静音

    // 针对Caller有效的属性
    public boolean speakerMuted;    // 是否扬声器静音

    private TSUnit() {

    }

    private TSUnit(String name, TSRole role, int parentId, boolean previewAble,
                  String previewUrl, State connState, State extState, State lockState,
                  boolean inSpeaking, boolean micMuted, boolean speakerMuted) {
        this.name = name;
        this.role = role;
        this.parentId = parentId;
        this.previewAble = previewAble;
        this.previewUrl = previewUrl;
        this.connState = connState;
        this.extState = extState;
        this.lockState = lockState;
        this.inSpeaking = inSpeaking;
        this.micMuted = micMuted;
        this.speakerMuted = speakerMuted;
    }

    public static TSUnit buildEmpty() {
        return new TSUnit("", null, -1, false,
                "", State.None, State.None, State.None,
                false, false, false);
    }

    public static TSUnit buildLocalSource(TSRole role, int sourceId, String previewUrl,
                                          State connState, State extState, State lockState) {
        if (!role.local)
            throw new RuntimeException("invalid TSUnit");

        String name = "本地:" + role.desc;
        boolean previewAble = !StringUtil.isEmpty(previewUrl) && connState == State.Done;
        return new TSUnit(name, role, sourceId, previewAble,
                previewUrl, connState, extState, lockState,
                false, false, false);
    }

    public static TSUnit buildRemoteSource(String sourceName, int sourceId, String previewUrl, State connState) {
        String name = TSRole.RemoteSource.desc + ":" + sourceName;
        boolean previewAble = !StringUtil.isEmpty(previewUrl) && connState == State.Done;
        return new TSUnit(name, TSRole.RemoteSource, sourceId,
                previewAble, previewUrl, connState, State.None, State.None,
                false, false, false);
    }

    public static TSUnit buildCaller(String callerName, int callerId, String previewUrl, State connState,
                                     boolean inSpeaking, boolean micMuted, boolean speakerMuted) {
        String name = TSRole.Caller.desc + ":" + callerName;
        boolean previewAble = !StringUtil.isEmpty(previewUrl) && connState == State.Done;
        return new TSUnit(name, TSRole.Caller, callerId,
                previewAble, previewUrl, connState, State.None,
                State.None, inSpeaking, micMuted, speakerMuted);
    }

    public static TSUnit buildCallerPlus(int callerId, String previewUrl, State connState) {
        boolean previewAble = !StringUtil.isEmpty(previewUrl) && connState == State.Done;
        return new TSUnit(TSRole.CallerPlus.desc, TSRole.CallerPlus,
                callerId, previewAble, previewUrl, connState,
                State.Done, State.None, false, false, false);
    }
}
