package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.annotation.AuditLog;
import com.scb.askopt_backend.constant.AuditConstant;
import com.scb.askopt_backend.context.AuditStatusContext;
import com.scb.askopt_backend.dto.*;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.askopt_backend.service.PodmanService;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
public class PodmanController {

    @Autowired
    private PodmanService podmanService;

    // 重启容器
    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/restart")
    public ApiResponse<String> restartContainer(@RequestBody ContainerRestartItem request) {
        try {
            podmanService.restart(request.getContainerName(), request.getNodeIp());
            return ApiResponse.success(
                    "Container " + request.getContainerName() + " restarted on node " + request.getNodeIp()
            );
        } catch (Exception e) {
            // ✅ 标记审计状态为失败
            AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // 批量重启
    @AuditLog(module = "CONTAINER", operation = AuditConstant.UPDATE)
    @PostMapping("/restart-batch")
    public BatchRestartContainerResponse batchRestart(@RequestBody BatchRestartContainerRequest request) {
        BatchRestartContainerResponse resp = podmanService.batchRestartContainers(request);

        // ====================== 批量操作状态判断
        if (resp.getFail() > 0) {
            if (resp.getSuccess() == 0) {
                AuditStatusContext.setStatus(AuditConstant.STATUS_FAIL);
            } else {
                AuditStatusContext.setStatus(AuditConstant.STATUS_PARTIAL_FAIL);
            }
        }

        return resp;
    }

    // 获取容器列表
    @GetMapping("/containers")
    public ApiResponse<List<ContainerInfoDTO>> getContainers(
            @RequestParam(required = false) List<String> nodeIps,
            @RequestParam(defaultValue = "false") boolean manual
    ) {
        List<ContainerInfoDTO> containers = podmanService.getContainers(nodeIps, manual);
        return ApiResponse.success(containers);
    }
}