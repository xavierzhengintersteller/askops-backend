package com.scb.askopt_backend.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class PodmanLogService {

    private final WebClient webClient;

    // 通过构造函数注入 WebClient（Spring 会自动注入 podmanWebClient Bean）
    public PodmanLogService(WebClient podmanWebClient) {
        this.webClient = podmanWebClient;
    }

    public Flux<String> streamContainerLogs(String containerName) {
        return webClient.get()
                .uri("/containers/{name}/logs/sse", containerName)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(s -> System.out.println("Start streaming logs: " + containerName))
                .doOnCancel(() -> System.out.println("Client disconnected: " + containerName))
                .doOnError(err -> System.err.println("Log stream error: " + err.getMessage()));
    }
}
