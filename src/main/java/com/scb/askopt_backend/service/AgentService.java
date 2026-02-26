package com.scb.askopt_backend.service;


import com.scb.askopt_backend.dto.AgentRegister.AgentHeartbeatRequest;
import com.scb.askopt_backend.dto.AgentRegister.AgentRegisterRequest;
import com.scb.askopt_backend.dto.AgentRegister.AgentRegisterResponse;
import com.scb.askopt_backend.dto.AgentRegister.AgentReportRequest;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.mapper.AgentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agent management service responsible for handling agent registration, heartbeat, and other related operations.
 * This service will interact with the database to store and retrieve agent information, and also handle the business logic related to agent lifecycle management.
 */
@Service
public class AgentService {
    @Autowired
    private AgentMapper agentMapper;

    /**
     * Agent 注册
     * @param request 注册请求
     * @return 注册响应
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentRegisterResponse register(AgentRegisterRequest request) {
        AgentRegisterResponse response = new AgentRegisterResponse();

        Agent existAgent = agentMapper.selectByName(request.getName());

        if (Objects.nonNull(existAgent)) {
            // AgentName已注册，返回已存在的Agent ID
            response.setSuccess(true);
            response.setMessage("该Agent已注册");
            response.setAgentId(existAgent.getId());
            return response;
        }

        // 2. 新增Agent记录
        Agent newAgent = new Agent();
        newAgent.setIp(request.getIp());
        newAgent.setPort(request.getPort());
        newAgent.setGroupId(request.getGroupId());
        newAgent.setName(request.getName());
        newAgent.setStatus("ONLINE");
        newAgent.setLastHeartbeatTime(LocalDateTime.now());
        newAgent.setCreateTime(LocalDateTime.now());
        newAgent.setUpdateTime(LocalDateTime.now());

        agentMapper.insertAgent(newAgent);

        // 3. 返回注册结果
        response.setSuccess(true);
        response.setMessage("注册成功");
        response.setAgentId(newAgent.getId());
        return response;
    }

    /**
     * Agent 心跳上报（更新在线状态）
     * @param request 心跳请求
     * @return 是否成功
     */
    public Boolean heartbeat(AgentHeartbeatRequest request) {
        // 1. 校验Agent ID和IP是否匹配
        Agent agent = agentMapper.selectById(request.getAgentId());
        if (Objects.isNull(agent) || !agent.getIp().equals(request.getIp())) {
            return false;
        }

        // 2. 更新心跳时间和状态
        agent.setStatus("ONLINE");
        agent.setLastHeartbeatTime(LocalDateTime.now());
        agent.setUpdateTime(LocalDateTime.now());

        agentMapper.updateHeartbeat(agent);
        return true;
    }

    /**
     * Agent 上报容器数据（可扩展：存储到数据库/消息队列）
     * @param request 上报请求
     * @return 是否成功
     */
    public Boolean report(AgentReportRequest request) {
        // 1. 校验Agent合法性
        Agent agent = agentMapper.selectById(request.getAgentId());
        if (Objects.isNull(agent) || !agent.getIp().equals(request.getIp())) {
            return false;
        }

        // 2. 处理上报数据（示例：可存储到数据库/转发到消息队列）
        // TODO: 实际场景中，可将containers数据关联Agent ID存储，或推送到MQ供其他服务消费
        System.out.println("Agent " + request.getAgentId() + " 上报容器数据：" + request.getContainers());

        return true;
    }
}
