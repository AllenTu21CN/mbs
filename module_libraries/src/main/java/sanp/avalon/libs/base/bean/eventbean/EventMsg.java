package sanp.avalon.libs.base.bean.eventbean;

/**
 * Created by zdh on 2017/6/8.
 * EventBus消息类
 */

public class EventMsg {
    private String mMsg;

    public EventMsg(String msg) {
        mMsg = msg;
    }

    public String getMsg() {
        return mMsg;
    }
}
