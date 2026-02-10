package com.scb.askopt_backend.config.Hmac;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class HmacRestTemplateConfig {

    private final HmacRequestSigner signer;


    @Bean
    public RestTemplate restTemplate(HmacRequestSigner signer) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            signer.sign(request.getMethod(), request.getURI().getPath(),
                    new String(body, StandardCharsets.UTF_8), request.getHeaders());
            return execution.execute(request, body);
        });
        return restTemplate;
    }

}
