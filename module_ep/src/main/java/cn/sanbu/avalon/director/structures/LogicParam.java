package cn.sanbu.avalon.director.structures;

import java.util.List;

public class LogicParam {
    public TokenType tokenType;
    public String tokenName;
    public ValueType valueType;
    public List<StaticEvent> recvEvents;
    public List<StaticEvent> sendEvents;

    public LogicParam(TokenType tokenType, String tokenName, ValueType valueType,
                      List<StaticEvent> recvEvents, List<StaticEvent> sendEvents) {
        this.tokenType = tokenType;
        this.tokenName = tokenName;
        this.valueType = valueType;
        this.recvEvents = recvEvents;
        this.sendEvents = sendEvents;
    }
}
