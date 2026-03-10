package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.*;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerRequest;
import com.scb.askopt_backend.dto.RestartContainer.BatchRestartContainerResponse;
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

    @GetMapping(
            value = "/{name}/logs/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> streamLogs(
            @PathVariable String name) {

        return podmanService.streamContainerLogs(name)
                .map(line ->
                        ServerSentEvent.builder(line)
                                .event("log")
                                .build()
                );
    }
    @GetMapping("/{name}/logs/raws")
    public ApiResponse<List<String>> rawLogs(
            @PathVariable String name,


            @RequestParam(required = false, defaultValue = "100") int lines
    ) {
        try {
            // 调用 Service 层获取日志列表
            List<String> logs = podmanService.rawLogs(name, lines);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(500,"获取日志失败: " + e.getMessage());
        }
    }
    @PostMapping("/restart")
    public ApiResponse<String> restartContainer(@RequestBody RestartContainerRequest request) {
        try {
            podmanService.restart(request.getContainerId(), request.getNodeIp());
            return ApiResponse.success(
                    "Container " + request.getContainerId() + " restarted on node " + request.getNodeIp()
            );
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }
    // 批量重启接口
    @PostMapping("/batch-restart")
    public BatchRestartContainerResponse batchRestart(@RequestBody BatchRestartContainerRequest request) {
        return podmanService.batchRestartContainers(request);
    }
    @GetMapping("containers")
    public ApiResponse<List<ContainerInfoDTO>> getAllContainer() {
        try {
            List<ContainerInfoDTO> containers = podmanService.getAllContainer();
            return ApiResponse.success(containers);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

}
