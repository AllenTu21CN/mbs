package cn.sanbu.avalon.endpoint3.director.structures;

import java.util.List;

public interface StateObserver {
    void onChanged(String tokenName, String tokenValue, List<Value> status);
}
