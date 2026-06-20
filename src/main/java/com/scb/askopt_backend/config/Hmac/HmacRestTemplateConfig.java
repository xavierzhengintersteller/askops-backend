package com.scb.askopt_backend.config.Hmac;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HmacRestTemplateConfig {

    // 连接超时 1s
    private static final int CONNECT_TIMEOUT = 1000;
    // 读取超时 1s
    private static final int READ_TIMEOUT = 10000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(requestFactory);
    }
}