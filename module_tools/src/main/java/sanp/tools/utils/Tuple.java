package sanp.tools.utils;

/**
 * Created by Tuyj on 2017/10/15.
 */

public class Tuple<A, B> {
    public final A first;
    public final B second;

    public Tuple(A a, B b) {
        this.first = a;
        this.second = b;
    }
}

