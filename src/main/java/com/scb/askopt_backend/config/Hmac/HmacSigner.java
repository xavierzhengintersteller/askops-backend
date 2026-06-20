package com.scb.askopt_backend.config.Hmac;

import com.scb.askopt_backend.exception.GlobalExceptionHandler.ApiException;
import com.scb.askopt_backend.exception.ResultCodeEnum;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class HmacSigner {

    private static final String ALGO = "HmacSHA256";

    private HmacSigner() {}

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            return HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new ApiException(ResultCodeEnum.GO_AGENT_SIGN_ERROR, e);
        }
    }
}