package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.agent.*;
import com.scb.askopt_backend.service.AgentService;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 管理API控制器
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;

    /**
     * Agent 心跳接口
     * Get /api/agent/health
     */
    @GetMapping("/health")
    public ApiResponse<AgentHealthResponse> heartbeat() {
            AgentHealthResponse success = agentService.heartbeat();
            return ApiResponse.success(success);
    }

}