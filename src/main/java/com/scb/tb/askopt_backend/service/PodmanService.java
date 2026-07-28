package com.scb.tb.askopt_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.scb.tb.askopt_backend.config.Hmac.GoAgentClient;
import com.scb.tb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.tb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.tb.askopt_backend.dto.common.PageResultDTO;
import com.scb.tb.askopt_backend.dto.podman.*;
import com.scb.tb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.tb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.tb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.tb.askopt_backend.dto.RestartContainer.RestartResult;
import com.scb.tb.askopt_backend.dto.podman.*;
import com.scb.tb.askopt_backend.entity.Agent;
import com.scb.tb.askopt_backend.entity.ContainerInfoPO;
import com.scb.tb.askopt_backend.exception.GlobalExceptionHandler.ApiException;
import com.scb.tb.askopt_backend.exception.ResultCodeEnum;
import com.scb.tb.askopt_backend.mapper.AgentMapper;
import com.scb.tb.askopt_backend.mapper.ContainerInfoMapper;
import com.scb.tb.askopt_backend.context.AuthContext;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private ContainerInfoMapper containerInfoMapper;

    @Autowired
    private WebClient webClient;

    @Autowired
    private HmacRequestSigner hmacRequestSigner;

    @Qualifier("containerExecutor")
    @Autowired
    private Executor containerExecutor;

    private static final int PRELOAD_DELAY = 25000;
    private static final int AGENT_ASYNC_TIMEOUT = 10;

    private static final String API_PREFIX = "/api/podman";
    private static final String PATH_CONTAINER_LIST = API_PREFIX + "/containers";
    private static final String PATH_BATCH_RESTART = API_PREFIX + "/containers/batch-restart";
    private static final String PATH_BATCH_STOP = API_PREFIX + "/containers/batch-stop";
    private static final String PATH_BATCH_START = API_PREFIX + "/containers/batch-start";
    private static final String PATH_CONTAINER_STOP = API_PREFIX + "/containers/%s/stop";
    private static final String PATH_CONTAINER_START = API_PREFIX + "/containers/%s/start";
    private static final String PATH_CONTAINER_LOGS = API_PREFIX + "/containers/%s/logs";

    // 提示文案常量，统一管理
    private static final String MSG_NO_AGENT = "无可用Agent节点";
    private static final String MSG_NO_PERMISSION = "无访问该节点权限";
    private static final String MSG_NODE_NOT_EXIST = "节点不存在";
    private static final String MSG_CONNECT_TIMEOUT = "请求节点超时";
    private static final String MSG_CONNECT_FAIL = "连接节点失败";
    private static final String MSG_NO_RESULT = "无执行结果";
    private static final String MSG_UNKNOWN_ERROR = "未知异常";

    //==================== 分页查询容器列表（DB持久化分页，支持动态排序） ====================
    public PageResultDTO<ContainerInfoDTO> getContainers(ContainerQueryDTO dto) {
        Long userId = AuthContext.getUserId();
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        if (userAgents.isEmpty()) {
            throw new ApiException(ResultCodeEnum.NO_AGENT);
        }

        // 从统一DTO取出参数
        List<String> nodeIps = dto.getNodeIps();
        boolean manual = dto.isManual();
        String sortField = dto.getSortField();
        String sortOrder = dto.getSortOrder();
        Long pageNum = dto.getPageNum();
        Long pageSize = dto.getPageSize();

        // 筛选目标节点
        List<AgentIpPortDTO> targetAgents;
        if (nodeIps != null && !nodeIps.isEmpty()) {
            targetAgents = userAgents.stream()
                    .filter(a -> nodeIps.contains(a.getIp()))
                    .collect(Collectors.toList());
        } else {
            targetAgents = userAgents;
        }

        // 手动刷新：重新拉取节点容器全量同步至PostgreSQL
        if (manual) {
            log.info("手动刷新容器数据，同步 {} 个节点至数据库", targetAgents.size());
            syncAgentContainerToDB(targetAgents);
        }

        List<String> allowIpList = targetAgents.stream()
                .map(AgentIpPortDTO::getIp)
                .collect(Collectors.toList());
        Page<ContainerInfoPO> pageParam = new Page<>(pageNum, pageSize);

        // 构建动态查询条件 + 动态排序
        LambdaQueryWrapper<ContainerInfoPO> wrapper = Wrappers.lambdaQuery();
        wrapper.in(ContainerInfoPO::getNodeIp, allowIpList);
        wrapper.eq(ContainerInfoPO::getIsDeleted, false);

        // 动态排序逻辑
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (StrUtil.isNotBlank(sortField)) {
            switch (sortField) {
                case "createdAt":
                    wrapper.orderBy(true, asc, ContainerInfoPO::getCreatedAt);
                    break;
                case "containerName":
                    wrapper.orderBy(true, asc, ContainerInfoPO::getContainerName);
                    break;
                case "image":
                    wrapper.orderBy(true, asc, ContainerInfoPO::getImage);
                    break;
                case "status":
                    wrapper.orderBy(true, asc, ContainerInfoPO::getStatus);
                    break;
                default:
                    wrapper.orderByDesc(ContainerInfoPO::getCreatedAt);
                    break;
            }
        } else {
            // 不传排序字段，默认创建时间倒序
            wrapper.orderByDesc(ContainerInfoPO::getCreatedAt);
        }

        // 替换原自定义Mapper分页
        IPage<ContainerInfoPO> poPage = containerInfoMapper.selectPage(pageParam, wrapper);

        // PO 转换为前端返回DTO
        List<ContainerInfoDTO> records = poPage.getRecords().stream()
                .map(this::poToDto)
                .collect(Collectors.toList());

        // 组装分页返回体
        PageResultDTO<ContainerInfoDTO> pageResult = new PageResultDTO<>();
        pageResult.setRecords(records);
        pageResult.setTotal(poPage.getTotal());
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setPages(poPage.getPages());
        return pageResult;
    }
    //==================== 同步Agent容器快照到数据库（定时/手动刷新调用） ====================
    private void syncAgentContainerToDB(List<AgentIpPortDTO> agents) {
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        List<CompletableFuture<Void>> futures = agents.stream()
                .map(agent -> CompletableFuture.runAsync(() -> {
                            try {
                                // 1. 请求Go Agent获取原始容器数据
                                List<RawContainerDTO> rawContainerList = fetchSingleAgentRawContainers(agent);
                                // 2. 软删除该节点旧容器数据
                                containerInfoMapper.markNodeContainersDeleted(agent.getIp());
                                // 3. 原始DTO转换为数据库PO实体
                                List<ContainerInfoPO> poList = rawContainerList.stream()
                                        .map(raw -> rawToPo(raw, agent.getIp()))
                                        .collect(Collectors.toList());
                                // 4. 批量插入/更新当前节点最新容器
                                if (!poList.isEmpty()) {
                                    containerInfoMapper.batchInsert(poList);
                                }
                                log.info("节点 {} 容器同步入库成功，容器数量：{}", agent.getIp(), rawContainerList.size());
                                success.incrementAndGet();
                            } catch (Exception e) {
                                log.error("节点 {} 同步容器数据失败", agent.getIp(), e);
                                fail.incrementAndGet();
                            }
                        }, containerExecutor)
                        .orTimeout(AGENT_ASYNC_TIMEOUT, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("节点 {} 拉取容器接口超时 {}s", agent.getIp(), AGENT_ASYNC_TIMEOUT);
                            fail.incrementAndGet();
                            return null;
                        }))
                .collect(Collectors.toList());

        // 阻塞等待全部异步同步任务完成
        for (CompletableFuture<Void> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
            }
        }
        log.info("===== 批量容器同步入库完成 =====");
        log.info("总节点：{} | 同步成功：{} | 同步失败：{}", agents.size(), success.get(), fail.get());
    }

    //==================== 定时全节点容器预热（每25秒自动同步） ====================
    @Scheduled(fixedDelay = PRELOAD_DELAY)
    public void preloadAllAgentContainers() {
        try {
            List<Agent> allAgentList = agentMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.emptyWrapper());
            if (allAgentList.isEmpty()) {
                log.info("系统无Agent节点，跳过定时容器预热");
                return;
            }
            List<AgentIpPortDTO> ipPortDtoList = allAgentList.stream()
                    .map(agent -> {
                        AgentIpPortDTO dto = new AgentIpPortDTO();
                        dto.setIp(agent.getIp());
                        dto.setPort(agent.getPort());
                        return dto;
                    }).collect(Collectors.toList());
            syncAgentContainerToDB(ipPortDtoList);
        } catch (Exception e) {
            log.error("定时预热全节点容器异常", e);
        }
    }

    //==================== 工具：请求单个Go Agent获取原始容器列表 ====================
    private List<RawContainerDTO> fetchSingleAgentRawContainers(AgentIpPortDTO agent) {
        String url = String.format("http://%s:%s%s", agent.getIp(), agent.getPort(), PATH_CONTAINER_LIST);
        try {
            // 修复：使用ParameterizedTypeReference获取List泛型
            return goAgentClient.get(url, new ParameterizedTypeReference<List<RawContainerDTO>>() {});
        } catch (ResourceAccessException e) {
            throw new ApiException(ResultCodeEnum.GO_AGENT_CONNECT_ERROR, "连接节点 " + agent.getIp() + " 失败");
        } catch (Exception e) {
            throw new ApiException(ResultCodeEnum.GO_AGENT_RESPONSE_ERROR, "解析节点容器返回数据异常");
        }
    }

    //==================== RawContainerDTO → 数据库PO实体转换 ====================
    private ContainerInfoPO rawToPo(RawContainerDTO raw, String nodeIp) {
        LocalDateTime now = LocalDateTime.now();
        ContainerInfoPO po = new ContainerInfoPO();
        po.setNodeIp(nodeIp);

        // 容器ID兜底
        String containerId = raw.getId();
        if (containerId == null || containerId.isBlank()) {
            containerId = "empty_id_" + System.currentTimeMillis();
        }
        po.setContainerId(containerId);

        // 容器名称兜底
        String containerName = raw.getName();
        if (containerName == null || containerName.isBlank()) {
            containerName = "unknown_container";
        }
        po.setContainerName(containerName);

        // image 非空兜底，解决数据库NOT NULL约束
        String image = raw.getImage();
        if (image == null || image.isBlank()) {
            image = "unknown_image";
        }
        po.setImage(image);

        // state、status兜底
        po.setState(raw.getState() == null ? "" : raw.getState());
        po.setStatus(raw.getStatus() == null ? "" : raw.getStatus());

        // 时间戳转换兜底
        if (raw.getCreated() != null) {
            po.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(raw.getCreated()), ZoneId.systemDefault()));
        } else {
            po.setCreatedAt(now);
        }

        po.setLastSeenTime(now);
        po.setIsDeleted(false);
        po.setPortsJson(raw.getPorts());

        Map<String, Object> extraMap = new HashMap<>();
        extraMap.put("Command", raw.getCommand());
        po.setExtraJson(extraMap);
        return po;
    }

    //==================== 数据库PO → 前端返回ContainerInfoDTO转换 ====================
    private ContainerInfoDTO poToDto(ContainerInfoPO po) {
        ContainerInfoDTO dto = new ContainerInfoDTO();
        dto.setNodeIp(po.getNodeIp());
        dto.setContainerId(po.getContainerId());
        dto.setShortId(StrUtil.sub(po.getContainerId(), 0, 12));
        dto.setContainerName(po.getContainerName());
        dto.setImage(po.getImage());
        dto.setState(po.getState());
        dto.setStatus(po.getStatus());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setLastSeenTime(po.getLastSeenTime());
        dto.setPorts(po.getPortsJson());

        Map<String, Object> extraJson = po.getExtraJson();
        if (extraJson != null) {
            dto.setCommand((String) extraJson.get("Command"));
        }

        // 已删除：imageId、networkSettings、mounts、labels 赋值代码
        return dto;
    }

    //==================== 单个容器重启（原有逻辑完整保留） ====================
    /**
     * 根据64位容器ID重启容器（调用Go单容器专用接口，不复用批量）
     * @param containerId 完整64位容器ID
     * @param nodeIp 目标节点IP
     */
    public void restart(String containerId, String nodeIp) {
        doSingleContainerAction(containerId, nodeIp, "restart", "/api/podman/containers/%s/restart");
    }

    public void stop(String containerId, String nodeIp) {
        doSingleContainerAction(containerId, nodeIp, "stop", PATH_CONTAINER_STOP);
    }

    public void start(String containerId, String nodeIp) {
        doSingleContainerAction(containerId, nodeIp, "start", PATH_CONTAINER_START);
    }


    //==================== 批量重启核心（原有逻辑完整保留） ====================
    public BatchRestartContainerResponse batchRestartContainers(BatchRestartContainerRequest request) {
        return doBatchContainerAction(request, PATH_BATCH_RESTART, "重启");
    }

    public BatchRestartContainerResponse batchStopContainers(BatchRestartContainerRequest request) {
        return doBatchContainerAction(request, PATH_BATCH_STOP, "停止");
    }

    public BatchRestartContainerResponse batchStartContainers(BatchRestartContainerRequest request) {
        return doBatchContainerAction(request, PATH_BATCH_START, "启动");
    }

    private void doSingleContainerAction(String containerId, String nodeIp, String actionName, String pathTemplate) {
        long startMs = System.currentTimeMillis();
        if (StrUtil.isBlank(containerId) || StrUtil.isBlank(nodeIp)) {
            throw new ApiException(ResultCodeEnum.BAD_REQUEST, "容器ID/节点IP不能为空");
        }
        String cleanContainerId = containerId.trim();
        String cleanNodeIp = nodeIp.trim();
        log.info("单容器{}请求，容器ID:{},节点:{}", actionName, cleanContainerId, cleanNodeIp);

        Long userId = AuthContext.getUserId();
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        AgentIpPortDTO targetAgent = userAgents.stream()
                .filter(a -> a.getIp().equals(cleanNodeIp))
                .findFirst()
                .orElseThrow(() -> new ApiException(ResultCodeEnum.NO_AGENT, MSG_NO_PERMISSION));

        String path = String.format(pathTemplate, cleanContainerId);
        String url = String.format("http://%s:%s%s", targetAgent.getIp(), targetAgent.getPort(), path);
        try {
            goAgentClient.post(url, null, String.class);
        } catch (ResourceAccessException e) {
            Throwable rootCause = e.getMostSpecificCause();
            String errMsg = (rootCause instanceof java.net.SocketTimeoutException) ? MSG_CONNECT_TIMEOUT : MSG_CONNECT_FAIL;
            throw new ApiException(ResultCodeEnum.GO_AGENT_CONNECT_ERROR, errMsg);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ResultCodeEnum.SYSTEM_ERROR, MSG_UNKNOWN_ERROR + "：" + e.getMessage());
        }

        log.info("节点 {} 容器ID {} {}成功，耗时{}ms", cleanNodeIp, cleanContainerId, actionName, System.currentTimeMillis() - startMs);
    }

    private BatchRestartContainerResponse doBatchContainerAction(BatchRestartContainerRequest request, String batchPath, String actionName) {
        Long userId = AuthContext.getUserId();
        if (request == null || request.getContainerItems() == null || request.getContainerItems().isEmpty()) {
            throw new ApiException(ResultCodeEnum.BAD_REQUEST, "请求容器列表不能为空");
        }

        List<ContainerRestartItem> allItems = request.getContainerItems();
        allItems.forEach(item -> {
            item.setContainerId(Optional.ofNullable(item.getContainerId()).map(String::trim).orElse(""));
            item.setNodeIp(Optional.ofNullable(item.getNodeIp()).map(String::trim).orElse(""));
        });
        log.info("批量{}入口，待处理容器总数:{}", actionName, allItems.size());

        BatchRestartContainerResponse finalResp = new BatchRestartContainerResponse();
        finalResp.setTotal(allItems.size());
        List<RestartResult> totalResultList = new ArrayList<>();

        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        if (userAgents.isEmpty()) {
            allItems.forEach(i -> totalResultList.add(buildFailResult(i.getContainerId(), i.getNodeIp(), MSG_NO_AGENT)));
            finalResp.setResults(totalResultList);
            finalResp.setSuccess(0);
            finalResp.setFail(allItems.size());
            return finalResp;
        }

        Set<String> allowIpSet = userAgents.stream().map(AgentIpPortDTO::getIp).collect(Collectors.toSet());
        Map<String, List<ContainerRestartItem>> groupByNode = allItems.stream()
                .collect(Collectors.groupingBy(ContainerRestartItem::getNodeIp));

        List<CompletableFuture<List<RestartResult>>> futures = new ArrayList<>();
        for (Map.Entry<String, List<ContainerRestartItem>> entry : groupByNode.entrySet()) {
            String nodeIp = entry.getKey();
            List<ContainerRestartItem> nodeItemList = entry.getValue();

            CompletableFuture<List<RestartResult>> future = CompletableFuture.supplyAsync(() -> {
                List<RestartResult> nodeResultList = new ArrayList<>();
                log.info("开始处理节点 {} 批量{}，容器数量:{}", nodeIp, actionName, nodeItemList.size());
                if (!allowIpSet.contains(nodeIp)) {
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerId(), item.getNodeIp(), MSG_NO_PERMISSION)));
                    return nodeResultList;
                }

                AgentIpPortDTO targetAgent = userAgents.stream()
                        .filter(a -> nodeIp.equals(a.getIp()))
                        .findFirst().orElse(null);
                if (targetAgent == null) {
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerId(), item.getNodeIp(), MSG_NODE_NOT_EXIST)));
                    return nodeResultList;
                }

                List<String> distinctIds = nodeItemList.stream()
                        .map(ContainerRestartItem::getContainerId)
                        .distinct()
                        .collect(Collectors.toList());
                AgentBatchRestartReq agentReq = new AgentBatchRestartReq();
                agentReq.setContainerIds(distinctIds);

                String batchUrl = String.format("http://%s:%s%s", targetAgent.getIp(), targetAgent.getPort(), batchPath);
                Map<String, AgentSingleRestartResult> resultMap = new HashMap<>();
                try {
                    AgentBatchRestartResp agentResp = goAgentClient.post(batchUrl, agentReq, AgentBatchRestartResp.class);
                    if (agentResp != null && agentResp.getResults() != null) {
                        for (AgentSingleRestartResult res : agentResp.getResults()) {
                            resultMap.put(res.getContainerName(), res);
                        }
                    }
                } catch (ResourceAccessException e) {
                    String msg;
                    Throwable root = e.getMostSpecificCause();
                    if (root instanceof java.net.SocketTimeoutException) {
                        msg = MSG_CONNECT_TIMEOUT;
                    } else {
                        msg = MSG_CONNECT_FAIL;
                    }
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerId(), item.getNodeIp(), msg)));
                    return nodeResultList;
                } catch (ApiException e) {
                    String errMsg = "节点操作异常[" + e.getCode() + "]：" + e.getMessage();
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerId(), item.getNodeIp(), errMsg)));
                    return nodeResultList;
                } catch (Exception e) {
                    nodeItemList.forEach(item -> nodeResultList.add(buildFailResult(item.getContainerId(), item.getNodeIp(), MSG_UNKNOWN_ERROR + "：" + e.getMessage())));
                    return nodeResultList;
                }

                for (ContainerRestartItem item : nodeItemList) {
                    AgentSingleRestartResult agentRes = resultMap.get(item.getContainerId());
                    RestartResult r = new RestartResult();
                    r.setContainerName(item.getContainerId());
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

        for (CompletableFuture<List<RestartResult>> f : futures) {
            try {
                List<RestartResult> nodeResults = f.get(AGENT_ASYNC_TIMEOUT + 1, TimeUnit.SECONDS);
                totalResultList.addAll(nodeResults);
            } catch (TimeoutException e) {
                log.error("批量{}任务等待结果超时", actionName, e);
            } catch (InterruptedException e) {
                log.error("批量{}任务线程被中断", actionName, e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("批量{}异步任务执行异常", actionName, e);
            }
        }

        long successCount = totalResultList.stream().filter(RestartResult::isSuccess).count();
        finalResp.setResults(totalResultList);
        finalResp.setSuccess((int) successCount);
        finalResp.setFail(allItems.size() - (int) successCount);
        log.info("批量{}全部节点处理完毕，总成功{}，总失败{}", actionName, successCount, finalResp.getFail());
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

    /**
     * 根据容器ID查询完整容器详情
     * @param nodeIp 节点IP
     * @param containerId 64位容器ID
     * @return 容器完整详情
     */
    public ContainerDetailDTO getContainerDetail(String nodeIp, String containerId) {
        Long userId = AuthContext.getUserId();
        // 校验用户是否有权访问该节点
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        AgentIpPortDTO targetAgent = userAgents.stream()
                .filter(a -> a.getIp().equals(nodeIp))
                .findFirst()
                .orElseThrow(() -> new ApiException(ResultCodeEnum.NO_AGENT, "无该节点访问权限"));
        // 请求Go Agent获取详情
        return fetchContainerDetail(targetAgent, containerId);
    }
    /**
     * 请求单个节点容器详情
     * @param agent agent节点信息
     * @param containerId 完整64位容器ID
     * @return 容器完整详情
     */
    public ContainerDetailDTO fetchContainerDetail(AgentIpPortDTO agent, String containerId) {
        String url = String.format("http://%s:%s/api/podman/containers/%s/detail",
                agent.getIp(), agent.getPort(), containerId);
        try {
            // 第一步：先拉取原始JSON字符串打印日志，用于调试
            String rawResponse = goAgentClient.get(url, String.class);
            log.info("【容器详情Go原始返回】url={}, response={}", url, rawResponse);

            // 第二步：正常反序列化为DTO返回
            return goAgentClient.get(url, new ParameterizedTypeReference<ContainerDetailDTO>() {});
        } catch (ResourceAccessException e) {
            throw new ApiException(ResultCodeEnum.GO_AGENT_CONNECT_ERROR, "连接节点 " + agent.getIp() + " 失败");
        } catch (Exception e) {
            log.error("拉取容器{}详情解析失败", containerId, e);
            throw new ApiException(ResultCodeEnum.GO_AGENT_RESPONSE_ERROR, "容器详情数据解析异常");
        }
    }



    public Flux<DataBuffer> streamContainerLogs(
            String nodeIp,
            String containerId,
            int tail,
            String since) {

        AgentIpPortDTO agent = resolveAuthorizedAgent(nodeIp);
        String url = buildLogsUrl(agent, containerId, tail, true, since);

        log.info("开始连接Agent SSE日志流 nodeIp={}, containerId={}, url={}",
                nodeIp, containerId, url);

        return webClient.get()
                .uri(url)
                .headers(headers ->
                        hmacRequestSigner.sign(HttpMethod.GET, url, "", headers))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> {
                            log.error("Agent返回异常状态 nodeIp={}, containerId={}, status={}",
                                    nodeIp,
                                    containerId,
                                    response.statusCode().value());

                            return response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException(
                                                    String.format(
                                                            "Agent日志接口异常, status=%d, msg=%s",
                                                            response.statusCode().value(),
                                                            body))));
                        })
                .bodyToFlux(DataBuffer.class)

                // 建立订阅
                .doOnSubscribe(subscription ->
                        log.info("开始订阅Agent日志流 nodeIp={}, containerId={}",
                                nodeIp, containerId))

                // 收到每个DataBuffer
                .doOnNext(buffer ->
                        log.debug("收到DataBuffer nodeIp={}, containerId={}, size={} bytes",
                                nodeIp,
                                containerId,
                                buffer.readableByteCount()))

                // 客户端取消（最重要）
                .doOnCancel(() ->
                        log.info("Agent日志流被取消(CANCEL) nodeIp={}, containerId={}",
                                nodeIp,
                                containerId))

                // 正常完成
                .doOnComplete(() ->
                        log.info("Agent日志流正常结束(COMPLETE) nodeIp={}, containerId={}",
                                nodeIp,
                                containerId))

                // Reactor发生异常
                .doOnError(e ->
                        log.error("Agent日志流发生异常 nodeIp={}, containerId={}",
                                nodeIp,
                                containerId,
                                e))

                // 无论哪种结束方式都会执行
                .doFinally(signalType ->
                        log.info("Agent日志流结束 signal={}, nodeIp={}, containerId={}",
                                signalType,
                                nodeIp,
                                containerId))

                .onErrorResume(e -> {
                    String errorSse =
                            "event:error\n" +
                                    "data:" + e.getMessage() + "\n\n";

                    return Flux.just(
                            DefaultDataBufferFactory.sharedInstance.wrap(
                                    errorSse.getBytes(StandardCharsets.UTF_8)));
                });
    }

    public String getContainerLogs(String nodeIp, String containerId, int safeTail, String since) {
        AgentIpPortDTO agent = resolveAuthorizedAgent(nodeIp);
        String url = buildLogsUrl(agent, containerId, safeTail, false, since);
        try {
            String resp = goAgentClient.get(url, String.class);
            return resp == null ? "" : resp;
        } catch (ResourceAccessException e) {
            Throwable root = e.getMostSpecificCause();
            String errMsg = (root instanceof java.net.SocketTimeoutException) ? MSG_CONNECT_TIMEOUT : MSG_CONNECT_FAIL;
            throw new ApiException(ResultCodeEnum.GO_AGENT_CONNECT_ERROR, errMsg);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ResultCodeEnum.SYSTEM_ERROR, MSG_UNKNOWN_ERROR + "：" + e.getMessage());
        }
    }
    /**
     * 权限校验 + 解析目标Agent
     */
    private  AgentIpPortDTO resolveAuthorizedAgent(String nodeIp) {
        Long userId = AuthContext.getUserId();
        List<AgentIpPortDTO> userAgents = agentMapper.findAgentsByUserId(userId);
        return userAgents.stream()
                .filter(a -> a.getIp().equals(nodeIp))
                .findFirst()
                .orElseThrow(() -> new ApiException(ResultCodeEnum.NO_AGENT));
    }
    /**
     * 组装新日志接口 URL:
     * /api/podman/containers/{containerId}/logs?tail=100&follow=false&since=10m
     */
    private String buildLogsUrl (AgentIpPortDTO agent, String containerId, int tail, boolean follow, String since) {
        String base = String.format("http://%s:%s" + PATH_CONTAINER_LOGS, agent.getIp(), agent.getPort(), containerId);
        StringBuilder sb = new StringBuilder(base)
                .append("?tail=").append(tail)
                .append("&follow=").append(follow);
        if (since != null && !since.isBlank()) {
            sb.append("&since=").append(since);
        }
        return sb.toString();

    }
}

