package com.scb.tb.askopt_backend.vo;

import lombok.Data;

@Data
public class DashboardStatsVO {
    private Integer agentTotal;     // Agent 总数
    private Integer agentOnline;    // 在线 Agent
    private Integer agentOffline;   // 离线 Agent
    private Integer containerTotal;// 容器总数
}