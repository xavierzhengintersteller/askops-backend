package com.scb.askopt_backend.dto.AgentRegister;

import com.scb.askopt_backend.dto.ContainerDTO;
import lombok.Data;

import java.util.List;

/**
 * Agent 上报容器数据DTO
 */
@Data
public class AgentReportRequest {
    private Long agentId;                   // Agent ID（必填）
    private String ip;                      // 防篡改：携带IP校验
    private List<ContainerDTO> containers;  // 容器列表数据
}
