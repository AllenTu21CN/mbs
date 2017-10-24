package sanp.avalon.libs.base.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by huang on 2017/6/20.
 */

public class Multimap<K,V> {
    private final HashMap<K,List<V>> store = new HashMap<K,List<V>>();
    /** retrieve a non-null list of values with key K */
    public List<V> getAll(K key) {
        List<V> values = store.get(key);
        return values != null ? values : Collections.<V>emptyList();
    }

    public void put(K key, V val) {
        List<V> curVals = store.get(key);
        if (curVals == null) {
            curVals = new ArrayList<V>(3);
            store.put(key, curVals);
        }
        curVals.add(val);
    }
}
