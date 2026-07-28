package com.scb.tb.askopt_backend.controller;

import cn.hutool.core.util.StrUtil;
import com.scb.tb.askopt_backend.annotation.AuditLog;
import com.scb.tb.askopt_backend.constant.AuditConstant;
import com.scb.tb.askopt_backend.context.AuditStatusContext;
import com.scb.tb.askopt_backend.context.AuthContext;
import com.scb.tb.askopt_backend.dto.*;
import com.scb.tb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.tb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.tb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.tb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.tb.askopt_backend.dto.common.PageResultDTO;
import com.scb.tb.askopt_backend.dto.podman.ContainerDetailDTO;
import com.scb.tb.askopt_backend.dto.podman.ContainerInfoDTO;
import com.scb.tb.askopt_backend.dto.podman.ContainerQueryDTO;
import com.scb.tb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.tb.askopt_backend.exception.ResultCodeEnum;
import com.scb.tb.askopt_backend.mapper.AgentMapper;
import com.scb.tb.askopt_backend.service.PodmanService;
import com.scb.tb.askopt_backend.vo.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/containers")
public class PodmanController {

    @Autowired
    private PodmanService podmanService;
    @Autowired
    private AgentMapper agentMapper;


    // 重启容器
    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/restart")
    public ApiResponse<String> restartContainer(@Valid @RequestBody ContainerRestartItem request) {
        log.info("【单容器重启接口】入参：{}", request);
        try {
            podmanService.restart(request.getContainerId(), request.getNodeIp());
            String successMsg = "Container " + request.getContainerId() + " restarted on node " + request.getNodeIp();
            log.info("【单容器重启接口】执行成功，返回：{}", successMsg);
            return ApiResponse.success(successMsg);
        } catch (Exception e) {
            AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            log.error("【单容器重启接口】执行异常：{}", e.getMessage(), e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // 批量重启
    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/restart-batch")
    public BatchRestartContainerResponse batchRestart(@RequestBody BatchRestartContainerRequest request) {
        log.info("【批量重启接口】入参容器总数：{}", request.getContainerItems().size());
        BatchRestartContainerResponse resp = podmanService.batchRestartContainers(request);

        // 批量操作状态判断
        if (resp.getFail() > 0) {
            if (resp.getSuccess() == 0) {
                AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
                log.warn("【批量重启接口】全部容器重启失败，成功{}，失败{}", resp.getSuccess(), resp.getFail());
            } else {
                AuditStatusContext.setStatus(AuditConstant.STATUS_PARTIAL_FAIL);
                log.warn("【批量重启接口】部分容器重启失败，成功{}，失败{}", resp.getSuccess(), resp.getFail());
            }
        } else {
            log.info("【批量重启接口】全部容器重启成功，总数{}", resp.getTotal());
        }
        return resp;
    }

    @GetMapping("/{containerId}/plainlogs")
    public ApiResponse<String> getContainerLogs(
            @PathVariable String containerId,
            @RequestParam String nodeIp,
            @RequestParam(required = false) Integer tail,
            @RequestParam(required = false) String since
    ) {
        try {
            if (StrUtil.isBlank(nodeIp)) {
                throw new GlobalExceptionHandler.ApiException(ResultCodeEnum.NO_NODEIP);
            }
            if (containerId == null || !containerId.matches("^[0-9a-f]{64}$")) {
                return ApiResponse.error(400, "Invalid containerId format");
            }
            int safeTail = (tail == null) ? 100 : tail;
            if (safeTail <= 0 || safeTail > 20000) {
                return ApiResponse.error(400, "Invalid tail parameter, must be between 1 and 20000");
            }
            if (since != null && !since.isBlank() && !since.matches("^\\d+[mh]$")) {
                return ApiResponse.error(400, "Invalid since parameter, only minute/hour format is allowed, e.g. 10m, 1h, 2h");
            }
            String logs = podmanService.getContainerLogs(nodeIp, containerId, safeTail, since);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            log.error("【容器日志接口】执行异常：{}", e.getMessage(), e);
            return ApiResponse.error(500, e.getMessage());
        }
    }



    @GetMapping(
            value = "/{containerId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamLogs(
            @PathVariable String containerId,
            @RequestParam String nodeIp,
            @RequestParam(required = false) Integer tail,
            @RequestParam(required = false) String since) {

        int tailNum = Optional.ofNullable(tail).orElse(100);
        tailNum = Math.max(0, tailNum);

        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());

        Flux<DataBuffer> flux =
                podmanService.streamContainerLogs(
                        nodeIp,
                        containerId,
                        tailNum,
                        since);

        Disposable disposable = flux.subscribe(

                dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);

                        // 原样发送SSE内容
                        emitter.send(bytes);

                    } catch (IOException e) {
                        log.info("SSE客户端已断开 containerId={}", containerId);

                        emitter.complete();

                    } finally {
                        // 一定要释放
                        DataBufferUtils.release(dataBuffer);
                    }
                },

                error -> {

                    log.error("日志流异常 containerId={}", containerId, error);

                    try {

                        emitter.send(
                                SseEmitter.event()
                                        .name("error")
                                        .data(error.getMessage())
                        );

                    } catch (IOException ignored) {
                    }

                    emitter.completeWithError(error);
                },

                emitter::complete
        );

