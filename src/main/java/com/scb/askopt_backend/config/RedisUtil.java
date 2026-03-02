package com.scb.askopt_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public final class RedisUtil {

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置 key（带过期时间，单位秒）
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis set 失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 获取 key
     */
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 key
     */
    public boolean del(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis delete 失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间（秒）
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis expire 失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis hasKey 失败: key={}", key, e);
            return false;
        }
    }
}