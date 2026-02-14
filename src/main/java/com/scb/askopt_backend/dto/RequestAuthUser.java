package com.scb.askopt_backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class RequestAuthUser {
    private String username;
    private List<String> roles;
    private Set<String> permissions;
}
