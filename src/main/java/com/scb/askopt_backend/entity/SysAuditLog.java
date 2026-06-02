package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    /**
     * 主键（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否超级管理员
     */
    private Boolean superAdmin;

    /**
     * 功能模块
     */
    private String module;

    /**
     * 操作类型：新增/修改/删除/查询/登录/登出
     */
    private String operation;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法 GET/POST/PUT/DELETE
     */
    private String requestMethod;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 客户端信息
     */
    private String userAgent;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseResult;

    /**
     * HTTP状态码
     */
    private Integer httpCode;

    /**
     * 耗时（毫秒）
     */
    private Long costTime;

    /**
     * 状态 SUCCESS / FAIL
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}