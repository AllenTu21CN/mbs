package cn.sanbu.avalon.endpoint3.director;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sanbu.base.BaseError;

import java.util.LinkedList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.director.structures.Event;
import cn.sanbu.avalon.endpoint3.director.impl.LayoutMapper;
import cn.sanbu.avalon.endpoint3.director.impl.FixedRoleListKeeper;
import cn.sanbu.avalon.endpoint3.director.impl.LogicHandler;
import cn.sanbu.avalon.endpoint3.director.impl.PriorityQueuer;
import cn.sanbu.avalon.endpoint3.director.impl.SimpleHandler;
import cn.sanbu.avalon.endpoint3.director.structures.Layout;

// 自动导播功能类
public class Director {

    private LayoutMapper mLayoutMapper;
    private List<LogicHandler> mSubHandlers;

    public Director() {

    }

    public int init(String owner, String content, List<String> validRoles) {
        try {
            // trans directing content to Object
            JsonObject json = new Gson().fromJson(content, JsonObject.class);
            return init(owner, json, validRoles);
        } catch (JsonSyntaxException e) {
            return BaseError.INVALID_PARAM;
        }
    }

    public int init(String owner, JsonObject content, List<String> validRoles) {
        if (mLayoutMapper != null)
            return 0;
        if (content == null)
            return BaseError.INVALID_PARAM;

        // check base items
        JsonElement layout = content.get("layout");
        JsonElement subStates = content.get("subStates");
        if (layout == null)
            return BaseError.INVALID_PARAM;
        if (subStates != null && !subStates.isJsonArray())
            return BaseError.INVALID_PARAM;

        // create LayoutMapper from config
        LayoutMapper mapper = new LayoutMapper();
        int ret = mapper.init(owner, layout.getAsJsonObject(), null, validRoles);
        if (ret != 0)
            return ret;

        // create LogicHandlers from config
        JsonArray array = subStates.getAsJsonArray();
        List<LogicHandler> handlers = new LinkedList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject())
                return BaseError.INVALID_PARAM;

            JsonObject config = element.getAsJsonObject();
            JsonElement logicId = config.get("logicId");
            if (logicId == null)
                return BaseError.INVALID_PARAM;

            LogicHandler handler = createHandler(logicId.getAsString());
            if (handler == null)
                return BaseError.ACTION_UNSUPPORTED;

            ret = handler.init(owner, config, mapper, validRoles);
            if (ret != 0)
                return ret;

            handlers.add(handler);
        }
        handlers.add(mapper);

        // keep all the handlers
        mLayoutMapper = mapper;
        mSubHandlers = handlers;
        return 0;
    }

    public void release() {
        if (mSubHandlers != null) {
            for (LogicHandler handler: mSubHandlers)
                handler.release();
            mSubHandlers.clear();
            mSubHandlers = null;
        }

        if (mLayoutMapper != null) {
            mLayoutMapper.release();
            mLayoutMapper = null;
            return;
        }
    }

    public Layout[] pushEvent(Event event) {
        for (LogicHandler subHandler : mSubHandlers) {
            List<Event> events = subHandler.pushEvent(event);
            if (events == null) {
                continue;
            }
            for (Event event1 : events) {
                pushEvent(event1);
            }
        }
        return mLayoutMapper.getLayout();
    }

    private static LogicHandler createHandler(String logicId) {
        if (logicId.equals("BI_SIMPLE"))
            return new SimpleHandler();
        else if (logicId.equals("BI_PRIORITY"))
            return new PriorityQueuer();
        else if (logicId.equals("BI_FIXED_ROLE_LIST"))
            return new FixedRoleListKeeper();
        else
            return null;
    }
}
