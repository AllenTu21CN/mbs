package cn.sanbu.avalon.endpoint3.director.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import java.util.List;

import cn.sanbu.avalon.endpoint3.director.structures.LogicParam;
import cn.sanbu.avalon.endpoint3.director.structures.StaticEvent;
import cn.sanbu.avalon.endpoint3.director.structures.TokenType;
import cn.sanbu.avalon.endpoint3.director.structures.ValueType;

public class DIRUtils {

    public static final String TAG = "director";

    private static final String EXPECTED_ROLE_PREFIX = "#";

    public static LogicParam parseLogicParam(JsonObject config) {
        JsonElement type = config.get("tokenType");
        JsonElement name = config.get("tokenName");
        JsonElement type2 = config.get("valueType");
        JsonElement recv = config.get("recvEvents");
        JsonElement send = config.get("sendEvents");

        if (type == null || name == null || type2 == null ||
                recv == null || !recv.isJsonArray() ||
                send == null || !send.isJsonArray())
                return null;

        try {
            TokenType tokenType = TokenType.valueOf(type.getAsString());
            String tokenName = name.getAsString();
            ValueType valueType = ValueType.valueOf(type2.getAsString());
            List<StaticEvent> recvEvents = new Gson().fromJson(recv, new TypeToken<List<StaticEvent>>() {
            }.getType());
            List<StaticEvent> sendEvents = new Gson().fromJson(send, new TypeToken<List<StaticEvent>>() {
            }.getType());

            return new LogicParam(tokenType, tokenName, valueType, recvEvents, sendEvents);
        } catch (Exception e) {
            LogUtil.w(TAG, "parseLogicParam error", e);
            return null;
        }
    }

    public static String getTokenValueByStatus(TokenType type, List list) {
        if (type == TokenType.NOT_EMPTY)
            return (list != null && !list.isEmpty()) ? "true" : "false";
        else if (type == TokenType.LIST_SIZE)
            return list != null ? String.valueOf(list.size()) : "0";
        else
            throw new RuntimeException("invalid type(" + type.name() + ") for list");
    }

    public static String getTokenValueByEvent(TokenType type, String eventValue) {
        if (type == TokenType.NOT_NULL)
            return String.valueOf(!StringUtil.isEmpty(eventValue) && !eventValue.equals("None"));
        else if (type == TokenType.EVT_VALUE)
            return eventValue == null ? "None" : eventValue;
        else
            throw new RuntimeException("invalid type(" + type.name() + ") for list");
    }

    public static boolean isExpectingFixedRole(String expectant) {
        return expectant.startsWith(EXPECTED_ROLE_PREFIX);
    }

    public static String getExpectedFixedRole(String expectant) {
        return expectant.substring(EXPECTED_ROLE_PREFIX.length());
    }
}