        emitter.onCompletion(() -> {
            log.info("SSE完成，取消WebClient订阅 containerId={}", containerId);

            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        });

        emitter.onTimeout(() -> {

            log.info("SSE超时，取消WebClient订阅 containerId={}", containerId);

            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            emitter.complete();
        });

        emitter.onError(e -> {

            log.info("SSE异常，取消WebClient订阅 containerId={}", containerId);

            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        });

        return emitter;
    }

    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/stop")
    public ApiResponse<String> stopContainer(@Valid @RequestBody ContainerRestartItem request) {
        try {
            podmanService.stop(request.getContainerId(), request.getNodeIp());
            return ApiResponse.success("Container " + request.getContainerId() + " stopped on node " + request.getNodeIp());
        } catch (Exception e) {
            AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            log.error("【停止容器接口】执行异常：{}", e.getMessage(), e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/start")
    public ApiResponse<String> startContainer(@Valid @RequestBody ContainerRestartItem request) {
        try {
            podmanService.start(request.getContainerId(), request.getNodeIp());
            return ApiResponse.success("Container " + request.getContainerId() + " started on node " + request.getNodeIp());
        } catch (Exception e) {
            AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            log.error("【启动容器接口】执行异常：{}", e.getMessage(), e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/stop-batch")
    public ApiResponse<BatchRestartContainerResponse> batchStopContainer(
            @RequestBody BatchRestartContainerRequest request
    ) {
        try {
            BatchRestartContainerResponse resp = podmanService.batchStopContainers(request);
            if (resp.getFail() > 0) {
                AuditStatusContext.setStatus(
                        resp.getSuccess() == 0 ? AuditConstant.STATUS_FAIL : AuditConstant.STATUS_PARTIAL_FAIL
                );
            }
            return ApiResponse.success(resp);
        } catch (Exception e) {
            AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            log.error("【批量停止容器接口】执行异常：{}", e.getMessage(), e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/start-batch")
    public ApiResponse<BatchRestartContainerResponse> batchStartContainer(
            @RequestBody BatchRestartContainerRequest request
    ) {
        try {
            BatchRestartContainerResponse resp = podmanService.batchStartContainers(request);
            if (resp.getFail() > 0) {
                AuditStatusContext.setStatus(
                        resp.getSuccess() == 0 ? AuditConstant.STATUS_FAIL : AuditConstant.STATUS_PARTIAL_FAIL
                );
            }
            return ApiResponse.success(resp);
        } catch (Exception e) {
            AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            log.error("【批量启动容器接口】执行异常：{}", e.getMessage(), e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 分页查询容器列表
     */
    @GetMapping("/containers")
    public ApiResponse<PageResultDTO<ContainerInfoDTO>> getContainers(
            @ModelAttribute ContainerQueryDTO dto
    ) {
        log.info("查询容器列表｜query={}", dto);
        PageResultDTO<ContainerInfoDTO> pageData = podmanService.getContainers(dto);
        log.info("容器查询完成｜总条数:{}, 当前页返回:{}", pageData.getTotal(), pageData.getRecords().size());
        return ApiResponse.success(pageData);
    }

    @GetMapping("/nodes")
    public ApiResponse<List<AgentIpPortDTO>> getCurrentUserNodes() {
        Long userId = AuthContext.getUserId();
        List<AgentIpPortDTO> list = agentMapper.findAgentsByUserId(userId);

        // 👇 这里加判断：空列表 → 返回 100001 错误码
        if (list == null || list.isEmpty()) {
            return ApiResponse.error(
                    ResultCodeEnum.NO_AGENT.getCode(),
                    ResultCodeEnum.NO_AGENT.getMessage()
            );
        }

        return ApiResponse.success(list);
    }
    @GetMapping("/container/detail")
    public ApiResponse<ContainerDetailDTO> getContainerDetail(
            @RequestParam String nodeIp,
            @RequestParam String containerId
    ) {
        ContainerDetailDTO detail = podmanService.getContainerDetail(nodeIp, containerId);
        return ApiResponse.success(detail);
    }
}