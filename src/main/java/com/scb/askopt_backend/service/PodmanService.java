package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.dto.ContainerInfoDTO;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.askopt_backend.dto.RestartContainer.RestartResult;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.exception.ApiException;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.AgentMapper;
import com.scb.askopt_backend.context.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PodmanService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HmacRequestSigner signer;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Qualifier("containerExecutor")
    @Autowired
    private Executor containerExecutor;

    @Autowired
    private RedisUtil redisUtil;

    private static final int EXPIRE = 25; // 缓存25秒
    private static final int PRELOAD_DELAY = 25000; // 25秒

    // ===================== 对外接口：支持 manual 强制刷新 =====================
    public List<ContainerInfoDTO> getContainers(List<String> nodeIps, boolean manual) {
        Long userId = AuthContext.getUserId();

        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        if (userAgents.isEmpty()) {
            throw new ApiException(
                    ResultCodeEnum.NO_AGENT.getCode(),
                    ResultCodeEnum.NO_AGENT.getMessage()
            );
        }

        List<AgentIpPortDTO> targetAgents;
        if (nodeIps != null && !nodeIps.isEmpty()) {
            targetAgents = userAgents.stream()
                    .filter(a -> nodeIps.contains(a.getIp()))
                    .collect(Collectors.toList());
        } else {
            targetAgents = userAgents;
        }

        if (manual) {
            log.info("手动刷新，拉取 {} 个节点最新数据", targetAgents.size());
            fetchAndCacheNodes(targetAgents);
        }

        return loadContainersFromCache(targetAgents);
    }

    // ===================== 从缓存加载多个节点 =====================
    private List<ContainerInfoDTO> loadContainersFromCache(List<AgentIpPortDTO> agents) {
        List<ContainerInfoDTO> result = new ArrayList<>();
        for (AgentIpPortDTO agent : agents) {
            String key = getNodeCacheKey(agent.getIp());
            try {
                Object obj = redisUtil.get(key);
                if (obj != null) {
                    List<ContainerInfoDTO> list = objectMapper.readValue((String) obj, new TypeReference<List<ContainerInfoDTO>>() {});
                    result.addAll(list);
                }
            } catch (Exception e) {
                log.error("加载节点 {} 缓存失败", agent.getIp(), e);
            }
        }
        return result;
    }

    // ===================== 并行拉取并按节点缓存 =====================
    private void fetchAndCacheNodes(List<AgentIpPortDTO> agents) {
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        List<CompletableFuture<Void>> futures = agents.stream()
                .map(agent -> CompletableFuture.runAsync(() -> {
                            try {
                                List<ContainerInfoDTO> list = fetchSingleAgent(agent);
                                String key = getNodeCacheKey(agent.getIp());
                                redisUtil.set(key, objectMapper.writeValueAsString(list), EXPIRE);
                                log.info("节点 {} 缓存成功，容器数：{}", agent.getIp(), list.size());
                                success.incrementAndGet();
                            } catch (Exception e) {
                                log.error("节点 {} 拉取失败", agent.getIp(), e);
                                fail.incrementAndGet();
                            }
                        }, containerExecutor)
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("节点 {} 超时（3s）", agent.getIp());
                            fail.incrementAndGet();
                            return null;
                        }))
                .collect(Collectors.toList());

        for (var f : futures) {
            try {
                f.get();
            } catch (Exception ignored) {}
        }

        log.info("===== 批量拉取完成 =====");
        log.info("总节点：{} | 成功：{} | 失败：{}", agents.size(), success.get(), fail.get());
    }

    // ===================== 单个节点缓存 KEY =====================
    private String getNodeCacheKey(String ip) {
        return "container:node:" + ip;
    }

    // ===================== 定时预热：全部节点 =====================
    @Scheduled(fixedDelay = PRELOAD_DELAY)
    public void preloadAllNodes() {
        try {
            List<Agent> all = agentMapper.selectList(Wrappers.emptyWrapper());
            if (all.isEmpty()) {
                log.info("无节点，跳过预热");
                return;
            }

            List<AgentIpPortDTO> list = all.stream()
                    .map(a -> {
                        AgentIpPortDTO dto = new AgentIpPortDTO();
                        dto.setIp(a.getIp());
                        dto.setPort(a.getPort());
                        return dto;
                    }).collect(Collectors.toList());

            fetchAndCacheNodes(list);
        } catch (Exception e) {
            log.error("定时预热失败", e);
        }
    }

    // ===================== 单个 Agent 请求 =====================
    private List<ContainerInfoDTO> fetchSingleAgent(AgentIpPortDTO agent) throws Exception {
        String ip = agent.getIp();
        String url = "http://" + ip + ":" + agent.getPort() + "/containers";

        HttpHeaders headers = new HttpHeaders();
        signer.sign(HttpMethod.GET, "/containers", "", headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        List<ContainerInfoDTO> containers = objectMapper.readValue(resp.getBody(), new TypeReference<List<ContainerInfoDTO>>() {});
        containers.forEach(c -> c.setNodeIp(ip));
        return containers;
    }

    // ===================== 重启 / 批量重启（完全不变） =====================
    public void restart(String containerName, String nodeIp) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        List<AgentIpPortDTO> agents = agentMapper.findAgentsByUserId(userId);
        AgentIpPortDTO agent = agents.stream().filter(a -> nodeIp.equals(a.getIp())).findFirst().orElse(null);
        if (agent == null) {
            throw new RuntimeException("无权限访问节点");
        }

        String agentUrl = "http://" + agent.getIp() + ":" + agent.getPort();
        restartSingleContainer(agentUrl, containerName, nodeIp);
        log.info("节点 {} 容器 {} 重启成功", nodeIp, containerName);
    }

    private void restartSingleContainer(String agentUrl, String containerName, String nodeIp) {
        String path = "/containers/" + containerName + "/restart";
        HttpHeaders headers = new HttpHeaders();
        signer.sign(HttpMethod.POST, path, "", headers);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        restTemplate.exchange(agentUrl + path, HttpMethod.POST, entity, Void.class);
    }

    public BatchRestartContainerResponse batchRestartContainers(BatchRestartContainerRequest request) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (request == null || request.getContainerItems() == null || request.getContainerItems().isEmpty()) {
            throw new RuntimeException("请求不能为空");
        }

        List<ContainerRestartItem> items = request.getContainerItems();
        BatchRestartContainerResponse resp = new BatchRestartContainerResponse();
        resp.setTotal(items.size());
        List<RestartResult> results = new ArrayList<>();

        List<AgentIpPortDTO> agents = agentMapper.findAgentsByUserId(userId);
        if (agents.isEmpty()) {
            items.forEach(i -> {
                RestartResult r = new RestartResult();
                r.setContainerName(i.getContainerName());
                r.setNodeIp(i.getNodeIp());
                r.setSuccess(false);
                r.setMessage("无可用节点");
                results.add(r);
            });
            resp.setResults(results);
            resp.setSuccess(0);
            resp.setFail(items.size());
            return resp;
        }

        Set<String> allowedIps = agents.stream().map(AgentIpPortDTO::getIp).collect(Collectors.toSet());
        Map<String, List<ContainerRestartItem>> group = items.stream().collect(Collectors.groupingBy(ContainerRestartItem::getNodeIp));

        for (Map.Entry<String, List<ContainerRestartItem>> entry : group.entrySet()) {
            String ip = entry.getKey();
            List<ContainerRestartItem> nodeItems = entry.getValue();

            if (!allowedIps.contains(ip)) {
                nodeItems.forEach(i -> {
                    RestartResult r = new RestartResult();
                    r.setContainerName(i.getContainerName());
                    r.setNodeIp(ip);
                    r.setSuccess(false);
                    r.setMessage("无权限");
                    results.add(r);
                });
                continue;
            }

            AgentIpPortDTO agent = agents.stream().filter(a -> ip.equals(a.getIp())).findFirst().orElse(null);
            if (agent == null) {
                nodeItems.forEach(i -> {
                    RestartResult r = new RestartResult();
                    r.setContainerName(i.getContainerName());
                    r.setNodeIp(ip);
                    r.setSuccess(false);
                    r.setMessage("节点不存在");
                    results.add(r);
                });
                continue;
            }

            String agentUrl = "http://" + agent.getIp() + ":" + agent.getPort();
            for (ContainerRestartItem item : nodeItems) {
                RestartResult r = new RestartResult();
                r.setContainerName(item.getContainerName());
                r.setNodeIp(ip);
                try {
                    restartSingleContainer(agentUrl, item.getContainerName(), ip);
                    r.setSuccess(true);
                    log.info("批量重启成功：{} {}", ip, item.getContainerName());
                } catch (Exception e) {
                    r.setSuccess(false);
                    r.setMessage(e.getMessage());
                    log.error("批量重启失败：{} {}", ip, item.getContainerName(), e);
                }
                results.add(r);
            }
        }

        long s = results.stream().filter(RestartResult::isSuccess).count();
        resp.setSuccess((int) s);
        resp.setFail(items.size() - (int) s);
        resp.setResults(results);
        return resp;
    }

}