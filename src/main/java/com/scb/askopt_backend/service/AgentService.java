package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.askopt_backend.constant.AgentStatus;
import com.scb.askopt_backend.dto.agent.*;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.mapper.AgentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent management service responsible for handling agent registration, heartbeat, and other related operations.
 * This service will interact with the database to store and retrieve agent information, and also handle the business logic related to agent lifecycle management.
 */
@Slf4j
@Service
public class AgentService {
    @Autowired
    private AgentMapper agentMapper;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HmacRequestSigner signer;


    public AgentRegisterResponse register (AgentRegisterRequest request) {
        AgentRegisterResponse response = new AgentRegisterResponse();
        try {
            Agent agent = new Agent();
            agent.setName(request.getName());
            agent.setIp(request.getIp());
            agent.setPort(request.getPort());
            agent.setGroupId(request.getGroupId());
            agent.setStatus(AgentStatus.REGISTERED.getCode());
            agent.setLastHeartbeatTime(LocalDateTime.now());
            // save to DB
            agentMapper.insertAgent(agent);
            // 构建成功响应
            response.setAgentId(agent.getId());
            response.setSuccess(true);
            response.setMessage("Agent注册成功");
        } catch (Exception e) {
            log.error("Agent注册失败", e);
            response.setSuccess(false);
            response.setMessage("Agent注册失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 只查数据库，不做网络请求
     */
    public AgentHealthResponse getHeartbeat() {
        List<Agent> agents = agentMapper.findAllAgents();
        List<AgentDTO> result = new ArrayList<>();
        long success = 0;
        long failed = 0;
        for (Agent agent : agents) {
            AgentDTO item = new AgentDTO();
            item.setId(agent.getId());
            item.setName(agent.getName());
            item.setIp(agent.getIp());
            item.setPort(agent.getPort());
            item.setStatus(agent.getStatus());
            item.setLastHeartbeatTime(agent.getLastHeartbeatTime());
            result.add(item);
            if (AgentStatus.ONLINE.getCode().equals(agent.getStatus())) {
                success++;
            } else if (AgentStatus.OFFLINE.getCode().equals(agent.getStatus())) {
                failed++;
            }
            // DISABLED / UNKNOWN 可以不计入 failed
        }
        AgentHealthResponse response = new AgentHealthResponse();
        response.setTotal((long) agents.size());
        response.setAgents(result);
        response.setSuccess(success);
        response.setFailed(failed);
        return response;
    }
}