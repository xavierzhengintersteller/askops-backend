package com.scb.askopt_backend.security;

import lombok.experimental.UtilityClass;

/**
 * 存储当前请求用户信息的工具类（基于 ThreadLocal）
 */
@UtilityClass
public class AuthContext {

    // 每个线程独立存储 Long 类型的用户ID
    private final ThreadLocal<Long> USER_ID_CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     * @param userId 数据库中的用户ID（Long类型）
     */
    public void setUserId(Long userId) {
        USER_ID_CONTEXT.set(userId);
    }

    /**
     * 获取当前线程的用户ID
     * @return 若未设置返回null
     */
    public Long getUserId() {
        return USER_ID_CONTEXT.get();
    }

    /**
     * 清理当前线程的用户ID信息，避免线程池复用导致数据残留
     */
    public void clear() {
        USER_ID_CONTEXT.remove();
    }
}