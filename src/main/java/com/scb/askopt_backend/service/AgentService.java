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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public AgentHealthResponse heartbeat() {
        List<Agent> agents = agentMapper.findAllAgents();
        List<AgentDTO> result = new ArrayList<>();
        String urlPath = "/health";
        long success = 0;
        long failed = 0;

        for (Agent agent : agents) {
            String agentUrl = "http://" + agent.getIp() + ":" + agent.getPort();
            AgentDTO item = new AgentDTO();

            item.setId(agent.getId());
            item.setName(agent.getName());
            item.setIp(agent.getIp());
            item.setPort(agent.getPort());
            try {
                HttpHeaders headers = new HttpHeaders();
                signer.sign(HttpMethod.GET, urlPath, "", headers);

                HttpEntity<String> entity = new HttpEntity<>("", headers);
                ResponseEntity<String> response = restTemplate.exchange(
                        agentUrl + urlPath,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    // 1. 替换硬编码 → 使用枚举的code值
                    item.setStatus(AgentStatus.ONLINE.getCode());
                    success++;
                    agentMapper.updateStatus(agent.getId(), AgentStatus.ONLINE.getCode());
                    agentMapper.updateHeartbeatTime(agent.getId(), LocalDateTime.now());

                } else {
                    // 2. 替换硬编码 → 使用枚举的code值
                    item.setStatus(AgentStatus.OFFLINE.getCode());
                    failed++;
                    agentMapper.updateStatus(agent.getId(), AgentStatus.OFFLINE.getCode());
                }
            } catch (Exception e){
                log.warn("Agent {} health check failed: {}", agent.getName(), e.getMessage());
                // 3. 替换硬编码 → 使用枚举的code值
                item.setStatus(AgentStatus.OFFLINE.getCode());
                failed++;
                agentMapper.updateStatus(agent.getId(), AgentStatus.OFFLINE.getCode());
            }
            item.setLastHeartbeatTime(LocalDateTime.now());
            result.add(item);
        }
        AgentHealthResponse response = new AgentHealthResponse();
        response.setTotal((long) agents.size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setAgents(result);
        return response;
    }

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
}