package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.dto.agent.*;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.AgentMapper;
import com.scb.askopt_backend.context.AuthContext;
import com.scb.askopt_backend.service.AgentService;
import com.scb.askopt_backend.vo.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Agent 心跳接口
     * Get /api/agent/health
     * 只查数据库，不做网络请求
     */
    @GetMapping("/health")
    public ApiResponse<AgentHealthResponse> heartbeat() {
        AgentHealthResponse success = agentService.getHeartbeat();
        return ApiResponse.success(success);
    }
    /**
     * Agent 注册接口
     * Post /api/agent/register
     * 请求体：AgentRegisterRequest
     * 响应体：AgentRegisterResponse
     */
    @PostMapping("register")
    public ApiResponse<AgentRegisterResponse> register(@Valid @RequestBody AgentRegisterRequest request) {
        AgentRegisterResponse registerResponse  = agentService.register(request);
        return ApiResponse.success(registerResponse );
    }
    @GetMapping("/nodes")
    public ApiResponse<List<AgentIpPortDTO>> getCurrentUserNodes() {
        Long userId = AuthContext.getUserId();
        List<AgentIpPortDTO> list = agentMapper.findAgentsByUserId(userId);

        // 👇 这里加判断：空列表 → 返回 100001 错误码
        if (list == null || list.isEmpty()) {
            return ApiResponse.error(
                    ResultCodeEnum.NO_AGENT.getCode(),
                    ResultCodeEnum.NO_AGENT.getMessage()
            );
        }

        return ApiResponse.success(list);
    }
}