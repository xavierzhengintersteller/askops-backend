package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.ContainerInfo;
import com.scb.askopt_backend.service.PodmanService;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

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
    @PostMapping("/restart/{name}")
    public ApiResponse<String> restartContainer(@PathVariable String name) {
        try {
            podmanService.restart(name);
            // 返回标准消息
            return ApiResponse.success("Container " + name + " restarted");
        } catch (Exception e) {
            // 异常处理
            return ApiResponse.error(500, e.getMessage() );
        }
    }
    @GetMapping("containers")
    public ApiResponse<String> getAllContainer() {
        try {
            String containers = podmanService.getAllContainer();
            return ApiResponse.success(containers);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

}
