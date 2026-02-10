package com.scb.askopt_backend.config.Hmac;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.apache.commons.codec.digest.DigestUtils;


import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HmacRequestSigner {

    private final HmacProperties properties;
    private final HmacSecretProvider secretProvider;

    public void sign(
            HttpMethod method,
            String path,
            String body,
            HttpHeaders headers
    ) {
        String clientId = properties.getClientId();
        String secret = secretProvider.getSecret(clientId);

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();

        String bodyHash = DigestUtils.sha256Hex(
                body == null ? "" : body
        );

        String payload = String.join("\n",
                method.name(),
                path,
                timestamp,
                nonce,
                bodyHash
        );

        String signature = HmacSigner.sign(payload, secret);

        headers.set("X-Client-Id", clientId);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Nonce", nonce);
        headers.set("X-Signature", signature);
    }
}
