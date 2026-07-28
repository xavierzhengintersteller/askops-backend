package com.scb.tb.askopt_backend.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserMenuVO {
    private Long id;
    private Long parentId;
    private String permissionName;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private List<UserMenuVO> children;
}