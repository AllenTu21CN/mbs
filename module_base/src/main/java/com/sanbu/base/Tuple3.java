package com.sanbu.base;

/**
 * Created by Tuyj on 2017/10/15.
 */

public class Tuple3<A, B, C> {
    public final A first;
    public final B second;
    public final C third;

    public Tuple3(A a, B b, C c) {
        this.first = a;
        this.second = b;
        this.third = c;
    }
}

