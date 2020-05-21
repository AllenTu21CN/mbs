package cn.sanbu.avalon.endpoint3.director.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sanbu.base.BaseError;
import com.sanbu.tools.StringUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.director.structures.Event;
import cn.sanbu.avalon.endpoint3.director.structures.LogicParam;
import cn.sanbu.avalon.endpoint3.director.structures.Value;
import cn.sanbu.avalon.endpoint3.director.structures.StateObserver;
import cn.sanbu.avalon.endpoint3.director.structures.StaticEvent;
import cn.sanbu.avalon.endpoint3.director.structures.TokenType;
import cn.sanbu.avalon.endpoint3.director.structures.ValueType;

/* 简单状态处理模型:
*  . 根据接收事件配置的值来设置当前状态的值
*  . 并维护指定名称的token,用于生成查找最终场景的key
* */
public class SimpleHandler implements LogicHandler {

    private static final String TAG = SimpleHandler.class.getSimpleName();

    private TokenType mTokenType;
    private String mTokenName;
    private ValueType mValueType;
    private List<StaticEvent> mRecvEvents;
    private List<Event> mSendEvents;

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
        String defaultValue = value == null ? null : value.getAsString();

        // update logic param
        mTokenType = param.tokenType;
        mTokenName = param.tokenName;
        mValueType = param.valueType;
        mRecvEvents = param.recvEvents;
        mSendEvents = new ArrayList<>(param.sendEvents.size());

        for (StaticEvent event: param.sendEvents)
            mSendEvents.add(new Event(event.id));

        // init values
        mName = TAG + "@" + owner;
        mConfig = config;
        mObserver = observer;
        mValidRoles = validRoles == null ? null : new ArrayList<>(validRoles);
        mStatus = new LinkedList<>();

        // init status
        if (isValidValue(defaultValue))
            updateStatus(defaultValue, -1);

        // callback inited status
        String tokenValue = DIRUtils.getTokenValueByEvent(mTokenType, defaultValue);
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
                String eventValue = staticEvt.value;

                // skip invalid value
                if (!isValidValue(eventValue))
                    return null;

                // update status
                updateStatus(eventValue, event.objId);

                // callback status
                String tokenValue = DIRUtils.getTokenValueByEvent(mTokenType, eventValue);
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

    private void updateStatus(String eventValue, int objId) {
        mStatus.clear();
        if (!StringUtil.isEmpty(eventValue) && !eventValue.equals("None"))
            mStatus.add(new Value(eventValue, objId));
    }
}
