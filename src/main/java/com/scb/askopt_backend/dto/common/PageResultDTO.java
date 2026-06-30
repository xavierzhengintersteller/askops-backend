package com.scb.askopt_backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultDTO<T> {
    private List<T> records;
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private Long pages;
}