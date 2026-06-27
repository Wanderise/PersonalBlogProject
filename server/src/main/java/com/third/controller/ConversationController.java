package com.third.controller;

import com.third.common.context.UserContext;
import com.third.common.result.Result;
import com.third.pojo.dto.ConversationsDTO;
import com.third.pojo.entity.AIMessage;
import com.third.pojo.vo.AIMessageVO;
import com.third.pojo.vo.ConversationsVO;
import com.third.service.AgentService;
import com.third.service.ConversationService;
import com.third.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequestMapping("/ai")
@RestController
public class ConversationController {

    private final DeepSeekChatModel chatModel;

    @Autowired
    public ConversationController(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired
    private ChatClient chatClient;

    @GetMapping("/conversations")
    public Result<List<ConversationsVO>> getConversations() {
        Integer userId = UserContext.getUserId();
        List<ConversationsVO> conversations = conversationService.getConversations(userId);
        return Result.success(conversations);
    }

    @PostMapping("/conversations")
    public Result<ConversationsVO> addConversation(@RequestBody ConversationsDTO conversationsDTO) {
        Integer userId = UserContext.getUserId();
        ConversationsVO conversationsVO = conversationService.addConversation(conversationsDTO, userId);
        return Result.success(conversationsVO);
    }

    @PutMapping("/conversations/{id}")
    public Result updateConversation(@PathVariable Integer id, @RequestBody ConversationsDTO conversationsDTO) {
        Integer userId = UserContext.getUserId();
        conversationService.changeConversationName(id, conversationsDTO, userId);
        return Result.success();
    }

    @DeleteMapping("/conversations/{id}")
    public Result<ConversationsVO> deleteConversation(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        conversationService.deleteConversation(id, userId);
        return Result.success();
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<AIMessageVO>> getConversationMessages(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        List<AIMessageVO> messages = conversationService.getConversationMessagesVO(id, userId);
        return Result.success(messages);
    }

    @GetMapping("/generate")
    public Result<Map<String, String>> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Result.success(Map.of("generation", chatModel.call(message)));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generateStream(@RequestParam String message,
                                       @RequestParam Integer conversationId,
                                       @RequestParam(required = false) Integer agentId,
                                       @RequestParam(required = false) String knowledgeBaseIds,
                                       @RequestParam(required = false) String articleIds) {
        Integer userId = UserContext.getUserId();
        String systemPrompt = agentId != null ? agentService.resolveSystemPrompt(agentId, userId) : "你是一个有帮助的AI助手";
        String ragPrompt = knowledgeBaseIds != null && !knowledgeBaseIds.isBlank()
                ? knowledgeBaseService.queryKnowledgeBase(knowledgeBaseIds, message, userId)
                : "";

        String prompt = """
                %s

                请仅根据下面从知识库中检索到的资料回答问题。
                %s

                问题:
                %s

                如果问题和知识库数据不直接相关就间接回答问题。
                """.formatted(systemPrompt, ragPrompt, message);

        List<Message> messages = conversationService.getConversationMessages(conversationId, userId);

        conversationService.saveUserMessage(message, conversationId, userId);
        AtomicReference<String> fullResponse = new AtomicReference<>("");
        return chatClient.prompt(prompt).user(message).messages(messages).stream().content()
                .doOnNext(chunk -> fullResponse.updateAndGet(s -> s + chunk))
                .doOnError(e -> log.error("流式聊天出错: {}", e.getMessage(), e))
                .doOnComplete(() -> {
                    AIMessage aiMessage = new AIMessage();
                    aiMessage.setRole("assistant");
                    aiMessage.setContent(fullResponse.get());
                    aiMessage.setConversationId(conversationId);
                    aiMessage.setGmtCreate(LocalDateTime.now());
                    conversationService.saveMessage(aiMessage);
                });
    }
}
