package sanp.tools.utils;

/**
 * Created by Tuyj on 2017/10/15.
 */

public class Tuple3<A, B, C> extends Tuple<A, B> {
    public final C third;
    public Tuple3(A a, B b, C c) {
        super(a, b);
        this.third = c;
    }
}
