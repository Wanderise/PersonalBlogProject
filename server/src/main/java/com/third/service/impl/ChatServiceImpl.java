package com.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.third.common.context.UserContext;
import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.mapper.*;
import com.third.parser.factory.DocumentReaderFactory;
import com.third.parser.reader.FileDocumentReader;
import com.third.pojo.dto.*;
import com.third.pojo.entity.*;
import com.third.pojo.entity.AIMessage;
import com.third.pojo.vo.*;
import com.third.service.ChatService;
import com.third.service.FileService;
import com.third.service.QdrantManager;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

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
    QdrantManager qdrantManager;

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

    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

    // DashScope Embedding API限制每次最多10条，超出分批提交
    private void batchAddToVectorStore(List<Document> documents) {
        int total = documents.size();
        int batches = (total + 9) / 10;
        for (int i = 0; i < total; i += 10) {
            int end = Math.min(i + 10, total);
            log.info("Qdrant写入进度: {}/{} 批 ({} 条文档)", (i / 10 + 1), batches, end - i);
            vectorStore.add(new ArrayList<>(documents.subList(i, end)));
        }
    }

    private void assertConversationOwner(Integer conversationId) {
        Integer userId = UserContext.getUserId();
        // 防止用户越权访问他人对话
        Long count = conversationMapper.selectCount(new LambdaQueryWrapper<Conversations>()
                .eq(Conversations::getId, conversationId)
                .eq(Conversations::getUserId, userId));
        if (count == null || count == 0) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
    }

    private void assertKnowledgeBaseOwner(Integer knowledgeBaseId) {
        Integer userId = UserContext.getUserId();
        Long count = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId));
        if (count == null || count == 0) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
    }

    // 流式SHA-256，用8KB缓冲区避免大文件一次性加载到内存
    // Word的MIME类型最长71字符，DB列太短，映射为简短扩展名
    private static String simplifyContentType(String contentType) {
        if (contentType == null) return "unknown";
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/msword" -> "doc";
            case "text/plain" -> "txt";
            default -> contentType.contains("officedocument.wordprocessingml") ? "docx"
                    : contentType.length() > 20 ? contentType.substring(contentType.lastIndexOf('/') + 1)
                    : contentType;
        };
    }

    // 流式SHA-256，用8KB缓冲区避免大文件一次性加载到内存
    public static String sha256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buf = new byte[8192];
            while (dis.read(buf) != -1) { /* 消耗流以填充digest */ }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
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
        Integer userId = UserContext.getUserId();
        Long count = agentMapper.countOwnership(id, userId);
        if (count == null || count == 0) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
        agentMapper.deleteAgentById(id);
    }

    @Override
    public ConversationsVO addConversation(ConversationsDTO conversationsDTO) {
        Conversations conversations = new Conversations();
        BeanUtils.copyProperties(conversationsDTO, conversations);
        Integer userId = UserContext.getUserId();
        conversations.setUserId(userId);
        conversations.setGmtCreate(LocalDateTime.now());
        conversations.setGmtModified(LocalDateTime.now());

        conversationMapper.addConversation(conversations);

        ConversationsVO conversationsVO = new ConversationsVO();
        BeanUtils.copyProperties(conversations, conversationsVO);
        return conversationsVO;
    }

    @Override
    public void changeConversationName(Integer id, ConversationsDTO conversationsDTO) {
        assertConversationOwner(id);
        LambdaUpdateWrapper<Conversations> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Conversations::getId, id)
                .set(Conversations::getTitle, conversationsDTO.getTitle());
        conversationMapper.update(wrapper);
    }

    @Override
    public void deleteConversation(Integer id) {
        assertConversationOwner(id);
        LambdaQueryWrapper<Conversations> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversations::getId, id);
        conversationMapper.delete(wrapper);
        LambdaQueryWrapper<AIMessage> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(AIMessage::getConversationId, id);
        messageMapper.delete(wrapper1);

    }

    @Override
    public List<Message> getConversationMessages(Integer id) {
        assertConversationOwner(id);
        LambdaQueryWrapper<AIMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessage::getConversationId, id)
                .orderByAsc(AIMessage::getGmtCreate);
        List<AIMessage> aiMessages = messageMapper.selectList(wrapper);
        log.info("aiMessageInfo:{}", aiMessages);
        return aiMessages.stream().map(message -> "user".equals(message.getRole()) ? new UserMessage(message.getContent()) : new AssistantMessage(message.getContent())
        ).collect(Collectors.toList());

    }

    @Override
    public List<AIMessageVO> getConversationMessagesVO(Integer id) {
        assertConversationOwner(id);
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

    @Override
    public void saveMessage(AIMessage aiMessage) {
        messageMapper.insert(aiMessage);
    }

    @Override
    public List<ConversationsVO> getConversations() {
        Integer userId = UserContext.getUserId();
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
            KnowledgeBaseVO vo = new KnowledgeBaseVO();
            BeanUtils.copyProperties(knowledgeBase, vo);
            Long count = ragFileMapper.selectCount(
                    new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, knowledgeBase.getId()));
            vo.setDocCount(count != null ? count : 0);
            return vo;
        }).collect(Collectors.toList());

    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Integer id) {
        assertKnowledgeBaseOwner(id);
        // 先删关联的rag文件，再删知识库本身，用事务保证一致性
        ragFileMapper.delete(new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, id));
        knowledgeBaseMapper.deleteById(id);
        List<RagFile> ragFiles = ragFileMapper.selectList(new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, id));
        for (RagFile ragFile : ragFiles) {
            fileService.deleteObject(ragFile.getR2Key());
        }
        qdrantManager.deleteByKbId(id);

    }

    @Override
    public List<RagFileVO> uploadRagFile(RagFileDTO ragFileDTO) {
        assertKnowledgeBaseOwner(ragFileDTO.getKnowledgeBaseId());

        List<MultipartFile> multipartFiles = ragFileDTO.getFiles();
        List<RagFileVO> ragFileVOList = new ArrayList<>();



        if (multipartFiles == null) {
            return ragFileVOList;
        }
        for (MultipartFile file : multipartFiles) {
            //      MYSQL存储
            try {

                RagFile ragFile = new RagFile();
                ragFile.setKnowledgeBaseId(ragFileDTO.getKnowledgeBaseId());
                ragFile.setFileType(simplifyContentType(file.getContentType()));
                // 防止文件名含../穿越路径
                String originalFilename = file.getOriginalFilename();
                String safeFilename = originalFilename != null
                        ? originalFilename.replaceAll("[/\\\\]", "_")
                        : "unnamed";
                ragFile.setTitle(safeFilename);
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("READY");
                ragFile.setR2Key("knowledge_base/" + ragFileDTO.getKnowledgeBaseId() + "/" + safeFilename);
                // 同KB内SHA-256去重，相同内容不重复上传
                String hash = sha256(file);
                RagFile hashexist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getHash, hash)
                        .eq(RagFile::getKnowledgeBaseId, ragFileDTO.getKnowledgeBaseId()));
                // 同KB内标题检测版本覆盖
                RagFile titleexist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getTitle, originalFilename)
                        .eq(RagFile::getKnowledgeBaseId, ragFileDTO.getKnowledgeBaseId()));
                if (hashexist != null) {
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(hashexist, ragFileVO);
                    ragFileVOList.add(ragFileVO);
                    continue;
                }
                // 相同标题视为同文件新版本，版本号+0.01
                Double version = 0.01;
                if (titleexist != null) {
                    version = titleexist.getVersion() + 0.01;
                }
                ragFile.setVersion(version);
                ragFile.setHash(hash);

                // MySQL insert先获取自增id
                ragFileMapper.insert(ragFile);

                // 先上传R2原始文件（避免PDF解析消耗stream后上传失败）
                log.info("开始上传R2: {}", safeFilename);
                fileService.uploadFile(ragFile.getR2Key(), file);
                log.info("R2上传完成: {}", safeFilename);

                // 再解析文件并写入Qdrant向量库
                FileDocumentReader reader = documentReaderFactory.getReader(file.getContentType());
                List<Document> documents = reader.read(file);
                List<Document> documentList = tokenTextSplitter.apply(documents);
                for (Document document : documentList) {
                    document.getMetadata().put("document_id", ragFile.getId());
                    document.getMetadata().put("kb_id", ragFileDTO.getKnowledgeBaseId());
                    document.getMetadata().put("version", version);
                }
                batchAddToVectorStore(documentList);

                RagFileVO ragFileVO = new RagFileVO();
                BeanUtils.copyProperties(ragFile, ragFileVO);
                ragFileVOList.add(ragFileVO);

            } catch (Exception e){
                log.error("上传文件失败: {}", e.getMessage(), e);
            }
        }

        return ragFileVOList;


    }

    @Override
    public List<RagFileVO> uploadRagArticle(RagFileDTO ragFileDTO) {
        assertKnowledgeBaseOwner(ragFileDTO.getKnowledgeBaseId());
        List<Integer> articleIds = ragFileDTO.getArticleIds();
        List<RagFileVO> ragFileVOList = new ArrayList<>();
        for  (Integer articleId : articleIds) {
            try {
                Article article = articleMapper.getArticleById(articleId);
                if (article == null) {
                    log.error("文章 {} 不存在", articleId);
                    continue;
                }
                String content = article.getContent();
                RagFile ragFile = new RagFile();
                ragFile.setKnowledgeBaseId(ragFileDTO.getKnowledgeBaseId());
                ragFile.setFileType("md");
                ragFile.setTitle(article.getTitle());
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("READY");
                String safeTitle = article.getTitle() != null
                        ? article.getTitle().replaceAll("[/\\\\]", "_")
                        : "unnamed";
                ragFile.setR2Key("knowledge_base/" + ragFileDTO.getKnowledgeBaseId() + "/" + safeTitle + ".md");
                String hash = sha256(content);
                ragFile.setHash(hash);
                // 同KB内去重，不同KB可存同一篇文章
                RagFile hashExist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getHash, hash)
                        .eq(RagFile::getKnowledgeBaseId, ragFileDTO.getKnowledgeBaseId()));
                if(hashExist != null){
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(hashExist, ragFileVO);
                    ragFileVOList.add(ragFileVO);
                    continue;
                }
                ragFile.setVersion(article.getVersion());
                ragFileMapper.insert(ragFile);

                List<Document> text = tokenTextSplitter.apply(List.of(new Document(content)));
                for (Document document : text) {
                    document.getMetadata().put("document_id", ragFile.getId());
                    document.getMetadata().put("kb_id", ragFileDTO.getKnowledgeBaseId());
                    document.getMetadata().put("version", article.getVersion());
                }
                batchAddToVectorStore(text);

                fileService.uploadMarkdown(ragFile.getR2Key(), content);

            } catch (Exception e){
                log.error("上传文章失败: {}", e.getMessage(), e);
            }
        }
        return ragFileVOList;
    }

    @Override
    public String resolveSystemPrompt(Integer agentId) {
        List<AgentVO> agents = getAgents();
        String prompt = "你是一个有帮助的AI助手";
        if (agentId != null) {
            for (AgentVO agent : agents) {
                if (Objects.equals(agent.getId(), agentId)) {
                    prompt = agent.getSystemPrompt();
                    break;
                }
            }
        }
        return prompt;
    }

    @Override
    public void saveUserMessage(String content, Integer conversationId) {
        AIMessage aiMessage = new AIMessage();
        aiMessage.setRole("user");
        aiMessage.setContent(content);
        aiMessage.setConversationId(conversationId);
        aiMessage.setGmtCreate(LocalDateTime.now());
        saveMessage(aiMessage);
    }

    @Override
    public String queryKnowledgeBase(String knowledgeBaseIds, String message) {
        String filter = Arrays.stream(knowledgeBaseIds.split(","))
                .map(id -> "kb_id == " + id)
                .collect(Collectors.joining(" || "));
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message).topK(2).filterExpression(filter).similarityThreshold(0.01).build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        String context = documents.stream().map(Document::getText).collect(Collectors.joining("\n"));
        return context;
    }

    @Override
    public List<RagFileVO> getRagFiles(Integer id) {
        Integer userId = UserContext.getUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getId, id));
        if (!userId.equals(knowledgeBase.getUserId())) {
            throw new NoAuthorization(RespondCode.UNAUTHORIZED);
        }
        return ragFileMapper.selectList(new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, id)).stream().map(ragFile -> {
            RagFileVO ragFileVO = new RagFileVO();
            BeanUtils.copyProperties(ragFile, ragFileVO);
            return ragFileVO;
        }).collect(Collectors.toList());
    }
}
