package cn.sanbu.avalon.director.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sanbu.base.BaseError;
import com.sanbu.tools.CompareHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.director.structures.Event;
import cn.sanbu.avalon.director.structures.LogicParam;
import cn.sanbu.avalon.director.structures.StaticEvent;
import cn.sanbu.avalon.director.structures.Value;
import cn.sanbu.avalon.director.structures.StateObserver;
import cn.sanbu.avalon.director.structures.TokenType;

/* 固定角色列表处理模型:
 *  . logicId: BI_FIXED_ROLE_LIST
 *  . 维护的列表中角色相同,仅objId不同,一般只会用作Caller角色的维护
 *  . 根据接收事件配置的值(+/-)来增加删除列表中的值
 *  . 并维护指定名称的token,用于生成查找最终场景的key
 * */
public class FixedRoleListKeeper implements LogicHandler {

    private static final String TAG = FixedRoleListKeeper.class.getSimpleName();

    private TokenType mTokenType;
    private String mTokenName;
    private List<StaticEvent> mRecvEvents;
    private List<Event> mSendEvents;
    private String mFixedRole;

    private boolean mWorked;
    private String mName;
    private JsonObject mConfig;
    private StateObserver mObserver;
    private List<Value> mStatus;

    @Override
    public int init(String owner, JsonObject config, StateObserver observer, List<String> validRoles) {
        if (mConfig != null)
            return 0;

        // parse logic param

        LogicParam param = DIRUtils.parseLogicParam(config);
        if (param == null)
            return BaseError.INVALID_PARAM;

        JsonElement role = config.get("role");
        if (role == null)
            return BaseError.INVALID_PARAM;

        // update logic param
        mTokenType = param.tokenType;
        mTokenName = param.tokenName;
        mRecvEvents = param.recvEvents;
        mSendEvents = new ArrayList<>(param.sendEvents.size());
        mFixedRole = role.getAsString();

        for (StaticEvent event: param.sendEvents)
            mSendEvents.add(new Event(event.id));

        // init values
        mName = TAG + "@" + owner;
        mConfig = config;
        mObserver = observer;
        mWorked = validRoles == null || validRoles.contains(mFixedRole);
        mStatus = new LinkedList<>();

        // callback inited status
        String tokenValue = DIRUtils.getTokenValueByStatus(mTokenType, mStatus);
        mObserver.onChanged(mTokenName, tokenValue, mStatus);
        return 0;
    }

    @Override
    public void release() {
        mTokenType = null;
        mTokenName = null;
        mRecvEvents = null;
        mSendEvents = null;
        mFixedRole = null;

        mConfig = null;
        mObserver = null;
        mStatus = null;
    }

    @Override
    public String getDescription() {
        return mConfig.get("desc").getAsString();
    }

    @Override
    public List<Event> pushEvent(Event event) {
        if (!mWorked)
            return null;

        for (StaticEvent staticEvt: mRecvEvents) {
            if (staticEvt.id == event.id) {
                // update status
                updateStatus(staticEvt.value, event.objId);

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

    private void updateStatus(String eventValue, int objId) {
        boolean add = CompareHelper.isEqual(eventValue, "+") ||
                CompareHelper.isEqual(eventValue, "add");

        if (add) {
            if (!containsValue(mStatus, objId))
                mStatus.add(new Value(mFixedRole, objId));
        } else {
            removeValue(mStatus, objId);
        }
    }

    private boolean containsValue(List<Value> values, int objId) {
        for (Value value : values) {
            if (value.objId == objId)
                return true;
        }
        return false;
    }

    private boolean removeValue(List<Value> values, int id) {
        Iterator<Value> it = values.iterator();
        while (it.hasNext()) {
            Value value = it.next();
            if (value.objId == id) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}
