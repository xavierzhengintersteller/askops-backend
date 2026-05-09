package com.scb.askopt_backend.constant;

/**
 * 权限认证 + Redis 全局常量
 */
public abstract class RedisConstants {

    // ===================== Token 过期时间 =====================
    /** AccessToken 有效期：15分钟（毫秒） */
    public static final long ACCESS_TOKEN_EXPIRE_MS = 15 * 60_000L;

    /** RefreshToken 有效期：7天（秒） */
    public static final long REFRESH_TOKEN_EXPIRE_SEC = 7 * 24 * 60 * 60L;

    // ===================== Redis Key 前缀 =====================
    public static final String REDIS_REFRESH_TOKEN = "refresh:";
    public static final String REDIS_PERMISSION_VERSION = "auth:ver:";
    public static final String REDIS_PERMISSION_LIST = "auth:perm:";
    public static final String REDIS_BLACKLIST_USER = "blacklist:user:";

}