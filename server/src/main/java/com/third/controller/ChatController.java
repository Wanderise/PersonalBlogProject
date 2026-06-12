package com.third.controller;

import com.third.common.result.Result;
import com.third.mapper.KnowledgeBaseMapper;
import com.third.pojo.dto.*;
import com.third.pojo.entity.AIMessage;
import com.third.pojo.vo.*;
import com.third.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.Rate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/ai")
@RestController
public class ChatController {

    private final DeepSeekChatModel chatModel;
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    public ChatController(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatClient chatClient;


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

    @GetMapping("/conversations")
    public Result<List<ConversationsVO>> getConversations() {
        List<ConversationsVO> conversations = chatService.getConversations();
        log.info("getConversations: {}", conversations);
        return Result.success(conversations);
    }

    @PostMapping("/conversations")
    public Result<ConversationsVO> addConversation(@RequestBody ConversationsDTO conversationsDTO) {
        log.info("addConversation: {}", conversationsDTO);
        ConversationsVO conversationsVO = chatService.addConversation(conversationsDTO);
        log.info("conversationsVO: {}", conversationsVO);
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
    public Result<List<AIMessageVO>> getConversationMessages(@PathVariable Integer id) {
        log.info("getConversationMessages: {}", id);
        List<AIMessageVO> messages = chatService.getConversationMessagesVO(id);
        return Result.success(messages);
    }


    @GetMapping("/generate")
    public Result<Map<String, String>> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Result.success(Map.of("generation", chatModel.call(message)));
    }



    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generateStream(@RequestParam String message,
                                       @RequestParam(required = false) Integer conversationId,
                                       @RequestParam(required = false) Integer agentId,
                                       @RequestParam(required = false) String knowledgeBaseIds,
                                       @RequestParam(required = false) String articleIds) {
//        提示词
        String systemPrompt = chatService.resolveSystemPrompt(agentId);
        String ragPrompt = knowledgeBaseIds != null ? chatService.queryKnowledgeBase(knowledgeBaseIds, message) : "";

        String prompt = """
                %s
                
                请仅根据下面从知识库中检索到的资料回答问题。
                %s
                
                问题:
                %s
                
                如果问题和知识库数据不匹配就直接回答问题。
                """.formatted(systemPrompt, ragPrompt, message);

//上下文
        List<Message> messages = chatService.getConversationMessages(conversationId);


        log.info("提示词：{} \n 上下文：{}", prompt , messages);

        chatService.saveUserMessage(message, conversationId);
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
                    chatService.saveMessage(aiMessage);
                });
    }

    @PostMapping("/knowledge-bases")
    public Result<KnowledgeBaseVO> addKnowledgeBase(@RequestBody KnowledgeBaseDTO knowledgeBaseDTO) {
        log.info("addKnowledgeBase: {}", knowledgeBaseDTO);
        KnowledgeBaseVO knowledgeBaseVO = chatService.addKnowledgeBase(knowledgeBaseDTO);
        return Result.success(knowledgeBaseVO);
    }

    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBaseVO>> getKnowledgeBases() {
        List<KnowledgeBaseVO> knowledgeBasesVO = chatService.getKnowledgeBase();
        return Result.success(knowledgeBasesVO);
    }

    @DeleteMapping({"/knowledge-bases/{id}", "/knowledge-base/{id}"})
    public Result deleteKnowledgeBase(@PathVariable Integer id) {
        chatService.deleteKnowledgeBase(id);
        return  Result.success();
    }

    @GetMapping("/knowledge-bases/{id}/documents")
    public Result<List<RagFileVO>> getRagFiles(@PathVariable Integer id) {
        List<RagFileVO> ragFileVOList = chatService.getRagFiles(id);
        return Result.success(ragFileVOList);
    }


    @PostMapping(value = "/rag/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<RagFileVO>> uploadRagFile(@RequestParam("files") List<MultipartFile> files,
                                                 @RequestParam("knowledgeBaseId") Integer knowledgeBaseId) {
        RagFileDTO ragFileDTO = new RagFileDTO();
        ragFileDTO.setFiles(files);
        ragFileDTO.setKnowledgeBaseId(knowledgeBaseId);
        log.info("uploadRagFile: {}", ragFileDTO);
        List<RagFileVO> ragFileVOList = chatService.uploadRagFile(ragFileDTO);
        return Result.success(ragFileVOList);

    }

    @PostMapping("/rag/articles")
    public Result<List<RagFileVO>>  uploadRagArticles(@RequestBody RagFileDTO ragFileDTO) {
        log.info("uploadRagArticles: {}", ragFileDTO);
        List<RagFileVO> ragFileVOList = chatService.uploadRagArticle(ragFileDTO);
        return Result.success(ragFileVOList);
    }


}
