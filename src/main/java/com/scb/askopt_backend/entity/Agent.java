package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_agent")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String ip;

    private Integer port;

    private Long groupId;

    private String status;

    @TableField("last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;

    @TableField("failcount")
    private Integer failCount;

    @TableField("heartbeat_timeout_sec")
    private Integer heartbeatTimeoutSec;

    @TableField("client_id")
    private String clientId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}