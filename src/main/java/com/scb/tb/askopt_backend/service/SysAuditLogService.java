package com.scb.tb.askopt_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.tb.askopt_backend.dto.admin.AuditLogOptionsDTO;
import com.scb.tb.askopt_backend.dto.admin.AuditLogQueryDTO;
import com.scb.tb.askopt_backend.entity.SysAuditLog;
import com.scb.tb.askopt_backend.mapper.SysAuditLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

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

        Page<SysAuditLog> page =
                new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<SysAuditLog> wrapper =
                new LambdaQueryWrapper<>();

        wrapper
                .eq(dto.getUserId() != null,
                        SysAuditLog::getUserId,
                        dto.getUserId())

                .eq(StringUtils.hasText(dto.getModule()),
                        SysAuditLog::getModule,
                        dto.getModule())

                .eq(StringUtils.hasText(dto.getOperation()),
                        SysAuditLog::getOperation,
                        dto.getOperation())

                .eq(StringUtils.hasText(dto.getStatus()),
                        SysAuditLog::getStatus,
                        dto.getStatus())

                .like(StringUtils.hasText(dto.getRequestIp()),
                        SysAuditLog::getRequestIp,
                        dto.getRequestIp())

                .eq(StringUtils.hasText(dto.getRequestPath()),
                        SysAuditLog::getRequestPath,
                        dto.getRequestPath())

                .ge(dto.getStartTime() != null,
                        SysAuditLog::getCreateTime,
                        dto.getStartTime())

                .le(dto.getEndTime() != null,
                        SysAuditLog::getCreateTime,
                        dto.getEndTime());

        applySort(wrapper, dto);

        return page(page, wrapper);
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
    private void applySort(
            LambdaQueryWrapper<SysAuditLog> wrapper,
            AuditLogQueryDTO dto) {

        boolean asc =
                "ascend".equalsIgnoreCase(dto.getSortOrder());

        String field = dto.getSortField();

        if (!StringUtils.hasText(field)) {
            wrapper.orderByDesc(SysAuditLog::getCreateTime);
            return;
        }

        switch (field) {

            case "id" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getId);

            case "userId" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getUserId);

            case "module" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getModule);

            case "operation" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getOperation);

            case "status" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getStatus);

            case "requestIp" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getRequestIp);
            case "requestPath" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getRequestPath);

            case "costTime" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getCostTime);

            case "httpCode" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getHttpCode);

            case "createTime" ->
                    wrapper.orderBy(true, asc, SysAuditLog::getCreateTime);

            default ->
                    wrapper.orderByDesc(SysAuditLog::getCreateTime);
        }
    }
}