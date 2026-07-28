package com.scb.tb.askopt_backend.dto.admin;

import lombok.Data;

@Data
public class BlacklistUserDTO {
    private Long userId;
    private Boolean enabled;
}
