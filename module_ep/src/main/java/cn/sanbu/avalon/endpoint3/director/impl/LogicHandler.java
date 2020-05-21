package cn.sanbu.avalon.endpoint3.director.impl;

import com.google.gson.JsonObject;

import java.util.List;

import cn.sanbu.avalon.endpoint3.director.structures.Event;
import cn.sanbu.avalon.endpoint3.director.structures.StateObserver;

public interface LogicHandler {
    int init(String owner, JsonObject config,
             StateObserver observer, List<String> validRoles);
    void release();
    String getDescription();
    List<Event> pushEvent(Event event);
}
