package com.third.service;

import com.third.pojo.dto.AgentDTO;
import com.third.pojo.vo.AgentVO;

import java.util.List;

public interface AgentService {

    AgentVO addAgent(AgentDTO agentDTO, Integer userId);

    List<AgentVO> getAgents(Integer userId);

    void deleteAgentById(Integer id, Integer userId);

    String resolveSystemPrompt(Integer agentId, Integer userId);
}
