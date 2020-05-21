package cn.sanbu.avalon.endpoint3.director.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sanbu.base.BaseError;
import com.sanbu.tools.CompareHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.director.structures.Event;
import cn.sanbu.avalon.endpoint3.director.structures.LogicParam;
import cn.sanbu.avalon.endpoint3.director.structures.StaticEvent;
import cn.sanbu.avalon.endpoint3.director.structures.Value;
import cn.sanbu.avalon.endpoint3.director.structures.StateObserver;
import cn.sanbu.avalon.endpoint3.director.structures.TokenType;
import cn.sanbu.avalon.endpoint3.director.structures.ValueType;

/* 简单的优先级列表处理模型:
 *  . 根据配置初始化一个列表
 *  . 根据事件将指定值置顶或者恢复默认顺序
 *  . 并维护指定名称的token,用于生成查找最终场景的key
 * */
public class PriorityQueuer implements LogicHandler {

    private static final String TAG = PriorityQueuer.class.getSimpleName();

    private TokenType mTokenType;
    private String mTokenName;
    private ValueType mValueType;
    private List<StaticEvent> mRecvEvents;
    private List<Event> mSendEvents;
    private List<String> mDefaultValues;

    private String mName;
    private JsonObject mConfig;
    private StateObserver mObserver;
    private List<String> mValidRoles;
    private List<Value> mStatus;

    @Override
    public int init(String owner, JsonObject config, StateObserver observer, List<String> validRoles) {
        if (mConfig != null)
            return 0;

        // parse logic param

        LogicParam param = DIRUtils.parseLogicParam(config);
        if (param == null)
            return BaseError.INVALID_PARAM;

        JsonElement value = config.get("defaultValue");
        if (value == null || !value.isJsonArray())
            return BaseError.INVALID_PARAM;
        List<String> defaultValues = new Gson().fromJson(value, new TypeToken<List<String>>() {
        }.getType());

        // update logic param
        mTokenType = param.tokenType;
        mTokenName = param.tokenName;
        mValueType = param.valueType;
        mRecvEvents = param.recvEvents;
        mSendEvents = new ArrayList<>(param.sendEvents.size());
        mDefaultValues = new ArrayList<>(defaultValues.size());

        for (StaticEvent event: param.sendEvents)
            mSendEvents.add(new Event(event.id));

        // init values
        mName = TAG + "@" + owner;
        mConfig = config;
        mObserver = observer;
        mValidRoles = validRoles == null ? null : new ArrayList<>(validRoles);
        mStatus = new LinkedList<>();

        // init origin status
        for (String v: defaultValues) {
            if (isValidValue(v)) {
                mDefaultValues.add(v);
                mStatus.add(new Value(v, -1));
            }
        }

        // callback inited status
        String tokenValue = DIRUtils.getTokenValueByStatus(mTokenType, mStatus);
        mObserver.onChanged(mTokenName, tokenValue, mStatus);
        return 0;
    }

    @Override
    public void release() {
        mTokenType = null;
        mTokenName = null;
        mValueType = null;
        mRecvEvents = null;
        mSendEvents = null;
        mDefaultValues = null;

        mConfig = null;
        mObserver = null;
        mValidRoles = null;
        mStatus = null;
    }

    @Override
    public String getDescription() {
        return mConfig.get("desc").getAsString();
    }

    @Override
    public List<Event> pushEvent(Event event) {
        for (StaticEvent staticEvt: mRecvEvents) {
            if (staticEvt.id == event.id) {

                // skip invalid value
                if (!isValidValue(staticEvt.value))
                    return null;

                // update status
                updateStatus(staticEvt, event.objId);

                // callback status
                String tokenValue = DIRUtils.getTokenValueByStatus(mTokenType, mStatus);
                mObserver.onChanged(mTokenName, tokenValue, mStatus);

                // update result events
                for (Event evt: mSendEvents)
                    evt.update(event.objId, event.data);
                return mSendEvents;
            }
        }
        return null;
    }

    private boolean isValidValue(String value) {
        return mValueType != ValueType.ROLE || mValidRoles == null ||
                value == null || mValidRoles.contains(value);
    }

    private void updateStatus(StaticEvent staticEvt, int objId) {
        boolean top = CompareHelper.isEqual(staticEvt.top, "true");

        // skip the needless set
        if (top) {
            Value first = mStatus.get(0);
            if (staticEvt.value.equals(first.eventValue) && objId == first.objId)
                return;
        }

        List<Value> status = new LinkedList<>();

        if (top)
            status.add(new Value(staticEvt.value, objId));

        for (String value: mDefaultValues) {
            if (top && value.equals(staticEvt.value))
                continue;

            objId = -1;
            for (Value old: mStatus) {
                if (old.eventValue.equals(value)) {
                    objId = old.objId;
                    break;
                }
            }
            status.add(new Value(value, objId));
        }

        mStatus = status;
    }
}
