package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.dto.ContainerInfo;
import com.scb.askopt_backend.mapper.AgentMapper;
import com.scb.askopt_backend.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Service
public class PodmanService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HmacRequestSigner signer;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private WebClient webClient;

    public Flux<String> streamContainerLogs(String containerName) {
        String path = "/containers/" + containerName + "/logs/sse";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
        signer.sign(HttpMethod.GET, path, "", headers); // 复用签名逻辑

        return webClient.get()
                .uri(path)
                .headers(httpHeaders -> httpHeaders.addAll(headers)) // 使用签名头
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(s -> System.out.println("Start streaming logs: " + containerName))
                .doOnCancel(() -> System.out.println("Client disconnected: " + containerName))
                .doOnError(err -> System.err.println("Log stream error: " + err.getMessage()));
    }

    private static final String AGENT_URL = "http://172.29.124.186:8080";

    public void restart(String containerName) {
        Long userId = AuthContext.getUserId();
        AgentIpPortDTO singleAgent = agentMapper.findAgentsByUserId(userId);

        if (singleAgent == null) {
            throw new RuntimeException("用户没有可用的 agent");
        }
        String agentUrl = "http://" + singleAgent.getIp() + ":" + singleAgent.getPort();
        String urlPath = "/containers/" + containerName + "/restart";
        HttpHeaders headers = new HttpHeaders();
        // 可以加签
        signer.sign(HttpMethod.POST, urlPath, "", headers);

        HttpEntity<String> entity = new HttpEntity<>("", headers);
        restTemplate.exchange(agentUrl + urlPath, HttpMethod.POST, entity, Void.class);
    }

    public String getAllContainer() {
        String path = "/containers";
        HttpHeaders headers = new HttpHeaders();
        signer.sign(HttpMethod.GET, path, "", headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                AGENT_URL + path,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody(); // 原始 JSON 透传
    }

    public List<String>  rawLogs(String name, int lines) {
        String path = "/containers/" + name + "/logs?lines=" +lines ;
        HttpHeaders headers = new HttpHeaders();
        signer.sign(HttpMethod.GET, path, "", headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                AGENT_URL + path,
                HttpMethod.GET,
                entity,
                String.class
        );
        return Arrays.asList(response.getBody()); // 转成 List<String>
    }

}