package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.annotation.AuditLog;
import com.scb.askopt_backend.constant.AuditConstant;
import com.scb.askopt_backend.context.AuditStatusContext;
import com.scb.askopt_backend.context.AuthContext;
import com.scb.askopt_backend.dto.*;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
import com.scb.askopt_backend.dto.RestartContainer.ContainerRestartItem;
import com.scb.askopt_backend.dto.common.PageQueryDTO;
import com.scb.askopt_backend.dto.common.PageResultDTO;
import com.scb.askopt_backend.dto.podman.ContainerDetailDTO;
import com.scb.askopt_backend.dto.podman.ContainerInfoDTO;
import com.scb.askopt_backend.dto.podman.ContainerQueryDTO;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.AgentMapper;
import com.scb.askopt_backend.service.PodmanService;
import com.scb.askopt_backend.vo.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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