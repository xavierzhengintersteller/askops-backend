package com.scb.askopt_backend.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AddUserDTO {
    private String username;
    private String password;
    private List<Long> roleIds; // 选中的角色ID
}