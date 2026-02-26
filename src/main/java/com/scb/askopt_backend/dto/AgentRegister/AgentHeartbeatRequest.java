package com.scb.askopt_backend.dto.AgentRegister;

import lombok.Data; /**
 * Agent 心跳请求DTO
 */
@Data
public class AgentHeartbeatRequest {
    private Long agentId;       // Agent ID（必填）
    private String ip;          // 防篡改：携带IP校验
}
