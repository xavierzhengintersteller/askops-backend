package com.scb.askopt_backend.dto.admin;

import lombok.Data;

@Data
public class UpdateUserPwdDTO {
    private Long userId;
    private String newPassword;
}