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

    // 提示文案常量，统一管理
    private static final String MSG_NO_AGENT = "无可用Agent节点";
    private static final String MSG_NO_PERMISSION = "无访问该节点权限";
    private static final String MSG_NODE_NOT_EXIST = "节点不存在";
    private static final String MSG_CONNECT_TIMEOUT = "请求节点超时";
    private static final String MSG_CONNECT_FAIL = "连接节点失败";
    private static final String MSG_NO_RESULT = "无执行结果";
    private static final String MSG_UNKNOWN_ERROR = "未知异常";

    //==================== 查询容器列表 ====================
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

    //==================== 单个容器重启 ====================
    public void restart(String containerName, String nodeIp) {
        long start = System.currentTimeMillis();
        // 基础非空校验
        if (containerName == null || containerName.isBlank() || nodeIp == null || nodeIp.isBlank()) {
            throw new ApiException(ResultCodeEnum.BAD_REQUEST, "容器名称/节点IP不能为空");
        }
        // 清洗参数
        String cleanContainer = containerName.trim();
        String cleanNodeIp = nodeIp.trim();
        log.info("单容器重启请求，容器:{},节点:{}", cleanContainer, cleanNodeIp);

        BatchRestartContainerRequest req = new BatchRestartContainerRequest();
        ContainerRestartItem item = new ContainerRestartItem();
        item.setContainerName(cleanContainer);
        item.setNodeIp(cleanNodeIp);
        req.setContainerItems(List.of(item));

        BatchRestartContainerResponse resp = batchRestartContainers(req);
        List<RestartResult> resultList = resp.getResults();
        // 防止数组越界
        if (resultList == null || resultList.isEmpty()) {
            log.error("单容器重启无返回结果，容器:{},节点:{}", cleanContainer, cleanNodeIp);
            throw new ApiException(ResultCodeEnum.SYSTEM_ERROR, "节点请求未返回执行结果，请稍后重试");
        }

        RestartResult result = resultList.get(0);
        if (!result.isSuccess()) {
            String errMsg = result.getMessage();
            log.error("单容器重启失败，容器:{},节点:{},原因:{}", cleanContainer, cleanNodeIp, errMsg);
            // 精准匹配枚举，不依赖模糊contains
            if (MSG_NO_PERMISSION.equals(errMsg)) {
                throw new ApiException(ResultCodeEnum.NO_PERMISSION_NODE, errMsg);
            } else if (MSG_NODE_NOT_EXIST.equals(errMsg)) {
                throw new ApiException(ResultCodeEnum.NO_SUCH_NODE, errMsg);
            } else if (MSG_CONNECT_TIMEOUT.equals(errMsg)) {
                throw new ApiException(ResultCodeEnum.GO_AGENT_TIMEOUT, errMsg);
            } else if (MSG_CONNECT_FAIL.equals(errMsg)) {
                throw new ApiException(ResultCodeEnum.GO_AGENT_CONNECT_ERROR, errMsg);
            } else {
                throw new ApiException(ResultCodeEnum.SYSTEM_ERROR, "容器重启失败：" + errMsg);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("节点 {} 容器 {} 重启成功，耗时{}ms", cleanNodeIp, cleanContainer, cost);
    }

    //==================== 批量重启核心 ====================
    public BatchRestartContainerResponse batchRestartContainers(BatchRestartContainerRequest request) {
        Long userId = AuthContext.getUserId();
        if (request == null || request.getContainerItems() == null || request.getContainerItems().isEmpty()) {
            throw new ApiException(ResultCodeEnum.BAD_REQUEST, "请求容器列表不能为空");
        }

        List<ContainerRestartItem> allItems = request.getContainerItems();
        // 统一清洗所有入参，消除单/批量trim不一致
        allItems.forEach(item -> {
            item.setContainerName(Optional.ofNullable(item.getContainerName()).map(String::trim).orElse(""));
            item.setNodeIp(Optional.ofNullable(item.getNodeIp()).map(String::trim).orElse(""));
        });
        log.info("批量重启入口，待处理容器总数:{}", allItems.size());

        BatchRestartContainerResponse finalResp = new BatchRestartContainerResponse();
        finalResp.setTotal(allItems.size());
        List<RestartResult> totalResultList = new ArrayList<>();

        // 1. 获取当前用户有权节点
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        if (userAgents.isEmpty()) {
            allItems.forEach(i -> totalResultList.add(buildFailResult(i.getContainerName(), i.getNodeIp(), MSG_NO_AGENT)));
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
                log.info("开始处理节点 {} 批量重启，容器数量:{}", nodeIp, nodeItemList.size());
                // 无权限节点
                if (!allowIpSet.contains(nodeIp)) {
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerName(), item.getNodeIp(), MSG_NO_PERMISSION)));
                    return nodeResultList;
                }

                AgentIpPortDTO targetAgent = userAgents.stream()
                        .filter(a -> nodeIp.equals(a.getIp()))
                        .findFirst().orElse(null);
                if (targetAgent == null) {
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerName(), item.getNodeIp(), MSG_NODE_NOT_EXIST)));
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
                        msg = MSG_CONNECT_TIMEOUT;
                    } else {
                        log.error("节点 {} 连接失败", nodeIp, e);
                        msg = MSG_CONNECT_FAIL;
                    }
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerName(), item.getNodeIp(), msg)));
                    return nodeResultList;
                } catch (ApiException e) {
                    log.error("节点 {} 批量请求业务异常 code:{}", nodeIp, e.getCode(), e);
                    String errMsg = "节点操作异常[" + e.getCode() + "]：" + e.getMessage();
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerName(), item.getNodeIp(), errMsg)));
                    return nodeResultList;
                } catch (Exception e) {
                    log.error("节点 {} 批量重启未知异常", nodeIp, e);
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerName(), item.getNodeIp(), MSG_UNKNOWN_ERROR + "：" + e.getMessage())));
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
                        r.setMessage(MSG_NO_RESULT);
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
        log.info("批量重启全部节点处理完毕，总成功{}，总失败{}", successCount, finalResp.getFail());
        return finalResp;
    }

    /**
     * 公共工具：快速构造失败返回结果，消除重复new样板代码
     */
    private RestartResult buildFailResult(String containerName, String nodeIp, String msg) {
        RestartResult r = new RestartResult();
        r.setContainerName(containerName);
        r.setNodeIp(nodeIp);
        r.setSuccess(false);
        r.setMessage(msg);
        return r;
    }
}