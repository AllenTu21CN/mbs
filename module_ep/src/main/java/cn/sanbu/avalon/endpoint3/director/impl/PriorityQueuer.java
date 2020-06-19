package cn.sanbu.avalon.endpoint3.director.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sanbu.base.BaseError;
import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;
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
 *  . logicId: BI_PRIORITY
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

    private StaticEvent mLastEvent;
    private int mLastObjId;

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
        mLastEvent = null;
        mLastObjId = -1;
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
        if (staticEvt.top != null)
            updateStatus4Priority(staticEvt, objId);
        else if (staticEvt.disable != null)
            updateStatus4Disable(staticEvt, objId);
        else
            LogUtil.w(DIRUtils.TAG, mName, "invalid event: " + new Gson().toJson(staticEvt));
    }

    private void updateStatus4Priority(StaticEvent staticEvt, int objId) {
        if (staticEvt != null) {
            mLastEvent = staticEvt;
            mLastObjId = objId;
        }

        boolean top = (staticEvt != null && CompareHelper.isEqual(staticEvt.top, "true"));
        List<Value> status = new LinkedList<>();

        if (top) {
            if (getValue(mStatus, staticEvt.value, -1) != null)
                status.add(new Value(staticEvt.value, objId));
        }

        for (String value: mDefaultValues) {
            Value current = getValue(mStatus, value, -1);
            if (current == null)
                continue;
            if (top && value.equals(staticEvt.value))
                continue;

            int id = current.objId;
            status.add(new Value(value, id));
        }

        mStatus = status;
    }

    private void updateStatus4Disable(StaticEvent staticEvt, int objId) {
        boolean disabled = CompareHelper.isEqual(staticEvt.disable, "true");

        if (disabled) {
            Iterator<Value> iterator = mStatus.iterator();
            while (iterator.hasNext()) {
                Value item = iterator.next();
                if (staticEvt.value.equals(item.eventValue)) {
                    iterator.remove();
                    break;
                }
            }
        } else {
            Value value = getValue(mStatus, staticEvt.value, -1);
            if (value == null)
                mStatus.add(new Value(staticEvt.value, objId));
            updateStatus4Priority(mLastEvent, mLastObjId);
        }
    }

    private static Value getValue(List<Value> values, String value, int objId) {
        Iterator<Value> iterator = values.iterator();
        while (iterator.hasNext()) {
            Value item = iterator.next();
            if (!value.equals(item.eventValue))
                continue;
            if (objId == -1 || objId == item.objId)
                return item;
        }
        return null;
    }
}
