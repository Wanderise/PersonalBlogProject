package com.third.service;

import com.third.pojo.dto.ConversationsDTO;
import com.third.pojo.entity.AIMessage;
import com.third.pojo.vo.AIMessageVO;
import com.third.pojo.vo.ConversationsVO;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ConversationService {

    ConversationsVO addConversation(ConversationsDTO conversationsDTO, Integer userId);

    void changeConversationName(Integer id, ConversationsDTO conversationsDTO, Integer userId);

    void deleteConversation(Integer id, Integer userId);

    List<Message> getConversationMessages(Integer id, Integer userId);

    List<AIMessageVO> getConversationMessagesVO(Integer id, Integer userId);

    void saveMessage(AIMessage aiMessage);

    List<ConversationsVO> getConversations(Integer userId);

    void saveUserMessage(String content, Integer conversationId, Integer userId);
}
