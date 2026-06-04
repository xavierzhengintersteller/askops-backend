package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.dto.admin.AuditLogOptionsDTO;
import com.scb.askopt_backend.dto.admin.AuditLogQueryDTO;
import com.scb.askopt_backend.entity.SysAuditLog;
import com.scb.askopt_backend.mapper.SysAuditLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public IPage<SysAuditLog> getAuditLogPage(AuditLogQueryDTO dto) {
        Page<SysAuditLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        return lambdaQuery()
                .eq(dto.getUserId() != null, SysAuditLog::getUserId, dto.getUserId())
                .eq(StringUtils.hasText(dto.getModule()), SysAuditLog::getModule, dto.getModule())
                .eq(StringUtils.hasText(dto.getOperation()), SysAuditLog::getOperation, dto.getOperation())
                .eq(StringUtils.hasText(dto.getStatus()), SysAuditLog::getStatus, dto.getStatus())
                .like(StringUtils.hasText(dto.getRequestIp()), SysAuditLog::getRequestIp, dto.getRequestIp())
                .eq(StringUtils.hasText(dto.getRequestPath()), SysAuditLog::getRequestPath, dto.getRequestPath())
                .ge(dto.getStartTime() != null, SysAuditLog::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, SysAuditLog::getCreateTime, dto.getEndTime())
                .orderByDesc(SysAuditLog::getCreateTime)
                .page(page);
    }

    public AuditLogOptionsDTO getOptions() {

        AuditLogOptionsDTO dto = new AuditLogOptionsDTO();

        dto.setModules(baseMapper.selectDistinctModules());
        dto.setOperations(baseMapper.selectDistinctOperations());
        dto.setStatuses(baseMapper.selectDistinctStatuses());
        dto.setApiEndpoints(baseMapper.selectDistinctRequestPaths());
        return dto;
    }
    public SysAuditLog getDetail(Long id) {

        return lambdaQuery()
                .eq(SysAuditLog::getId, id)
                .one();
    }
}