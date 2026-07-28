package com.scb.tb.askopt_backend.controller;

import com.scb.tb.askopt_backend.dto.agent.*;
import com.scb.tb.askopt_backend.dto.agent.AgentHeartbeatDTO;
import com.scb.tb.askopt_backend.dto.agent.AgentRegisterDTO;
import com.scb.tb.askopt_backend.mapper.AgentMapper;
import com.scb.tb.askopt_backend.service.AgentService;
import com.scb.tb.askopt_backend.vo.ApiResponse;
import jakarta.validation.Valid;
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
    @Autowired
    private AgentMapper agentMapper;
    /**
     * Go Agent启动注册接口
     */
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody AgentRegisterDTO dto) {
        String msg = agentService.register(dto);
        return ApiResponse.success(msg);
    }

    /**
     * Go Agent定时心跳上报
     */
    @PostMapping("/heartbeat")
    public ApiResponse<String> heartbeat(@RequestBody AgentHeartbeatDTO dto) {
        String msg = agentService.heartbeat(dto);
        return ApiResponse.success(msg);
    }

}