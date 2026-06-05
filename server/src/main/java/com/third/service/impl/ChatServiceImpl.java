package com.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.third.common.context.UserContext;
import com.third.mapper.AgentMapper;
import com.third.mapper.ConversationMapper;
import com.third.mapper.MessageMapper;
import com.third.pojo.dto.AIMessage;
import com.third.pojo.dto.AgentDTO;
import com.third.pojo.dto.ConversationsDTO;
import com.third.pojo.entity.Agent;
import com.third.pojo.entity.Conversations;
import com.third.pojo.vo.AIMessageVO;
import com.third.pojo.vo.AgentVO;
import com.third.pojo.vo.ConversationsVO;
import com.third.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.protocols.query.internal.marshall.ListQueryMarshaller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Autowired
    ConversationMapper conversationMapper;
    @Autowired
    AgentMapper agentMapper;
    @Autowired
    MessageMapper messageMapper;

    @Override
    public AgentVO addAgent(AgentDTO agentDTO) {
        AgentVO agentVO = new AgentVO();
        Agent agent = new Agent();
        BeanUtils.copyProperties(agentDTO, agent);
        agent.setGmtCreate(LocalDateTime.now());
        Integer userId = UserContext.getUserId();
        agent.setUserId(userId);
        agentMapper.addAgent(agent);
        BeanUtils.copyProperties(agent, agentVO);
        return agentVO;
    }

    @Override
    public List<AgentVO> getAgents() {
        Integer userId = UserContext.getUserId();
        List<AgentVO> agentList = agentMapper.getAgents(userId);
        return agentList;
    }

    @Override
    public void deleteAgentById(Integer id) {
        agentMapper.deleteAgentById(id);
    }

    @Override
    public ConversationsVO addConversation(ConversationsDTO conversationsDTO) {
        Conversations conversations = new Conversations();
        BeanUtils.copyProperties(conversationsDTO, conversations);
        Integer userId = UserContext.getUserId();
        conversations.setUserId(userId);
        conversations.setGmtModified(LocalDate.now());
        conversations.setGmtModified(LocalDate.now());

        conversationMapper.addConversation(conversations);

        ConversationsVO conversationsVO = new ConversationsVO();
        BeanUtils.copyProperties(conversations, conversationsVO);
        return conversationsVO;
    }

    @Override
    public void changeConversationName(Integer id, ConversationsDTO conversationsDTO) {
        LambdaUpdateWrapper<Conversations> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Conversations::getId, id)
                .set(Conversations::getTitle, conversationsDTO.getTitle());
        conversationMapper.update(wrapper);
    }

    @Override
    public void deleteConversation(Integer id) {
        LambdaQueryWrapper<Conversations> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversations::getId, id);
        conversationMapper.delete(wrapper);
        LambdaQueryWrapper<AIMessage> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(AIMessage::getConversationId, id);
        messageMapper.delete(wrapper1);

    }

    @Override
    public List<Message> getConversationMessages(Integer id) {
        LambdaQueryWrapper<AIMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessage::getConversationId, id);
        List<AIMessage> aiMessages = messageMapper.selectList(wrapper);
        log.info("aiMessageInfo:{}", aiMessages);
        return aiMessages.stream().map(message -> "user".equals(message.getRole()) ? new UserMessage(message.getContent()) : new AssistantMessage(message.getContent())
        ).collect(Collectors.toList());

    }

    @Override
    public List<AIMessageVO> getConversationMessagesVO(Integer id) {
        LambdaQueryWrapper<AIMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessage::getConversationId, id);
        List<AIMessage> aiMessages = messageMapper.selectList(wrapper);
        return aiMessages.stream().map(message -> {
            AIMessageVO aiMessageVO = new AIMessageVO();
            BeanUtils.copyProperties(message, aiMessageVO);
            return aiMessageVO;
        }).collect(Collectors.toList());

    }

    @Override
    public void saveMessage(AIMessage aiMessage) {
        messageMapper.insert(aiMessage);
    }

    @Override
    public List<ConversationsVO> getConversations() {
        Integer userId = UserContext.getUserId();
        LambdaQueryWrapper<Conversations> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversations::getUserId, userId);
        List<Conversations> conversations = conversationMapper.selectList(wrapper);
        return conversations.stream().map(conversation -> {
            ConversationsVO conversationsVO = new ConversationsVO();
            BeanUtils.copyProperties(conversation, conversationsVO);
            return conversationsVO;
        }).collect(Collectors.toList());
    }
}
