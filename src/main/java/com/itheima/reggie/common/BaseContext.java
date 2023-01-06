package com.itheima.reggie.common;


/**
 * 基于ThreadLocal封装工具类，用户保存和获取当前登录用户id
 * 同一个请求使用同一个线程，ThreadLocal可以在一个线程的任意地方使用和获取
 */
public class BaseContext {
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }
}
