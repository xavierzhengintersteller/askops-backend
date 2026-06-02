package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.entity.SysAuditLog;
import com.scb.askopt_backend.mapper.SysAuditLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysAuditLogService extends ServiceImpl<SysAuditLogMapper, SysAuditLog> {

    /**
     * 异步保存审计日志，独立事务，不影响主业务
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asyncSave(SysAuditLog log) {
        this.save(log);
    }
}