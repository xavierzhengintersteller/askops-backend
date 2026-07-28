package com.scb.tb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.scb.tb.askopt_backend.dto.podman.PortDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "container_info", autoResultMap = true)
public class ContainerInfoPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nodeIp;
    private String containerId;
    private String containerName;
    private String image;
    private String state;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenTime;
    private Boolean isDeleted;

    /** 端口列表JSONB */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<PortDTO> portsJson;

    /** 仅存储command，其余详情字段不再入库 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraJson;

    // 已删除：private String imageId;
}