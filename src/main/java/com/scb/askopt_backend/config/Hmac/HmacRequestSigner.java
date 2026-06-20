package com.scb.askopt_backend.config.Hmac;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class HmacRequestSigner {

    private final HmacProperties properties;
    private final HmacSecretProvider secretProvider;

    /**
     * 生成HMAC签名（已移除Nonce）
     */
    public void sign(
            HttpMethod method,
            String path,
            String body,
            HttpHeaders headers
    ) {
        String clientId = properties.getClientId();
        String secret = secretProvider.getSecret(clientId);
        // 时间戳：秒级时间
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        // 请求体SHA256哈希
        String bodyHash = DigestUtils.sha256Hex(body == null ? "" : body);

        // 签名原文：方法 + 路径 + 时间戳 + bodyHash（移除nonce）
        String payload = String.join("\n",
                method.name(),
                path,
                timestamp,
                bodyHash
        );

        String signature = HmacSigner.sign(payload, secret);

        // 设置请求头（移除 X-Nonce）
        headers.set("X-Client-Id", clientId);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Signature", signature);
    }
}