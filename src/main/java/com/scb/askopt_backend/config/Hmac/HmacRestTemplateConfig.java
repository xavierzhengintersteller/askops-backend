package com.scb.askopt_backend.config.Hmac;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class HmacRestTemplateConfig {

    private final HmacRequestSigner signer;

    // 配置超时时间（可抽离到配置文件，此处先硬编码，生产建议用@Value注入）
    // 连接超时：3秒（建立TCP连接的超时）
    private static final int CONNECT_TIMEOUT = 1000;
    // 读取超时：5秒（等待响应数据的超时）
    private static final int READ_TIMEOUT = 1000;

    @Bean
    public RestTemplate restTemplate(HmacRequestSigner signer) {
        // 1. 创建请求工厂，配置超时时间
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 连接超时（毫秒）：超过该时间未建立连接则抛出超时异常
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        // 读取超时（毫秒）：连接建立后，超过该时间未收到响应则抛出超时异常
        requestFactory.setReadTimeout(READ_TIMEOUT);

        // 2. 初始化RestTemplate，指定自定义请求工厂
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        // 3. 添加HMAC签名拦截器（保留原有逻辑）
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // 签名逻辑：请求方法 + 接口路径 + 请求体 + 请求头
            signer.sign(request.getMethod(), request.getURI().getPath(),
                    new String(body, StandardCharsets.UTF_8), request.getHeaders());
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}