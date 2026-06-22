package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.dto.agent.AgentHeartbeatDTO;
import com.scb.askopt_backend.dto.agent.AgentRegisterDTO;
import com.scb.askopt_backend.entity.Agent;
import com.scb.askopt_backend.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentService extends ServiceImpl<AgentMapper, Agent> {
    private final AgentMapper agentMapper;

    @Transactional(rollbackFor = Exception.class)
    public String register(AgentRegisterDTO dto) {
        Agent exist = lambdaQuery().eq(Agent::getName, dto.getName()).one();
        LocalDateTime now = LocalDateTime.now();
        if (exist != null) {
            // 已存在则更新信息，状态改为在线
            exist.setIp(dto.getIp());
            exist.setPort(dto.getPort());
            exist.setGroupId(dto.getGroupId());
            exist.setClientId(dto.getClientId());
            exist.setHeartbeatTimeoutSec(dto.getHeartbeatTimeoutSec() == null ? 30 : dto.getHeartbeatTimeoutSec());
            exist.setStatus("ONLINE");
            exist.setLastHeartbeatTime(now);
            exist.setFailCount(0);
            updateById(exist);
            return "agent re-register success";
        }
        // 新建Agent
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setIp(dto.getIp());
        agent.setPort(dto.getPort());
        agent.setGroupId(dto.getGroupId());
        agent.setClientId(dto.getClientId());
        agent.setHeartbeatTimeoutSec(dto.getHeartbeatTimeoutSec() == null ? 30 : dto.getHeartbeatTimeoutSec());
        agent.setStatus("REGISTERING");
        agent.setLastHeartbeatTime(now);
        agent.setFailCount(0);
        save(agent);
        return "agent register success";
    }

    public String heartbeat(AgentHeartbeatDTO dto) {
        int affect = agentMapper.updateHeartbeat(dto.getName(), LocalDateTime.now());
        if (affect <= 0) {
            throw new RuntimeException("agent not found:" + dto.getName());
        }
        return "heartbeat ok";
    }

    // 定时任务：巡检超时Agent
    public void scanTimeoutAgent() {
        agentMapper.markOfflineTimeoutAgent(LocalDateTime.now());
    }

    public List<Agent> listOnlineAgent() {
        return agentMapper.selectOnlineAgent();
    }
}