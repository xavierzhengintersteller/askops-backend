package com.scb.askopt_backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Agent 服务器实体（存储到数据库）
 */
@Data
public class Agent {
    /**
     * Agent 唯一标识
     */
    private Long id;

    /**
     * Agent IP 地址
     */
    private String ip;

    /**
     * Agent 端口
     */
    private Integer port;

    /**
     * 所属分组（用于权限控制）
     */
    private Long groupId;

    /**
     * Agent 名称（唯一）
     */
    private String name;

    /**
     * 状态：ONLINE(在线)、OFFLINE(离线)、REGISTERING(注册中)
     */
    private String status;

    /**
     * 最后心跳时间（用于检测离线）
     */
    private LocalDateTime lastHeartbeatTime;

    /**
     * 注册时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}