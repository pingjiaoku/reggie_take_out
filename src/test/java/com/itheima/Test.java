package com.itheima;

public class Test<T extends AppTest> {
    private T t;

    public Test(T t) {
        this.t = t;
    }

    public String toString() {
        return t.toString();
    }
}
