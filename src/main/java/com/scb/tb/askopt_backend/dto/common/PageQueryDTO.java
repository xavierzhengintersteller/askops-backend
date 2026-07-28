package com.scb.tb.askopt_backend.dto.common;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    private Long pageSize = 20L;
    // 新增排序字段
    private String sortField;
    // 排序方向 asc / desc
    private String sortOrder;
}