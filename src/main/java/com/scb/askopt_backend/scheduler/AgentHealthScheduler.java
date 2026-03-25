package com.scb.askopt_backend.scheduler;

import com.scb.askopt_backend.config.Hmac.HmacRequestSigner;
import com.scb.askopt_backend.constant.AgentStatus;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.mapper.AgentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class AgentHealthScheduler {
    @Autowired
    private AgentMapper agentMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private HmacRequestSigner signer;

    private static final int MAX_FAIL_COUNT = 3;
    private static final int THREAD_POOL_SIZE = 10;
    // 优化线程池配置，增加拒绝策略和命名
    private final ExecutorService executor = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("agent-health-probe-" + (++count));
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝时由调用线程执行，避免任务丢失
    );

    /**
     * 固定延迟6秒探测一次（原注释写的每分钟，代码是6秒，保持代码逻辑）
     */
    @Scheduled(fixedDelay = 600000)
    public void probeAndUpdateAgentStatus() {
        log.info("[AgentHealthScheduler] 开始探测所有 agent 健康状态");
        List<Agent> agents = agentMapper.findAllAgents();
        if (agents.isEmpty()) {
            log.info("[AgentHealthScheduler] 无可用Agent，本轮探测结束");
            return;
        }

        List<Future<?>> futures = new ArrayList<>();
        for (Agent agent : agents) {
            futures.add(executor.submit(() -> probeAgent(agent)));
        }

        // 等待所有任务完成，捕获异常但不中断
        for (Future<?> f : futures) {
            try {
                f.get(5, TimeUnit.SECONDS); // 单个任务超时控制，避免阻塞
            } catch (TimeoutException e) {
                log.error("[AgentHealthScheduler] 探测任务超时，强制终止", e);
                f.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                log.error("[AgentHealthScheduler] 探测任务执行异常", e);
            }
        }

        log.info("[AgentHealthScheduler] 本轮探测完成，总计探测 {} 个 agent", agents.size());
    }

    private void probeAgent(Agent agent) {
        Long agentId = agent.getId();
        String agentName = agent.getName();
        String agentIp = agent.getIp();
        int agentPort = agent.getPort();
        String urlPath = "/health";
        String fullUrl = "http://" + agentIp + ":" + agentPort + urlPath;
        boolean probeSuccess = false;

        try {
            // 构建带签名的请求头
            HttpHeaders headers = new HttpHeaders();
            signer.sign(HttpMethod.GET, urlPath, "", headers);
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            // 调用健康检查接口
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // 仅2xx响应视为探测成功
            if (response.getStatusCode().is2xxSuccessful()) {
                probeSuccess = true;
                log.info("[AgentHealthScheduler] 探测 agent [{}]({}:{}) 成功，状态 ONLINE",
                        agentName, agentIp, agentPort);
            } else {
                log.warn("[AgentHealthScheduler] 探测 agent [{}]({}:{}) 返回非2xx状态码: {}，标记为失败",
                        agentName, agentIp, agentPort, response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("[AgentHealthScheduler] 探测 agent [{}]({}:{}) 异常: {}",
                    agentName, agentIp, agentPort, e.getMessage());
        }

        // 处理探测结果
        if (probeSuccess) {
            // 成功：重置失败次数，更新状态为ONLINE，更新心跳时间
            agentMapper.updateAgentStatusAndFailCount(agentId,
                    AgentStatus.ONLINE.getCode(),
                    0,
                    LocalDateTime.now());
            log.debug("[AgentHealthScheduler] agent [{}]({}:{}) 重置失败次数为0，状态更新为ONLINE",
                    agentName, agentIp, agentPort);
        } else {
            // 失败：原子递增失败次数
            agentMapper.incrementFailCount(agentId);

            // 关键修复：查询单条Agent的最新失败次数（而非全量）
            Integer currentFailCount = agentMapper.getFailCountById(agentId);
            int failCount = currentFailCount == null ? 1 : currentFailCount;

            // 更新心跳时间（失败也更新）
            agentMapper.updateHeartbeatTime(agentId, LocalDateTime.now());

            // 判断是否达到最大失败次数
            if (failCount >= MAX_FAIL_COUNT) {
                agentMapper.updateAgentStatusAndFailCount(agentId,
                        AgentStatus.OFFLINE.getCode(),
                        failCount,
                        LocalDateTime.now());
                log.warn("[AgentHealthScheduler] agent [{}]({}:{}) 连续失败 {} 次，标记为 OFFLINE",
                        agentName, agentIp, agentPort, failCount);
            } else {
                log.info("[AgentHealthScheduler] agent [{}]({}:{}) 探测失败，当前连续失败 {} 次（未达阈值{}）",
                        agentName, agentIp, agentPort, failCount, MAX_FAIL_COUNT);
                // 可选：失败但未达阈值时，保持原有状态（或更新为DEGRADE）
                agentMapper.updateAgentStatusAndFailCount(agentId,
                        agent.getStatus(), // 保留原有状态，仅更新失败次数和心跳
                        failCount,
                        LocalDateTime.now());
            }
        }
    }

    /**
     * 优雅关闭线程池，避免应用关闭时资源泄漏
     */
    @PreDestroy
    public void destroy() {
        log.info("[AgentHealthScheduler] 开始关闭探测线程池");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        log.info("[AgentHealthScheduler] 探测线程池已关闭");
    }
}