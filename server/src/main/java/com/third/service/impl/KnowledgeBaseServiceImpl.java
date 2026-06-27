package com.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.mapper.*;
import com.third.pojo.dto.KnowledgeBaseDTO;
import com.third.pojo.dto.RagFileDTO;
import com.third.pojo.entity.*;
import com.third.pojo.vo.KnowledgeBaseVO;
import com.third.pojo.vo.RagFileVO;
import com.third.service.FileService;
import com.third.service.KnowledgeBaseService;
import com.third.service.QdrantManager;
import com.third.service.RagJobDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
    private static final int MAX_OBJECT_NAME_LENGTH = 120;
    private static final long RAG_READY_TIMEOUT_MS = 30_000L;
    private static final long RAG_READY_POLL_INTERVAL_MS = 500L;

    @Autowired
    private FileService fileService;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private QdrantManager qdrantManager;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Autowired
    private RagFileMapper ragFileMapper;
    @Autowired
    private RagJobDispatcher ragJobDispatcher;

    @Value("${app.rag.search.top-k:6}")
    private int searchTopK;

    @Value("${app.rag.search.document-top-k:30}")
    private int documentSearchTopK;

    @Value("${app.rag.search.similarity-threshold:0.2}")
    private double similarityThreshold;

    @Value("${app.rag.search.document-similarity-threshold:0.0}")
    private double documentSimilarityThreshold;


    /**
     * 断言知识库所有者
     *
     * @param knowledgeBaseId 知识库id
     * @param userId          用户ID
     */
    private void assertKnowledgeBaseOwner(Integer knowledgeBaseId, Integer userId) {
        Long count = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId));
        if (count == null || count == 0) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
    }

    /**
     * 简化内容类型
     *
     * @param contentType 内容类型
     * @return {@link String }
     */
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

    private static String sha256(MultipartFile file) {
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

    private static String sha256(String content) {
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

    private static String safeObjectName(String name, String fallback) {
        String safeName = (name == null || name.isBlank()) ? fallback : name.trim();
        safeName = safeName.replaceAll("[/\\\\]", "_")
                .replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_");
        while (safeName.contains("..")) {
            safeName = safeName.replace("..", "_");
        }
        safeName = safeName.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");
        if (safeName.isBlank()) {
            safeName = fallback;
        }
        if (safeName.length() > MAX_OBJECT_NAME_LENGTH) {
            safeName = safeName.substring(0, MAX_OBJECT_NAME_LENGTH);
        }
        return safeName;
    }

    private void deleteStoredObjectIfValid(String objectKey) {
        try {
            FileService.validateObjectKey(objectKey, "knowledge_base/");
        } catch (IllegalArgumentException e) {
            log.warn("skip deleting invalid knowledge base object key");
            return;
        }
        fileService.deleteObject(objectKey);
    }

    /**
     * 添加知识库
     *
     * @param knowledgeBaseDTO 知识库dto
     * @param userId           用户ID
     * @return {@link KnowledgeBaseVO }
     */
    @Override
    public KnowledgeBaseVO addKnowledgeBase(KnowledgeBaseDTO knowledgeBaseDTO, Integer userId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(knowledgeBaseDTO, knowledgeBase);
        knowledgeBase.setGmtCreate(LocalDateTime.now());
        knowledgeBase.setUserId(userId);
        knowledgeBaseMapper.insert(knowledgeBase);
        KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, knowledgeBaseVO);
        return knowledgeBaseVO;
    }

    /**
     * 获取知识库
     *
     * @param userId 用户ID
     * @return {@link List }<{@link KnowledgeBaseVO }>
     */
    @Override
    public List<KnowledgeBaseVO> getKnowledgeBase(Integer userId) {
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

    /**
     * 删除知识库
     *
     * @param id     ID
     * @param userId 用户ID
     */
    @Override
    @Transactional
    @CacheEvict(value = "kb:file:list", key = "#userId + ':' + #id")
    public void deleteKnowledgeBase(Integer id, Integer userId) {
        assertKnowledgeBaseOwner(id, userId);

        List<RagFile> ragFiles = ragFileMapper.selectList(
                new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, id));

        // Do not commit a database deletion when vector cleanup failed.
        qdrantManager.deleteByKbId(id);
        ragFileMapper.delete(new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, id));
        knowledgeBaseMapper.deleteById(id);

        for (RagFile ragFile : ragFiles) {
            deleteStoredObjectIfValid(ragFile.getR2Key());
        }
    }

    /**
     * 上传rag文件
     *
     * @param ragFileDTO rag文件dto
     * @param userId     用户ID
     * @return {@link List }<{@link RagFileVO }>
     */
    @Override
    @CacheEvict(value = "kb:file:list", key = "#userId + ':' + #ragFileDTO.knowledgeBaseId")
    public List<RagFileVO> uploadRagFile(RagFileDTO ragFileDTO, Integer userId) {
        Integer knowledgeBaseId = ragFileDTO.getKnowledgeBaseId();
        assertKnowledgeBaseOwner(knowledgeBaseId, userId);

        List<MultipartFile> multipartFiles = ragFileDTO.getFiles();
        List<RagFileVO> ragFileVOList = new ArrayList<>();

        if (multipartFiles == null) {
            return ragFileVOList;
        }
        for (MultipartFile file : multipartFiles) {
            RagFile ragFile = new RagFile();
            try {
                ragFile.setKnowledgeBaseId(knowledgeBaseId);
                ragFile.setFileType(simplifyContentType(file.getContentType()));
                String originalFilename = file.getOriginalFilename();
                String safeFilename = safeObjectName(originalFilename, "unnamed");
                ragFile.setTitle(safeFilename);
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("PROCESSING");
                ragFile.setR2Key("knowledge_base/" + knowledgeBaseId + "/" + safeFilename);
                byte[] content = file.getBytes();
                String hash = sha256(file);
                RagFile hashexist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getHash, hash)
                        .eq(RagFile::getKnowledgeBaseId, knowledgeBaseId));
                RagFile titleexist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getTitle, safeFilename)
                        .eq(RagFile::getKnowledgeBaseId, knowledgeBaseId));
                if (hashexist != null) {
                    if ("FAILED".equals(hashexist.getStatus())) {
                        hashexist.setTitle(safeFilename);
                        hashexist.setFileType(ragFile.getFileType());
                        hashexist.setR2Key("knowledge_base/" + knowledgeBaseId + "/" + safeFilename);
                        hashexist.setVersion(hashexist.getVersion() == null ? 0.01 : hashexist.getVersion());
                        hashexist.setStatus("PROCESSING");
                        fileService.uploadFile(hashexist.getR2Key(), content, file.getContentType());
                        ragFileMapper.updateById(hashexist);
                        ragJobDispatcher.dispatch(hashexist.getId());
                    }
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(hashexist, ragFileVO);
                    ragFileVOList.add(ragFileVO);
                    continue;
                }
                Double version = 0.01;
                if (titleexist != null) {
                    version = (titleexist.getVersion() == null ? 0.01 : titleexist.getVersion()) + 0.01;
                }
                ragFile.setVersion(version);
                ragFile.setHash(hash);

                ragFileMapper.insert(ragFile);

                log.debug("uploading rag object to storage");
                fileService.uploadFile(ragFile.getR2Key(), content, file.getContentType());
                log.debug("rag object upload completed");
                ragJobDispatcher.dispatch(ragFile.getId());

                RagFileVO ragFileVO = new RagFileVO();
                BeanUtils.copyProperties(ragFile, ragFileVO);
                ragFileVOList.add(ragFileVO);

            } catch (Exception e) {
                log.error("上传文件失败: {}", e.getMessage(), e);
                if (ragFile.getId() != null) {
                    ragFile.setStatus("FAILED");
                    ragFileMapper.updateById(ragFile);
                    RagFileVO failed = new RagFileVO();
                    BeanUtils.copyProperties(ragFile, failed);
                    ragFileVOList.add(failed);
                }
            }
        }

        return ragFileVOList;
    }

    /**
     * 上传rag文章
     *
     * @param ragFileDTO rag文件dto
     * @param userId     用户ID
     * @return {@link List }<{@link RagFileVO }>
     */
    @Override
    @CacheEvict(value = "kb:file:list", key = "#userId + ':' + #ragFileDTO.knowledgeBaseId")
    public List<RagFileVO> uploadRagArticle(RagFileDTO ragFileDTO, Integer userId) {
        Integer knowledgeBaseId = ragFileDTO.getKnowledgeBaseId();
        assertKnowledgeBaseOwner(knowledgeBaseId, userId);
        List<Integer> articleIds = ragFileDTO.getArticleIds();
        List<RagFileVO> ragFileVOList = new ArrayList<>();
        if (articleIds == null) {
            return ragFileVOList;
        }
        for (Integer articleId : articleIds) {
            RagFile ragFile = new RagFile();
            try {
                Article article = articleMapper.getArticleById(articleId);
                if (article == null) {
                    log.error("文章 {} 不存在", articleId);
                    continue;
                }
                if (!Objects.equals(userId, article.getWriterId())) {
                    throw new NoAuthorization(RespondCode.FORBIDDEN);
                }
                String content = article.getContent();
                ragFile.setKnowledgeBaseId(knowledgeBaseId);
                ragFile.setFileType("md");
                ragFile.setTitle(article.getTitle());
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("PROCESSING");
                String safeTitle = safeObjectName(article.getTitle(), "article-" + article.getId());
                ragFile.setR2Key("knowledge_base/" + knowledgeBaseId + "/" + safeTitle + ".md");
                String hash = sha256(content);
                ragFile.setHash(hash);
                RagFile hashExist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getHash, hash)
                        .eq(RagFile::getKnowledgeBaseId, knowledgeBaseId));
                if (hashExist != null) {
                    if ("FAILED".equals(hashExist.getStatus())) {
                        hashExist.setTitle(article.getTitle());
                        hashExist.setFileType("md");
                        hashExist.setR2Key("knowledge_base/" + knowledgeBaseId + "/" + safeTitle + ".md");
                        hashExist.setVersion(article.getVersion());
                        hashExist.setStatus("PROCESSING");
                        fileService.uploadMarkdown(hashExist.getR2Key(), content);
                        ragFileMapper.updateById(hashExist);
                        ragJobDispatcher.dispatch(hashExist.getId());
                    }
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(hashExist, ragFileVO);
                    ragFileVOList.add(ragFileVO);
                    continue;
                }
                ragFile.setVersion(article.getVersion());
                ragFileMapper.insert(ragFile);

                fileService.uploadMarkdown(ragFile.getR2Key(), content);
                ragJobDispatcher.dispatch(ragFile.getId());

                RagFileVO ragFileVO = new RagFileVO();
                BeanUtils.copyProperties(ragFile, ragFileVO);
                ragFileVOList.add(ragFileVO);

            } catch (NoAuthorization e) {
                throw e;
            } catch (Exception e) {
                log.error("上传文章失败: {}", e.getMessage(), e);
                if (ragFile.getId() != null) {
                    ragFile.setStatus("FAILED");
                    ragFileMapper.updateById(ragFile);
                    RagFileVO failed = new RagFileVO();
                    BeanUtils.copyProperties(ragFile, failed);
                    ragFileVOList.add(failed);
                }
            }
        }
        return ragFileVOList;
    }

    /**
     * 查询知识库
     *
     * @param knowledgeBaseIds 知识库id
     * @param message          消息
     * @return {@link String }
     */
    @Override
    public String queryKnowledgeBase(String knowledgeBaseIds, String documentIds, String message, Integer userId) {
        List<Integer> ids = parseIds(knowledgeBaseIds);
        List<Integer> docIds = parseIds(documentIds);
        if (ids.isEmpty() && docIds.isEmpty()) {
            return "";
        }
        ids.forEach(id -> assertKnowledgeBaseOwner(id, userId));
        if (!docIds.isEmpty()) {
            assertRagFilesOwner(docIds, userId);
        }
        String filter = !docIds.isEmpty()
                ? docIds.stream().map(id -> "document_id == " + id).collect(Collectors.joining(" || "))
                : ids.stream().map(id -> "kb_id == " + id).collect(Collectors.joining(" || "));
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(!docIds.isEmpty() ? documentSearchTopK : searchTopK)
                .filterExpression(filter)
                .similarityThreshold(!docIds.isEmpty() ? documentSimilarityThreshold : similarityThreshold)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents.stream().map(Document::getText).collect(Collectors.joining("\n"));
    }

    public String queryKnowledgeBase(String knowledgeBaseIds, String message, Integer userId) {
        return queryKnowledgeBase(knowledgeBaseIds, null, message, userId);
    }

    @Override
    public void waitForRagFilesReady(String documentIds, Integer userId) {
        List<Integer> ids = parseIds(documentIds);
        if (ids.isEmpty()) {
            return;
        }
        assertRagFilesOwner(ids, userId);
        long deadline = System.currentTimeMillis() + RAG_READY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            List<RagFile> files = selectRagFiles(ids);
            boolean allReady = true;
            for (RagFile file : files) {
                if ("FAILED".equals(file.getStatus())) {
                    throw new IllegalStateException("文档解析失败：" + file.getTitle());
                }
                if (!"READY".equals(file.getStatus())) {
                    allReady = false;
                    break;
                }
            }
            if (allReady) {
                return;
            }
            try {
                Thread.sleep(RAG_READY_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待文档解析被中断", e);
            }
        }
        throw new IllegalStateException("文档还在解析中，请稍后再试");
    }

    private List<Integer> parseIds(String rawIds) {
        if (rawIds == null || rawIds.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.stream(rawIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(Integer::valueOf)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw new NoAuthorization(RespondCode.PARAM_ERROR);
        }
    }

    private void assertRagFilesOwner(List<Integer> documentIds, Integer userId) {
        List<RagFile> files = selectRagFiles(documentIds);
        if (files.size() != documentIds.size()) {
            throw new NoAuthorization(RespondCode.NOT_FOUND);
        }
        files.forEach(file -> assertKnowledgeBaseOwner(file.getKnowledgeBaseId(), userId));
    }

    private List<RagFile> selectRagFiles(List<Integer> documentIds) {
        if (documentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return ragFileMapper.selectList(new LambdaQueryWrapper<RagFile>().in(RagFile::getId, documentIds));
    }

    /**
     * 获取rag文件
     *
     * @param id     ID
     * @param userId 用户ID
     * @return {@link List }<{@link RagFileVO }>
     */
    @Override
    @Cacheable(value = "kb:file:list", key = "#userId + ':' + #id")
    public List<RagFileVO> getRagFiles(Integer id, Integer userId) {
        assertKnowledgeBaseOwner(id, userId);
        return ragFileMapper.selectList(
                new LambdaQueryWrapper<RagFile>().eq(RagFile::getKnowledgeBaseId, id))
                .stream().map(ragFile -> {
                    RagFileVO ragFileVO = new RagFileVO();
                    BeanUtils.copyProperties(ragFile, ragFileVO);
                    return ragFileVO;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "kb:file:list", key = "#userId + ':' + #knowledgeBaseId")
    public void deleteRagFile(Integer knowledgeBaseId, Integer documentId, Integer userId) {
        assertKnowledgeBaseOwner(knowledgeBaseId, userId);
        RagFile ragFile = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                .eq(RagFile::getId, documentId)
                .eq(RagFile::getKnowledgeBaseId, knowledgeBaseId));
        if (ragFile == null) {
            throw new NoAuthorization(RespondCode.NOT_FOUND);
        }
        qdrantManager.deleteByDocumentId(documentId);
        ragFileMapper.deleteById(documentId);
        deleteStoredObjectIfValid(ragFile.getR2Key());
    }

    @Override
    @CacheEvict(value = "kb:file:list", key = "#userId + ':' + #knowledgeBaseId")
    public void retryRagFile(Integer knowledgeBaseId, Integer documentId, Integer userId) {
        assertKnowledgeBaseOwner(knowledgeBaseId, userId);
        RagFile ragFile = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                .eq(RagFile::getId, documentId)
                .eq(RagFile::getKnowledgeBaseId, knowledgeBaseId));
        if (ragFile == null) {
            throw new NoAuthorization(RespondCode.NOT_FOUND);
        }
        FileService.validateObjectKey(ragFile.getR2Key(), "knowledge_base/");
        ragFile.setStatus("PROCESSING");
        ragFileMapper.updateById(ragFile);
        ragJobDispatcher.dispatch(documentId);
    }
}
