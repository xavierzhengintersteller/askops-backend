package com.scb.askopt_backend.security;

import lombok.experimental.UtilityClass;

/**
 * 存储当前请求用户信息的工具类（基于 ThreadLocal）
 */
@UtilityClass
public class AuthContext {

    // 每个线程独立存储 AuthUser
    private final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前线程的用户
     */
    public void set(String authUser) {
        CONTEXT.set(authUser);
    }

    /**
     * 获取当前线程的用户
     */
    public String get() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程的用户信息，避免线程池复用导致数据残留
     */
    public void clear() {
        CONTEXT.remove();
    }
}
