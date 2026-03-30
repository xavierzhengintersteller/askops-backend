package com.scb.askopt_backend.vo;

import lombok.Data;

@Data
public class AgentVO {
    private Long agentId;    // id
    private String ip;       // ip
    private String name;     // name（你用 name 而非 hostname）
    private Integer port;    // port
    private Long groupId;    // 所属组
}