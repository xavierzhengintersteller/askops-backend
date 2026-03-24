package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_permission")
public class SysPermission {

    private Long id;

    /** 权限编码，如 containers:read */
    private String permissionCode;

    /** URL 匹配模式，如 /containers/** */
    private String urlPattern;

    /** HTTP 方法，如 GET / POST / * */
    private String httpMethod;

    private String description;
}
