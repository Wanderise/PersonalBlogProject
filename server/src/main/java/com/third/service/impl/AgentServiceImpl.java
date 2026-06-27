package com.third.service.impl;

import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.mapper.AgentMapper;
import com.third.pojo.dto.AgentDTO;
import com.third.pojo.entity.Agent;
import com.third.pojo.vo.AgentVO;
import com.third.service.AgentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private AgentMapper agentMapper;

    /**
     * 添加Agent
     *
     * @param agentDTO 代理人dto
     * @param userId   用户ID
     * @return {@link AgentVO }
     */
    @Override
    public AgentVO addAgent(AgentDTO agentDTO, Integer userId) {
        AgentVO agentVO = new AgentVO();
        Agent agent = new Agent();
        BeanUtils.copyProperties(agentDTO, agent);
        agent.setGmtCreate(LocalDateTime.now());
        agent.setUserId(userId);
        agentMapper.addAgent(agent);
        BeanUtils.copyProperties(agent, agentVO);
        return agentVO;
    }

    /**
     * 获取Agent
     *
     * @param userId 用户ID
     * @return {@link List }<{@link AgentVO }>
     */
    @Override
    public List<AgentVO> getAgents(Integer userId) {
        return agentMapper.getAgents(userId);
    }

    /**
     * 按id删除Agent
     *
     * @param id     ID
     * @param userId 用户ID
     */
    @Override
    @CacheEvict(value = "agent:systemprompt", key = "#userId + ':' + #id")
    public void deleteAgentById(Integer id, Integer userId) {
        Long count = agentMapper.countOwnership(id, userId);
        if (count == null || count == 0) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
        agentMapper.deleteAgentById(id);
    }

    /**
     * 处理系统提示
     *
     * @param agentId Agentid
     * @param userId  用户ID
     * @return {@link String }
     */
    @Override
    @Cacheable(value = "agent:systemprompt", key = "#userId + ':' + #agentId")
    public String resolveSystemPrompt(Integer agentId, Integer userId) {
        List<AgentVO> agents = getAgents(userId);
        String prompt = "你是一个有帮助的AI助手";
        if (agentId != null) {
            for (AgentVO agent : agents) {
                if (Objects.equals(agent.getId(), agentId)) {
                    prompt = agent.getSystemPrompt();
                    break;
                }
            }
        }
        return prompt;
    }
}
