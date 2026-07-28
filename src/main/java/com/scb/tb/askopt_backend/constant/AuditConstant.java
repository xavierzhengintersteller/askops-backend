package com.scb.tb.askopt_backend.constant;

/**
 * 审计操作类型常量（纯英文，企业规范）
 */
public class AuditConstant {

    // 新增
    public static final String CREATE = "CREATE";
    // 修改
    public static final String UPDATE = "UPDATE";
    // 删除
    public static final String DELETE = "DELETE";
    // 查询
    public static final String QUERY = "QUERY";
    // 登录
    public static final String LOGIN = "LOGIN";
    // 登出
    public static final String LOGOUT = "LOGOUT";
    // 导出
    public static final String EXPORT = "EXPORT";
    // 导入
    public static final String IMPORT = "IMPORT";

    // 新增：审计状态（批量接口专用）
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PARTIAL_FAIL = "PARTIAL_FAIL";
    public static final String STATUS_FAIL = "FAIL";
}