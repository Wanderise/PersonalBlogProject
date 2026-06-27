package com.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.mapper.ConversationMapper;
import com.third.mapper.MessageMapper;
import com.third.pojo.dto.ConversationsDTO;
import com.third.pojo.entity.AIMessage;
import com.third.pojo.entity.Conversations;
import com.third.pojo.vo.AIMessageVO;
import com.third.pojo.vo.ConversationsVO;
import com.third.service.ConversationService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话服务impl
 *
 * @author 123
 * @date 2026/06/14
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private MessageMapper messageMapper;

    /**
     * 断言对话所有者
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     */
    private void assertConversationOwner(Integer conversationId, Integer userId) {
        Long count = conversationMapper.selectCount(new LambdaQueryWrapper<Conversations>()
                .eq(Conversations::getId, conversationId)
                .eq(Conversations::getUserId, userId));
        if (count == null || count == 0) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
    }

    /**
     * 添加对话
     *
     * @param conversationsDTO 对话dto
     * @param userId           用户ID
     * @return {@link ConversationsVO }
     */
    @Override
    public ConversationsVO addConversation(ConversationsDTO conversationsDTO, Integer userId) {
        Conversations conversations = new Conversations();
        BeanUtils.copyProperties(conversationsDTO, conversations);
        conversations.setUserId(userId);
        conversations.setGmtCreate(LocalDateTime.now());
        conversations.setGmtModified(LocalDateTime.now());
        conversationMapper.addConversation(conversations);
        ConversationsVO conversationsVO = new ConversationsVO();
        BeanUtils.copyProperties(conversations, conversationsVO);
        return conversationsVO;
    }

    /**
     * 更改对话名称
     *
     * @param id               ID
     * @param conversationsDTO 对话dto
     * @param userId           用户ID
     */
    @Override
    public void changeConversationName(Integer id, ConversationsDTO conversationsDTO, Integer userId) {
        assertConversationOwner(id, userId);
        LambdaUpdateWrapper<Conversations> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Conversations::getId, id)
                .set(Conversations::getTitle, conversationsDTO.getTitle());
        conversationMapper.update(wrapper);
    }

    /**
     * 删除对话
     *
     * @param id     ID
     * @param userId 用户ID
     */
    @Override
    public void deleteConversation(Integer id, Integer userId) {
        assertConversationOwner(id, userId);
        LambdaQueryWrapper<Conversations> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversations::getId, id);
        conversationMapper.delete(wrapper);
        LambdaQueryWrapper<AIMessage> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(AIMessage::getConversationId, id);
        messageMapper.delete(wrapper1);
    }

    /**
     * 获取对话消息
     *
     * @param id     ID
     * @param userId 用户ID
     * @return {@link List }<{@link Message }>
     */
    @Override
    public List<Message> getConversationMessages(Integer id, Integer userId) {
        assertConversationOwner(id, userId);
        LambdaQueryWrapper<AIMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessage::getConversationId, id)
                .orderByAsc(AIMessage::getGmtCreate);
        List<AIMessage> aiMessages = messageMapper.selectList(wrapper);
        return aiMessages.stream().map(message ->
                "user".equals(message.getRole())
                        ? new UserMessage(message.getContent())
                        : new AssistantMessage(message.getContent())
        ).collect(Collectors.toList());
    }

    /**
     * 获取对话消息vo
     *
     * @param id     ID
     * @param userId 用户ID
     * @return {@link List }<{@link AIMessageVO }>
     */
    @Override
    public List<AIMessageVO> getConversationMessagesVO(Integer id, Integer userId) {
        assertConversationOwner(id, userId);
        LambdaQueryWrapper<AIMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessage::getConversationId, id)
                .orderByAsc(AIMessage::getGmtCreate);
        List<AIMessage> aiMessages = messageMapper.selectList(wrapper);
        return aiMessages.stream().map(message -> {
            AIMessageVO aiMessageVO = new AIMessageVO();
            BeanUtils.copyProperties(message, aiMessageVO);
            return aiMessageVO;
        }).collect(Collectors.toList());
    }

    /**
     * 保存消息
     *
     * @param aiMessage ai消息
     */
    @Override
    public void saveMessage(AIMessage aiMessage) {
        messageMapper.insert(aiMessage);
    }

    /**
     * 获取对话
     *
     * @param userId 用户ID
     * @return {@link List }<{@link ConversationsVO }>
     */
    @Override
    public List<ConversationsVO> getConversations(Integer userId) {
        LambdaQueryWrapper<Conversations> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversations::getUserId, userId)
                .orderByDesc(Conversations::getGmtModified);
        List<Conversations> conversations = conversationMapper.selectList(wrapper);
        return conversations.stream().map(conversation -> {
            ConversationsVO conversationsVO = new ConversationsVO();
            BeanUtils.copyProperties(conversation, conversationsVO);
            return conversationsVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveUserMessage(String content, Integer conversationId, Integer userId) {
        assertConversationOwner(conversationId, userId);
        AIMessage aiMessage = new AIMessage();
        aiMessage.setRole("user");
        aiMessage.setContent(content);
        aiMessage.setConversationId(conversationId);
        aiMessage.setGmtCreate(LocalDateTime.now());
        saveMessage(aiMessage);
        conversationMapper.update(new LambdaUpdateWrapper<Conversations>()
                .eq(Conversations::getId, conversationId)
                .eq(Conversations::getUserId, userId)
                .set(Conversations::getGmtModified, LocalDateTime.now()));
    }
}
