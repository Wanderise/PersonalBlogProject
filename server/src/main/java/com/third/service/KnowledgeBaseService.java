package com.third.service;

import com.third.pojo.dto.KnowledgeBaseDTO;
import com.third.pojo.dto.RagFileDTO;
import com.third.pojo.vo.KnowledgeBaseVO;
import com.third.pojo.vo.RagFileVO;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeBaseVO addKnowledgeBase(KnowledgeBaseDTO knowledgeBaseDTO, Integer userId);

    List<KnowledgeBaseVO> getKnowledgeBase(Integer userId);

    void deleteKnowledgeBase(Integer id, Integer userId);

    List<RagFileVO> uploadRagFile(RagFileDTO ragFileDTO, Integer userId);

    List<RagFileVO> uploadRagArticle(RagFileDTO ragFileDTO, Integer userId);

    String queryKnowledgeBase(String knowledgeBaseIds, String message, Integer userId);

    List<RagFileVO> getRagFiles(Integer id, Integer userId);

    void deleteRagFile(Integer knowledgeBaseId, Integer documentId, Integer userId);

    void retryRagFile(Integer knowledgeBaseId, Integer documentId, Integer userId);
}
