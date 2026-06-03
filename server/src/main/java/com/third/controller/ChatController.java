package com.third.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.third.common.result.Result;
import com.third.pojo.dto.AIMessage;
import com.third.pojo.dto.AgentDTO;
import com.third.pojo.dto.ConversationsDTO;
import com.third.pojo.entity.Conversations;
import com.third.pojo.vo.AgentVO;
import com.third.pojo.vo.ConversationsVO;
import com.third.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/ai")
@RestController
public class ChatController {

    private final DeepSeekChatModel chatModel;

    @Autowired
    public ChatController(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Autowired
    ChatService chatService;

    @Autowired
    ChatClient chatClient;

    public void save(String prompt, Long conversationId){
        AIMessage aiMessage = new AIMessage();
        aiMessage.setRole("user");
        aiMessage.setContent(prompt);
        aiMessage.setConversationId(conversationId);
        aiMessage.setGmtCreate(LocalDateTime.now());
        chatService.saveMessage(aiMessage);
    }

    @PostMapping("/agents")
    public Result<AgentVO> addAgent(@RequestBody AgentDTO agentDTO) {
        log.info("addAgent: {}", agentDTO);
        AgentVO agentVO = chatService.addAgent(agentDTO);
        return Result.success(agentVO);
    }

    @GetMapping("/agents")
    public Result<List<AgentVO>> getAgentList() {
        List<AgentVO> agentList = chatService.getAgents();
        log.info("getAgent: {}", agentList);
        return Result.success(agentList);
    }

    @DeleteMapping("/agents/{id}")
    public Result<AgentVO> deleteAgent(@PathVariable Integer id) {
        log.info("deleteAgent: {}", id);
        chatService.deleteAgentById(id);
        return Result.success();
    }

    @PostMapping("/conversations")
    public Result<ConversationsVO> addConversation(@RequestBody ConversationsDTO conversationsDTO) {
        log.info("addConversation: {}", conversationsDTO);
        ConversationsVO conversationsVO = chatService.addConversation(conversationsDTO);
        return Result.success(conversationsVO);
    }

    @PutMapping("/conversations/{id}")
    public Result updateConversation(@PathVariable Integer id, @RequestBody ConversationsDTO conversationsDTO) {
        log.info("updateConversation: {}", id);
        chatService.changeConversationName(id, conversationsDTO);
        return Result.success();
    }

    @DeleteMapping("/conversations/{id}")
    public Result<ConversationsVO> deleteConversation(@PathVariable Integer id) {
        log.info("deleteConversation: {}", id);
        chatService.deleteConversation(id);
        return Result.success();
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<Message>> getConversationMessages(@PathVariable Long id) {
        log.info("getConversationMessages: {}", id);
        List<Message> messages = chatService.getConversationMessages(id);
        return Result.success(messages);
    }


    @GetMapping("/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Map.of("generation", chatModel.call(message));
    }



    @GetMapping(value = "/chat/stream", produces = "text/html;charset=UTF-8")
    public Flux<String> generateStream(@PathVariable String message, @PathVariable Long ConversationId, @PathVariable Long AgentId) {
        List<AgentVO> agentList = chatService.getAgents();
        String prompt = "";
        for (AgentVO agentVO : agentList) {
            if (agentVO.getId().longValue() != AgentId)
                continue;
            prompt = agentVO.getSystemPrompt();
        }
        List<Message> messages = chatService.getConversationMessages(ConversationId);
        save(prompt, ConversationId);
        return chatClient.prompt(prompt).user(message).messages(messages).stream().content();
    }
}