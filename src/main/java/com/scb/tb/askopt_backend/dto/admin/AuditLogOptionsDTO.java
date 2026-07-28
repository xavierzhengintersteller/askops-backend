package com.scb.tb.askopt_backend.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AuditLogOptionsDTO {

    private List<String> modules;

    private List<String> operations;

    private List<String> statuses;

    private List<String> apiEndpoints;
}