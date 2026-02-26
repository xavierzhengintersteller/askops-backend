package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.AgentRegister.*;
import com.scb.askopt_backend.service.AgentService;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 管理API控制器
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;

    /**
     * Agent 注册接口
     * POST /api/agent/register
     */
    @PostMapping("/register")
    public ApiResponse<AgentRegisterResponse> register(@RequestBody AgentRegisterRequest request) {
        try {
            AgentRegisterResponse response = agentService.register(request);
            return  ApiResponse.success(response);
        } catch (Exception e) {
            return  ApiResponse.error(500, "注册失败：" + e.getMessage());
        }
    }

    /**
     * Agent 心跳接口
     * PUT /api/agent/health
     */
    @PutMapping("/health")
    public ApiResponse<Boolean> heartbeat(@RequestBody AgentHeartbeatRequest request) {
        try {
            Boolean success = agentService.heartbeat(request);
            if (success) {
                return  ApiResponse.success( true);
            } else {
                return  ApiResponse.error(400, "Agent不存在或IP不匹配");
            }
        } catch (Exception e) {
            return  ApiResponse.error(500, "心跳失败：" + e.getMessage());
        }
    }

    /**
     * Agent 数据上报接口
     * POST /api/agent/report
     */
    @PostMapping("/report")
    public ApiResponse<Boolean> report(@RequestBody AgentReportRequest request) {
        try {
            Boolean success = agentService.report(request);
            if (success) {
                return  ApiResponse.success(true);
            } else {
                return  ApiResponse.error(400, "Agent不存在或IP不匹配");
            }
        } catch (Exception e) {
            return  ApiResponse.error(500, "上报失败：" + e.getMessage());
        }
    }
}