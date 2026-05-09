package com.scb.askopt_backend.service;

import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.vo.DashboardStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DashboardService {

    @Autowired
    private AgentService agentService;

    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        // 1. Agent 总数
        long agentTotal = agentService.count();

        // 2. 在线 Agent
        long agentOnline = agentService.lambdaQuery()
                .eq(Agent::getStatus, "ONLINE")
                .count();

        // 3. 离线 Agent
        long agentOffline = agentService.lambdaQuery()
                .eq(Agent::getStatus, "OFFLINE")
                .count();

        // 容器总数
        long containerTotal = 0;

        vo.setAgentTotal((int) agentTotal);
        vo.setAgentOnline((int) agentOnline);
        vo.setAgentOffline((int) agentOffline);
        vo.setContainerTotal((int) containerTotal);

        return vo;
    }
}