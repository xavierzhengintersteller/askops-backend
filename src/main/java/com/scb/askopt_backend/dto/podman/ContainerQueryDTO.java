package com.scb.askopt_backend.dto.podman;

import lombok.Data;
import jakarta.validation.constraints.Min;
import java.util.List;

@Data
public class ContainerQueryDTO {
    // 业务筛选
    private List<String> nodeIps;
    // 控制标记：是否强制同步DB
    private boolean manual = false;
    // 分页
    @Min(1)
    private Long pageNum = 1L;
    @Min(1)
    private Long pageSize = 20L;
    // 排序
    private String sortField;
    private String sortOrder = "desc";
}