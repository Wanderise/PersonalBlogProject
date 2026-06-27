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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

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

    @Value("${app.rag.search.similarity-threshold:0.2}")
    private double similarityThreshold;


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
            fileService.deleteObject(ragFile.getR2Key());
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
                String safeFilename = originalFilename != null
                        ? originalFilename.replaceAll("[/\\\\]", "_")
                        : "unnamed";
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
                        hashexist.setStatus("PROCESSING");
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
                if (!userId.equals(article.getWriterId())) {
                    throw new NoAuthorization(RespondCode.FORBIDDEN);
                }
                String content = article.getContent();
                ragFile.setKnowledgeBaseId(knowledgeBaseId);
                ragFile.setFileType("md");
                ragFile.setTitle(article.getTitle());
                ragFile.setGmtCreate(LocalDate.now());
                ragFile.setStatus("PROCESSING");
                String safeTitle = article.getTitle() != null
                        ? article.getTitle().replaceAll("[/\\\\]", "_")
                        : "unnamed";
                ragFile.setR2Key("knowledge_base/" + knowledgeBaseId + "/" + safeTitle + ".md");
                String hash = sha256(content);
                ragFile.setHash(hash);
                RagFile hashExist = ragFileMapper.selectOne(new LambdaQueryWrapper<RagFile>()
                        .eq(RagFile::getHash, hash)
                        .eq(RagFile::getKnowledgeBaseId, knowledgeBaseId));
                if (hashExist != null) {
                    if ("FAILED".equals(hashExist.getStatus())) {
                        hashExist.setStatus("PROCESSING");
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
    public String queryKnowledgeBase(String knowledgeBaseIds, String message, Integer userId) {
        List<Integer> ids;
        try {
            ids = Arrays.stream(knowledgeBaseIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(Integer::valueOf)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw new NoAuthorization(RespondCode.PARAM_ERROR);
        }
        if (ids.isEmpty()) {
            return "";
        }
        ids.forEach(id -> assertKnowledgeBaseOwner(id, userId));
        String filter = ids.stream()
                .map(id -> "kb_id == " + id)
                .collect(Collectors.joining(" || "));
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(searchTopK)
                .filterExpression(filter)
                .similarityThreshold(similarityThreshold)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents.stream().map(Document::getText).collect(Collectors.joining("\n"));
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
        fileService.deleteObject(ragFile.getR2Key());
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
        ragFile.setStatus("PROCESSING");
        ragFileMapper.updateById(ragFile);
        ragJobDispatcher.dispatch(documentId);
    }
}
