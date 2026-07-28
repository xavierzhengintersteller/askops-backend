package com.scb.tb.askopt_backend.dto.admin;

import lombok.Data;

@Data
public class UpdateUserPwdDTO {
    private Long userId;
    private String newPassword;
}