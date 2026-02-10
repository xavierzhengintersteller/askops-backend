package com.scb.askopt_backend.config.Hmac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HmacSecretProvider {

    private final HmacProperties properties;

    public String getSecret(String clientId) {
        String secret = properties.getClients().get(clientId);
        if (secret == null) {
            throw new IllegalStateException("Unknown clientId: " + clientId);
        }
        return secret;
    }
}
