package com.third.service;

import com.third.pojo.dto.*;
import com.third.pojo.entity.AIMessage;
import com.third.pojo.vo.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChatService {

    AgentVO addAgent(AgentDTO agentDTO);

    List<AgentVO> getAgents();

    void deleteAgentById(Integer id);

    ConversationsVO addConversation(ConversationsDTO conversationsDTO);

    void changeConversationName(Integer id, ConversationsDTO conversationsDTO);

    void deleteConversation(Integer id);

    List<Message> getConversationMessages(Integer id);

    List<AIMessageVO> getConversationMessagesVO(Integer id);

    void saveMessage(AIMessage aiMessage);

    List<ConversationsVO> getConversations();

    KnowledgeBaseVO addKnowledgeBase(KnowledgeBaseDTO knowledgeBaseDTO);

    List<KnowledgeBaseVO> getKnowledgeBase();

    void deleteKnowledgeBase(Integer id);

    List<RagFileVO> uploadRagFile(RagFileDTO ragFileDTO);

    List<RagFileVO> uploadRagArticle(RagFileDTO ragFileDTO);

    String resolveSystemPrompt(Integer agentId);

    void saveUserMessage(String content, Integer conversationId);
}
