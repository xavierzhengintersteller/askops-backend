package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class PermissionService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Transactional
    public void updateUserPermission(Long userId) {

        // 修改 role_permission 表

        // 🔥 version++
        userMapper.incrementPermissionVersion(userId);

        Long newVersion = userMapper.getPermissionVersion(userId);

        // 重新查权限
        Set<Long> newPerms =
                permissionMapper.findPermissionIdsByUserId(userId);

        // 🔥 更新 Redis
        redisUtil.set("auth:ver:" + userId, newVersion, 7*24*60*60);
        redisUtil.set("auth:perm:" + userId, newPerms, 7*24*60*60);
    }
}

