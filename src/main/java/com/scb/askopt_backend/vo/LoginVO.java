package com.scb.askopt_backend.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String accessToken;   // 短期 JWT
    private String refreshToken;  // 长期刷新 token
}
