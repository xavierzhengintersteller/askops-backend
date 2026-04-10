package com.scb.askopt_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.askopt_backend.dto.ContainerInfoDTO;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.askopt_backend.dto.RestartContainer.RestartResult;
import com.scb.askopt_backend.mapper.AgentMapper;
import com.scb.askopt_backend.security.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;
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
    private WebClient webClient;
    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * 批量重启容器（支持跨节点）
     * @param request 批量重启请求（容器名+节点IP列表）
     * @return 批量重启结果（包含每个容器的成功/失败状态）
     */
    public BatchRestartContainerResponse batchRestartContainers(BatchRestartContainerRequest request) {
        // 1. 基础校验
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，无法批量重启容器");
        }
        if (request == null || request.getContainerItems() == null || request.getContainerItems().isEmpty()) {
            throw new RuntimeException("批量重启请求不能为空");
        }
        List<ContainerRestartItem> items = request.getContainerItems();
        BatchRestartContainerResponse response = new BatchRestartContainerResponse();
        response.setTotal(items.size());
        List<RestartResult> results = new ArrayList<>();

        // 2. 查询用户有权限的所有agent，校验节点权限
        List<AgentIpPortDTO> accessibleAgents = agentMapper.findAgentsByUserId(userId);
        if (accessibleAgents.isEmpty()) {
            // 所有请求都失败
            items.forEach(item -> {
                RestartResult result = new RestartResult();
                result.setContainerName(item.getContainerName());
                result.setNodeIp(item.getNodeIp());
                result.setSuccess(false);
                result.setMessage("用户没有可用的agent");
                results.add(result);
            });
            response.setResults(results);
            response.setSuccess(0);
            response.setFail(items.size());
            return response;
        }
        // 提取用户有权限的节点IP集合
        Set<String> accessibleNodeIps = accessibleAgents.stream()
                .map(AgentIpPortDTO::getIp)
                .collect(Collectors.toSet());

        // 3. 按节点IP分组，批量处理同一节点的容器
        Map<String, List<ContainerRestartItem>> nodeGroup = items.stream()
                .collect(Collectors.groupingBy(ContainerRestartItem::getNodeIp));

        // 4. 遍历每个节点，重启该节点下的所有容器
        for (Map.Entry<String, List<ContainerRestartItem>> entry : nodeGroup.entrySet()) {
            String nodeIp = entry.getKey();
            List<ContainerRestartItem> nodeItems = entry.getValue();

            // 4.1 校验当前节点权限
            if (!accessibleNodeIps.contains(nodeIp)) {
                // 该节点无权限，所有容器重启失败
                nodeItems.forEach(item -> {
                    RestartResult result = new RestartResult();
                    result.setContainerName(item.getContainerName());
                    result.setNodeIp(nodeIp);
                    result.setSuccess(false);
                    result.setMessage("用户无权限访问该节点(" + nodeIp + ")");
                    results.add(result);
                });
                continue;
            }

            // 4.2 获取当前节点的agent信息
            AgentIpPortDTO agent = accessibleAgents.stream()
                    .filter(a -> nodeIp.equals(a.getIp()))
                    .findFirst()
                    .orElse(null);
            if (agent == null) {
                nodeItems.forEach(item -> {
                    RestartResult result = new RestartResult();
                    result.setContainerName(item.getContainerName());
                    result.setNodeIp(nodeIp);
                    result.setSuccess(false);
                    result.setMessage("节点(" + nodeIp + ")配置异常，未找到对应的agent信息");
                    results.add(result);
                });
                continue;
            }

            // 4.3 遍历该节点下的所有容器，逐个重启（也可优化为调用agent批量接口）
            String agentUrl = "http://" + agent.getIp() + ":" + agent.getPort();
            for (ContainerRestartItem item : nodeItems) {
                String containerName = item.getContainerName();
                RestartResult result = new RestartResult();
                result.setContainerName(containerName);
                result.setNodeIp(nodeIp);

                try {
                    // 调用单个重启逻辑（复用已有restart的核心逻辑）
                    restartSingleContainer(agentUrl, containerName, nodeIp);
                    result.setSuccess(true);
                    result.setMessage(null);
                    log.info("批量重启：节点 {} 容器 {} 成功", nodeIp, containerName);
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setMessage("重启失败：" + e.getMessage());
                    log.error("批量重启：节点 {} 容器 {} 失败", nodeIp, containerName, e);
                }
                results.add(result);
            }
        }

        // 5. 统计成功/失败数
        long successCount = results.stream().filter(RestartResult::isSuccess).count();
        response.setSuccess((int) successCount);
        response.setFail(items.size() - (int) successCount);
        response.setResults(results);

        return response;
    }
    /**
     * 优化后：重启指定 nodeIp 上的指定容器（带权限校验）
     * @param containerName 要重启的容器名
     * @param nodeIp 容器所属的 agent IP（前端传递）
     */
    public void restart(String containerName, String nodeIp) {
        // 1. 获取当前登录用户ID，校验登录状态
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，无法重启容器");
        }

        // 2. 查询用户有权限的所有 agent，校验 nodeIp 权限
        List<AgentIpPortDTO> accessibleAgents = agentMapper.findAgentsByUserId(userId);
        if (accessibleAgents.isEmpty()) {
            throw new RuntimeException("用户没有可用的 agent");
        }

        // 3. 根据 nodeIp 匹配对应的 agent（确保用户有权限访问该 agent）
        Optional<AgentIpPortDTO> targetAgent = accessibleAgents.stream()
                .filter(agent -> nodeIp.equals(agent.getIp()))
                .findFirst();
        if (targetAgent.isEmpty()) {
            throw new RuntimeException("用户无权限访问该节点(" + nodeIp + ")，无法重启容器");
        }

        // 4. 拼接 agent 重启接口 URL
        AgentIpPortDTO agent = targetAgent.get();
        String agentUrl = "http://" + agent.getIp() + ":" + agent.getPort();
        String urlPath = "/containers/" + containerName + "/restart";
        log.info("准备重启节点 {} 上的容器 {}，接口地址：{}", nodeIp, containerName, agentUrl + urlPath);

        try {
            restartSingleContainer( agentUrl,  containerName,  nodeIp);
            log.info("节点 {} 上的容器 {} 重启成功", nodeIp, containerName);
        } catch (Exception e) {
            log.error("节点 {} 上的容器 {} 重启失败", nodeIp, containerName, e);
            throw new RuntimeException("重启容器失败：" + e.getMessage(), e);
        }
    }
    /**
     * 复用的单个容器重启逻辑（抽离为私有方法，便于批量/单个接口复用）
     */
    private void restartSingleContainer(String agentUrl, String containerName, String nodeIp) {
        String urlPath = "/containers/" + containerName + "/restart";
        HttpHeaders headers = new HttpHeaders();
        signer.sign(HttpMethod.POST, urlPath, "", headers);

        HttpEntity<String> entity = new HttpEntity<>("", headers);
        restTemplate.exchange(
                agentUrl + urlPath,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    /**
     * 保留并优化 getAllContainer 接口（增加日志和容错）
     */
    public List<ContainerInfoDTO> getAllContainer() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，无法查询容器");
        }

        List<AgentIpPortDTO> agents = agentMapper.findAgentsByUserId(userId);
        if (agents.isEmpty()) {
            throw new RuntimeException("没有可用 agent");
        }

        List<ContainerInfoDTO> result = new ArrayList<>();

        for (AgentIpPortDTO agent : agents) {
            String agentIp = agent.getIp();
            String agentUrl = "http://" + agentIp + ":" + agent.getPort();
            String path = "/containers";
            log.info("查询节点 {} 的容器列表，接口地址：{}", agentIp, agentUrl + path);

            HttpHeaders headers = new HttpHeaders();
            signer.sign(HttpMethod.GET, path, "", headers);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        agentUrl + path,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                List<ContainerInfoDTO> containers = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ContainerInfoDTO>>() {}
                );

                // 注入 nodeIp，为前端重启接口提供参数
                for (ContainerInfoDTO c : containers) {
                    c.setNodeIp(agentIp);
                }

                result.addAll(containers);
                log.info("节点 {} 容器列表查询成功，共 {} 个容器", agentIp, containers.size());

            } catch (Exception e) {
                log.error("节点 {} 容器列表查询失败", agentIp, e);
                // 单个 agent 失败不影响整体，抛出警告而非终止
                throw new RuntimeException("节点 " + agentIp + " 容器查询失败：" + e.getMessage(), e);
                // 若想忽略失败，可改为：
                // log.warn("节点 {} 容器查询失败，跳过该节点", agentIp, e);
            }
        }

        return result;
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
    public List<ContainerInfoDTO> getContainers(List<String> nodeIps) throws Exception {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，无法查询容器");
        }

        // 1. 获取用户有权限的所有节点
        List<AgentIpPortDTO> agents = agentMapper.findAgentsByUserId(userId);
        if (agents.isEmpty()) {
            throw new RuntimeException("没有可用 agent");
        }

        List<ContainerInfoDTO> result = new ArrayList<>();

        // 2. 前端传了节点列表 → 只查这些节点
        if (nodeIps != null && !nodeIps.isEmpty()) {
            for (String ip : nodeIps) {
                Optional<AgentIpPortDTO> agent = agents.stream()
                        .filter(a -> ip.equals(a.getIp()))
                        .findFirst();
                if (agent.isPresent()) {
                    fetchContainersFromAgent(agent.get(), result);
                }
            }
            return result;
        }

        // 3. 没传 → 查全部
        for (AgentIpPortDTO agent : agents) {
            try {
                fetchContainersFromAgent(agent, result);
            } catch (Exception e) {
                log.error("节点 {} 查询失败", agent.getIp(), e);
            }
        }

        return result;
    }

    /**
     * 抽取公共方法：从单个 agent 获取容器
     */
    private void fetchContainersFromAgent(AgentIpPortDTO agent, List<ContainerInfoDTO> result) throws Exception {
        String agentIp = agent.getIp();
        String agentUrl = "http://" + agentIp + ":" + agent.getPort();
        String path = "/containers";

        HttpHeaders headers = new HttpHeaders();
        signer.sign(HttpMethod.GET, path, "", headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                agentUrl + path,
                HttpMethod.GET,
                entity,
                String.class
        );

        List<ContainerInfoDTO> containers = objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<ContainerInfoDTO>>() {}
        );

        // 注入 nodeIp
        for (ContainerInfoDTO c : containers) {
            c.setNodeIp(agentIp);
        }

        result.addAll(containers);
        log.info("节点 {} 容器查询成功，数量：{}", agentIp, containers.size());
    }
}