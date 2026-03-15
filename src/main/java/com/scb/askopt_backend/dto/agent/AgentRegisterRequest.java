package com.scb.askopt_backend.dto.agent;

import lombok.Data;

@Data
public class AgentRegisterRequest {
    private String ip;          // Agent IP（必填）
    private Integer port;       // Agent 端口（必填）
    private Long groupId;   // 所属分组（必填）唯一
    private String name;        // Agent 名称（可选）
}
