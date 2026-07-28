package com.scb.tb.askopt_backend.dto.admin;
import lombok.Data;

@Data
public class UserPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String username;
    private Integer status;
}