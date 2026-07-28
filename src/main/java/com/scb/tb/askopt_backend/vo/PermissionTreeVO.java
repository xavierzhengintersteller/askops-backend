package com.scb.tb.askopt_backend.vo;

import lombok.Data;
import java.util.List;

@Data
public class PermissionTreeVO {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Long parentId;
    private String type;
    private String urlPattern;
    private String httpMethod;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Boolean visible;
    private String description;

    private List<PermissionTreeVO> children;
}