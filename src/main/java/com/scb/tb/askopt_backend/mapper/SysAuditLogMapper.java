package com.scb.tb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.tb.askopt_backend.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
    @Select("""
        SELECT DISTINCT module
        FROM sys_audit_log
        WHERE module IS NOT NULL
        ORDER BY module
        """)
    List<String> selectDistinctModules();

    @Select("""
        SELECT DISTINCT operation
        FROM sys_audit_log
        WHERE operation IS NOT NULL
        ORDER BY operation
        """)
    List<String> selectDistinctOperations();

    @Select("""
        SELECT DISTINCT status
        FROM sys_audit_log
        WHERE status IS NOT NULL
        ORDER BY status
        """)
    List<String> selectDistinctStatuses();

    @Select("""
        SELECT DISTINCT request_path
        FROM sys_audit_log
        WHERE request_path IS NOT NULL
        ORDER BY request_path
        """)
    List<String> selectDistinctRequestPaths();
}