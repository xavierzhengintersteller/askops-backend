package com.scb.askopt_backend.service;


import com.scb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.askopt_backend.dto.agent.AgentHealthResponse;
import com.scb.askopt_backend.dto.agent.AgentRegisterRequest;
import com.scb.askopt_backend.dto.agent.AgentRegisterResponse;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.mapper.AgentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent management service responsible for handling agent registration, heartbeat, and other related operations.
 * This service will interact with the database to store and retrieve agent information, and also handle the business logic related to agent lifecycle management.
 */
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
        String urlPath = "/health";
        for (Agent agent : agents) {
            String agentUrl = "http://" + agent.getIp() + ":" + agent.getPort();
            HttpHeaders headers = new HttpHeaders();
            signer.sign(HttpMethod.GET, urlPath, "", headers);

            HttpEntity<String> entity = new HttpEntity<>("", headers);
            restTemplate.exchange(
                    agentUrl + urlPath,
                    HttpMethod.GET,
                    entity,
                    Void.class
            );
        }
        return null;
    }
}
