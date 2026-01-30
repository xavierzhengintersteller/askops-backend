package com.scb.askopt_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient podmanWebClient() {
        return WebClient.builder()
                .baseUrl("http://172.29.124.186:8080")
                .build();
    }
}

