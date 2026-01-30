package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.service.PodmanLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/containers")
public class PodmanLogController {
    @Autowired
    private  PodmanLogService logService;


    @GetMapping(
            value = "/{name}/logs/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> streamLogs(
            @PathVariable String name) {

        return logService.streamContainerLogs(name)
                .map(line ->
                        ServerSentEvent.builder(line)
                                .event("log")
                                .build()
                );
    }
}
