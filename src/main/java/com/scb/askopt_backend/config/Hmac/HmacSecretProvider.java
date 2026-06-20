package com.scb.askopt_backend.config.Hmac;

import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.askopt_backend.exception.GlobalExceptionHandler.ApiException;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HmacSecretProvider {

    private final HmacProperties properties;

    public String getSecret(String clientId) {
        String secret = properties.getClients().get(clientId);
        if (secret == null) {
            throw new ApiException(ResultCodeEnum.GO_AGENT_CLIENT_ID_INVALID);
        }
        return secret;
    }
}
