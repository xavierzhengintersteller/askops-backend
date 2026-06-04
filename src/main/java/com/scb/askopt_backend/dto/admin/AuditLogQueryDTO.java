package com.scb.askopt_backend.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogQueryDTO {
    // 分页
    private long pageNum = 1;
    private long pageSize = 10;

    // 过滤条件
    private Long userId;
    private String module;
    private String operation;
    private String status;
    private String requestIp;
    private String requestPath;

    // 时间区间
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /**
     * 排序字段
     */
    private String sortField;

    /**
     * ascend / descend
     */
    private String sortOrder;
}