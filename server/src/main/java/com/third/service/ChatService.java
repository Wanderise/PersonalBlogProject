package com.third.service;

import com.third.pojo.dto.AIMessage;
import com.third.pojo.dto.AgentDTO;
import com.third.pojo.dto.ConversationsDTO;
import com.third.pojo.vo.AIMessageVO;
import com.third.pojo.vo.AgentVO;
import com.third.pojo.vo.ConversationsVO;
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
}
