package com.scb.askopt_backend.config.Hmac;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "hmac")
@Data
public class HmacProperties {

    /**
     * 当前服务使用的 clientId
     */
    private String clientId;

    /**
     * clientId -> secret
     */
    private Map<String, String> clients = new HashMap<>();
}
