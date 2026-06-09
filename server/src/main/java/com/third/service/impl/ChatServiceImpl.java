package com.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.third.common.context.UserContext;
import com.third.mapper.*;
import com.third.parser.factory.DocumentReaderFactory;
import com.third.parser.reader.FileDocumentReader;
import com.third.pojo.dto.*;
import com.third.pojo.dto.AIMessage;
import com.third.pojo.entity.*;
import com.third.pojo.vo.*;
import com.third.service.ChatService;
import com.third.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    FileService fileService;

    @Autowired
    DocumentReaderFactory documentReaderFactory;

    @Autowired
    VectorStore vectorStore;

    @Autowired
    ArticleMapper articleMapper;
    @Autowired
    ConversationMapper conversationMapper;
    @Autowired
    AgentMapper agentMapper;
    @Autowired
    MessageMapper messageMapper;
    @Autowired
    KnowledgeBaseMapper knowledgeBaseMapper;
    @Autowired
    RagFileMapper ragFileMapper;

    public String sha256(MultipartFile file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(file.getBytes());

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    @Override
    public KnowledgeBaseVO addKnowledgeBase(KnowledgeBaseDTO knowledgeBaseDTO) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(knowledgeBaseDTO, knowledgeBase);
        Integer userId = UserContext.getUserId();
        knowledgeBase.setGmtCreate(LocalDateTime.now());
        knowledgeBase.setUserId(userId);
        knowledgeBaseMapper.insert(knowledgeBase);
        KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, knowledgeBaseVO);
        return knowledgeBaseVO;

    }

    @Override
    public List<KnowledgeBaseVO> getKnowledgeBase() {
        Integer userId = UserContext.getUserId();
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId);
        return knowledgeBaseMapper.selectList(wrapper).stream().map(knowledgeBase -> {
            KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
            BeanUtils.copyProperties(knowledgeBase, knowledgeBaseVO);
            return knowledgeBaseVO;
        }).collect(Collectors.toList());

    }

    @Override
    public List<RagFileVO> uploadRagFile(RagFileDTO ragFileDTO) {

        List<MultipartFile> multipartFiles = ragFileDTO.getFiles();
        List<RagFileVO> ragFileVOList = new ArrayList<>();



        for (MultipartFile file : multipartFiles) {
            //      MYSQL存储
            try {

                RagFile ragFile = new RagFile();
                ragFile.setKnowledgeBaseId(ragFileDTO.getKnowledgeBaseId());
                ragFile.setFileType(file.getContentType());
                ragFile.setTitle(file.getName());
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("READY");
                ragFile.setR2Key("knowledge/" + file.getOriginalFilename());
                String hash = sha256(file);
                RagFile hashexist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>().eq(RagFile::getHash, hash));
                RagFile titleexist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>().eq(RagFile::getTitle, file.getName()));
                if (hashexist != null) {
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(ragFile, ragFileVO);
                    ragFileVOList.add(ragFileVO);
                    continue;
                }
                Double version = 0.01;
                if (titleexist != null) {
                    version = titleexist.getVersion() + 0.01;
                    ragFile.setVersion(version);
                }

                ragFile.setHash(hash);
                ragFileMapper.insert(ragFile);

                RagFileVO ragFileVO = new RagFileVO();
                BeanUtils.copyProperties(ragFile, ragFileVO);
                ragFileVOList.add(ragFileVO);
                //        Qdrant存储
                FileDocumentReader reader = documentReaderFactory.getReader(file.getContentType());
                List<Document> documents = reader.read(file);
                TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
                List<Document> documentList = tokenTextSplitter.apply(documents);
                for (Document document : documentList) {
                    document.getMetadata().put("document_id", ragFile.getId());
                    document.getMetadata().put("kb_id", ragFileDTO.getKnowledgeBaseId());
                    document.getMetadata().put("version", version);
                }
                vectorStore.add(documentList);

//        r2存储
                fileService.uploadFile("knowledge/" + file.getOriginalFilename() ,file);

            } catch (Exception e){
                log.error(e.getMessage());
            }
        }

        return ragFileVOList;


    }

    @Override
    public List<RagFileVO> uploadRagArticle(RagFileDTO ragFileDTO) {
        List<Integer> articleIds = ragFileDTO.getArticleIds();
        List<RagFileVO> ragFileVOList = new ArrayList<>();
        for  (Integer articleId : articleIds) {
            try {
                Article article = articleMapper.getArticleById(articleId);
                String content = article.getContent();
                RagFile ragFile = new RagFile();
                ragFile.setKnowledgeBaseId(ragFileDTO.getKnowledgeBaseId());
                ragFile.setFileType("md");
                ragFile.setTitle(article.getTitle());
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("READY");
                ragFile.setR2Key("knowledge/" + article.getTitle() + ".md");
                String hash = sha256(content);
                ragFile.setHash(hash);
                RagFile hashExist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>().eq(RagFile::getHash, hash));
                if(hashExist != null){
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(ragFile, ragFileVO);
                    ragFileVOList.add(ragFileVO);
                    continue;
                }
                ragFile.setVersion(article.getVersion());
                ragFileMapper.insert(ragFile);

                TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
                List<Document> text = tokenTextSplitter.apply(List.of(new Document(content)));
                for (Document document : text) {
                    document.getMetadata().put("document_id", ragFile.getId());
                    document.getMetadata().put("kb_id", ragFileDTO.getKnowledgeBaseId());
                    document.getMetadata().put("version", article.getVersion());
                }
                vectorStore.add(text);

                fileService.uploadMarkdown("knowledge/" + article.getTitle() + ".md", content);

            } catch (Exception e){
                log.error(e.getMessage());
            }
        }
        return ragFileVOList;
    }
}
