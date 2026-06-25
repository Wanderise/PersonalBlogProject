package com.third.controller;

import com.third.common.context.UserContext;
import com.third.common.result.Result;
import com.third.pojo.dto.AgentDTO;
import com.third.pojo.vo.AgentVO;
import com.third.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/ai")
@RestController
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/agents")
    public Result<AgentVO> addAgent(@RequestBody AgentDTO agentDTO) {
        Integer userId = UserContext.getUserId();
        log.info("addAgent: {}", agentDTO);
        AgentVO agentVO = agentService.addAgent(agentDTO, userId);
        return Result.success(agentVO);
    }

    @GetMapping("/agents")
    public Result<List<AgentVO>> getAgentList() {
        Integer userId = UserContext.getUserId();
        List<AgentVO> agentList = agentService.getAgents(userId);
        log.info("getAgent: {}", agentList);
        return Result.success(agentList);
    }

    @DeleteMapping("/agents/{id}")
    public Result<AgentVO> deleteAgent(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        log.info("deleteAgent: {}", id);
        agentService.deleteAgentById(id, userId);
        return Result.success();
    }
}
