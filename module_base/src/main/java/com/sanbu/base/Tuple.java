package com.sanbu.base;

/**
 * Created by Tuyj on 2017/10/15.
 */

public class Tuple<A, B> {
    public A first;
    public B second;

    public Tuple(A a, B b) {
        this.first = a;
        this.second = b;
    }
}

