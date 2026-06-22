package com.scb.askopt_backend.task;

import com.scb.askopt_backend.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentHeartbeatScanTask {
    private final AgentService agentService;

    // 每10秒扫描一次离线Agent
    @Scheduled(fixedRate = 10000)
    public void scanOfflineAgent() {
        agentService.scanTimeoutAgent();
    }
}