package sanp.avalon.libs.base.utils;

public class Tuple3<A, B, C> extends Tuple<A, B> {
    public final C third;
    public Tuple3(A a, B b, C c) {
        super(a, b);
        this.third = c;
    }
}
