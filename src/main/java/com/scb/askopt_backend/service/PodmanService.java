package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.askopt_backend.config.Hmac.GoAgentClient;
import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.dto.ContainerInfoDTO;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.askopt_backend.dto.RestartContainer.RestartResult;
import com.scb.askopt_backend.dto.podman.AgentBatchRestartReq;
import com.scb.askopt_backend.dto.podman.AgentBatchRestartResp;
import com.scb.askopt_backend.dto.podman.AgentSingleRestartResult;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.exception.GlobalExceptionHandler.ApiException;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.AgentMapper;
import com.scb.askopt_backend.context.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PodmanService {

    @Autowired
    private GoAgentClient goAgentClient;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Qualifier("containerExecutor")
    @Autowired
    private Executor containerExecutor;

    @Autowired
    private RedisUtil redisUtil;

    private static final int EXPIRE = 25;
    private static final int PRELOAD_DELAY = 25000;
    private static final int AGENT_ASYNC_TIMEOUT = 10;

    private static final String API_PREFIX = "/api/podman";
    private static final String PATH_CONTAINER_LIST = API_PREFIX + "/containers";
    private static final String PATH_BATCH_RESTART = API_PREFIX + "/containers/batch-restart";

    //==================== 查询容器列表（原有逻辑不变，异常细分）====================
    public List<ContainerInfoDTO> getContainers(List<String> nodeIps, boolean manual) {
        Long userId = AuthContext.getUserId();
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        if (userAgents.isEmpty()) {
            throw new ApiException(ResultCodeEnum.NO_AGENT);
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
                            } catch (ApiException e) {
                                throw e;
                            } catch (Exception e) {
                                log.error("节点 {} 拉取容器列表未知异常", agent.getIp(), e);
                                fail.incrementAndGet();
                            }
                        }, containerExecutor)
                        .orTimeout(AGENT_ASYNC_TIMEOUT, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("节点 {} 拉取容器列表超时（{}s）", agent.getIp(), AGENT_ASYNC_TIMEOUT);
                            fail.incrementAndGet();
                            return null;
                        }))
                .collect(Collectors.toList());

        for (var f : futures) {
            try {
                f.get();
            } catch (Exception ignored) {}
        }
        log.info("===== 批量拉取容器列表完成 =====");
        log.info("总节点：{} | 成功：{} | 失败：{}", agents.size(), success.get(), fail.get());
    }

    private String getNodeCacheKey(String ip) {
        return "container:node:" + ip;
    }

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

    private List<ContainerInfoDTO> fetchSingleAgent(AgentIpPortDTO agent) {
        String url = String.format("http://%s:%s%s", agent.getIp(), agent.getPort(), PATH_CONTAINER_LIST);
        try {
            String rawResp = goAgentClient.get(url, String.class);
            List<ContainerInfoDTO> containers = objectMapper.readValue(rawResp, new TypeReference<List<ContainerInfoDTO>>() {});
            containers.forEach(c -> c.setNodeIp(agent.getIp()));
            return containers;
        } catch (ResourceAccessException e) {
            log.error("节点 {} 连接失败", agent.getIp(), e);
            throw new ApiException(ResultCodeEnum.GO_AGENT_CONNECT_ERROR, "连接节点 " + agent.getIp() + " 失败");
        } catch (Exception e) {
            log.error("节点 {} 容器列表响应解析失败", agent.getIp(), e);
            throw new ApiException(ResultCodeEnum.GO_AGENT_RESPONSE_ERROR, "解析节点容器数据异常");
        }
    }

    //==================== 单个容器重启（参数校验 + 复用批量接口）====================
    public void restart(String containerName, String nodeIp) {
        if (containerName == null || containerName.isBlank() || nodeIp == null || nodeIp.isBlank()) {
            throw new ApiException(ResultCodeEnum.BAD_REQUEST, "容器名称/节点IP不能为空");
        }
        BatchRestartContainerRequest req = new BatchRestartContainerRequest();
        ContainerRestartItem item = new ContainerRestartItem();
        item.setContainerName(containerName.trim());
        item.setNodeIp(nodeIp.trim());
        req.setContainerItems(List.of(item));

        BatchRestartContainerResponse resp = batchRestartContainers(req);
        RestartResult result = resp.getResults().get(0);
        if (!result.isSuccess()) {
            throw new ApiException(ResultCodeEnum.SYSTEM_ERROR, "容器重启失败：" + result.getMessage());
        }
        log.info("节点 {} 容器 {} 重启成功", nodeIp, containerName);
    }

    //==================== 批量重启核心（去重、细分异常、并行聚合）====================
    public BatchRestartContainerResponse batchRestartContainers(BatchRestartContainerRequest request) {
        Long userId = AuthContext.getUserId();
        if (request == null || request.getContainerItems() == null || request.getContainerItems().isEmpty()) {
            throw new ApiException(ResultCodeEnum.BAD_REQUEST, "请求容器列表不能为空");
        }

        List<ContainerRestartItem> allItems = request.getContainerItems();
        BatchRestartContainerResponse finalResp = new BatchRestartContainerResponse();
        finalResp.setTotal(allItems.size());
        List<RestartResult> totalResultList = new ArrayList<>();

        // 1. 获取当前用户有权节点
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        if (userAgents.isEmpty()) {
            allItems.forEach(i -> {
                RestartResult r = new RestartResult();
                r.setContainerName(i.getContainerName());
                r.setNodeIp(i.getNodeIp());
                r.setSuccess(false);
                r.setMessage("无可用Agent节点");
                totalResultList.add(r);
            });
            finalResp.setResults(totalResultList);
            finalResp.setSuccess(0);
            finalResp.setFail(allItems.size());
            return finalResp;
        }

        Set<String> allowIpSet = userAgents.stream().map(AgentIpPortDTO::getIp).collect(Collectors.toSet());
        // 按节点IP分组
        Map<String, List<ContainerRestartItem>> groupByNode = allItems.stream()
                .collect(Collectors.groupingBy(ContainerRestartItem::getNodeIp));

        List<CompletableFuture<List<RestartResult>>> futures = new ArrayList<>();
        for (Map.Entry<String, List<ContainerRestartItem>> entry : groupByNode.entrySet()) {
            String nodeIp = entry.getKey();
            List<ContainerRestartItem> nodeItemList = entry.getValue();

            CompletableFuture<List<RestartResult>> future = CompletableFuture.supplyAsync(() -> {
                List<RestartResult> nodeResultList = new ArrayList<>();
                // 无权限节点
                if (!allowIpSet.contains(nodeIp)) {
                    nodeItemList.forEach(item -> {
                        RestartResult r = new RestartResult();
                        r.setContainerName(item.getContainerName());
                        r.setNodeIp(nodeIp);
                        r.setSuccess(false);
                        r.setMessage("无访问该节点权限");
                        nodeResultList.add(r);
                    });
                    return nodeResultList;
                }

                AgentIpPortDTO targetAgent = userAgents.stream()
                        .filter(a -> nodeIp.equals(a.getIp()))
                        .findFirst().orElse(null);
                if (targetAgent == null) {
                    nodeItemList.forEach(item -> {
                        RestartResult r = new RestartResult();
                        r.setContainerName(item.getContainerName());
                        r.setNodeIp(nodeIp);
                        r.setSuccess(false);
                        r.setMessage("节点不存在");
                        nodeResultList.add(r);
                    });
                    return nodeResultList;
                }

                // Java层提前去重，减少Go端重复执行压力
                List<String> distinctNames = nodeItemList.stream()
                        .map(ContainerRestartItem::getContainerName)
                        .distinct()
                        .collect(Collectors.toList());
                AgentBatchRestartReq agentReq = new AgentBatchRestartReq();
                agentReq.setContainerNames(distinctNames);

                String baseUrl = String.format("http://%s:%s", targetAgent.getIp(), targetAgent.getPort());
                String batchUrl = baseUrl + PATH_BATCH_RESTART;

                Map<String, AgentSingleRestartResult> resultMap = new HashMap<>();
                try {
                    AgentBatchRestartResp agentResp = goAgentClient.post(batchUrl, agentReq, AgentBatchRestartResp.class);
                    // 缓存容器执行结果
                    for (AgentSingleRestartResult res : agentResp.getResults()) {
                        resultMap.put(res.getContainerName(), res);
                    }
                    log.info("节点 {} 批量重启完成，成功{}个，失败{}个", nodeIp, agentResp.getSuccess(), agentResp.getFail());
                } catch (ResourceAccessException e) {
                    String msg;
                    Throwable root = e.getMostSpecificCause();
                    if (root instanceof java.net.SocketTimeoutException) {
                        log.error("节点 {} 请求超时", nodeIp, e);
                        msg = "请求节点超时";
                    } else {
                        log.error("节点 {} 连接失败", nodeIp, e);
                        msg = "连接节点失败";
                    }
                    nodeItemList.forEach(item -> {
                        RestartResult r = new RestartResult();
                        r.setContainerName(item.getContainerName());
                        r.setNodeIp(nodeIp);
                        r.setSuccess(false);
                        r.setMessage(msg);
                        nodeResultList.add(r);
                    });
                    return nodeResultList;
                } catch (ApiException e) {
                    log.error("节点 {} 批量请求业务异常 code:{}", nodeIp, e.getCode(), e);
                    nodeItemList.forEach(item -> {
                        RestartResult r = new RestartResult();
                        r.setContainerName(item.getContainerName());
                        r.setNodeIp(nodeIp);
                        r.setSuccess(false);
                        r.setMessage("节点操作异常[" + e.getCode() + "]：" + e.getMessage());
                        nodeResultList.add(r);
                    });
                    return nodeResultList;
                } catch (Exception e) {
                    log.error("节点 {} 批量重启未知异常", nodeIp, e);
                    nodeItemList.forEach(item -> {
                        RestartResult r = new RestartResult();
                        r.setContainerName(item.getContainerName());
                        r.setNodeIp(nodeIp);
                        r.setSuccess(false);
                        r.setMessage("未知异常：" + e.getMessage());
                        nodeResultList.add(r);
                    });
                    return nodeResultList;
                }

                // 还原原始传入顺序，填充结果
                for (ContainerRestartItem item : nodeItemList) {
                    AgentSingleRestartResult agentRes = resultMap.get(item.getContainerName());
                    RestartResult r = new RestartResult();
                    r.setContainerName(item.getContainerName());
                    r.setNodeIp(nodeIp);
                    if (agentRes != null) {
                        r.setSuccess(agentRes.isSuccess());
                        r.setMessage(agentRes.getMsg());
                    } else {
                        r.setSuccess(false);
                        r.setMessage("无执行结果");
                    }
                    nodeResultList.add(r);
                }
                return nodeResultList;
            }, containerExecutor).orTimeout(AGENT_ASYNC_TIMEOUT, TimeUnit.SECONDS);

            futures.add(future);
        }

        // 汇总所有节点结果
        for (CompletableFuture<List<RestartResult>> f : futures) {
            try {
                List<RestartResult> nodeResults = f.get(AGENT_ASYNC_TIMEOUT + 1, TimeUnit.SECONDS);
                totalResultList.addAll(nodeResults);
            } catch (TimeoutException e) {
                log.error("批量任务等待结果超时", e);
            } catch (InterruptedException e) {
                log.error("批量任务线程被中断", e);
            } catch (Exception e) {
                log.error("批量重启异步任务执行异常", e);
            }
        }

        long successCount = totalResultList.stream().filter(RestartResult::isSuccess).count();
        finalResp.setResults(totalResultList);
        finalResp.setSuccess((int) successCount);
        finalResp.setFail(allItems.size() - (int) successCount);
        return finalResp;
    }
}