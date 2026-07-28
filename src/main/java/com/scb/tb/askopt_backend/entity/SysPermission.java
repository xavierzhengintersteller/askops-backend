package com.scb.tb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {

    private Long id;

    /** 权限编码，如 containers:read */
    private String permissionCode;

    /** 权限名称，前端展示用 */
    private String permissionName;

    /** 父权限ID，树结构核心 */
    private Long parentId;

    /** 类型：menu / button / api */
    private String type;

    /** URL 匹配模式，如 /containers/** */
    private String urlPattern;

    /** HTTP 方法，如 GET / POST / * */
    private String httpMethod;

    /** 前端路由 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** UI图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 是否显示菜单 */
    private Boolean visible;

    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
