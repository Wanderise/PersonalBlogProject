package com.third.controller;

import com.third.common.context.UserContext;
import com.third.common.result.Result;
import com.third.pojo.dto.KnowledgeBaseDTO;
import com.third.pojo.dto.RagFileDTO;
import com.third.pojo.vo.KnowledgeBaseVO;
import com.third.pojo.vo.RagFileVO;
import com.third.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RequestMapping("/ai")
@RestController
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/knowledge-bases")
    public Result<KnowledgeBaseVO> addKnowledgeBase(@RequestBody KnowledgeBaseDTO knowledgeBaseDTO) {
        Integer userId = UserContext.getUserId();
        log.info("addKnowledgeBase: {}", knowledgeBaseDTO);
        KnowledgeBaseVO knowledgeBaseVO = knowledgeBaseService.addKnowledgeBase(knowledgeBaseDTO, userId);
        return Result.success(knowledgeBaseVO);
    }

    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBaseVO>> getKnowledgeBases() {
        Integer userId = UserContext.getUserId();
        List<KnowledgeBaseVO> knowledgeBasesVO = knowledgeBaseService.getKnowledgeBase(userId);
        return Result.success(knowledgeBasesVO);
    }

    @DeleteMapping({"/knowledge-bases/{id}", "/knowledge-base/{id}"})
    public Result deleteKnowledgeBase(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        knowledgeBaseService.deleteKnowledgeBase(id, userId);
        return Result.success();
    }

    @GetMapping("/knowledge-bases/{id}/documents")
    public Result<List<RagFileVO>> getRagFiles(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        List<RagFileVO> ragFileVOList = knowledgeBaseService.getRagFiles(id, userId);
        return Result.success(ragFileVOList);
    }

    @DeleteMapping("/knowledge-bases/{knowledgeBaseId}/documents/{documentId}")
    public Result<Void> deleteRagFile(@PathVariable Integer knowledgeBaseId,
                                      @PathVariable Integer documentId) {
        knowledgeBaseService.deleteRagFile(knowledgeBaseId, documentId, UserContext.getUserId());
        return Result.success();
    }

    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/retry")
    public Result<Void> retryRagFile(@PathVariable Integer knowledgeBaseId,
                                     @PathVariable Integer documentId) {
        knowledgeBaseService.retryRagFile(knowledgeBaseId, documentId, UserContext.getUserId());
        return Result.success();
    }

    @PostMapping(value = "/rag/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<RagFileVO>> uploadRagFile(@RequestParam("files") List<MultipartFile> files,
                                                  @RequestParam("knowledgeBaseId") Integer knowledgeBaseId) {
        Integer userId = UserContext.getUserId();
        RagFileDTO ragFileDTO = new RagFileDTO();
        ragFileDTO.setFiles(files);
        ragFileDTO.setKnowledgeBaseId(knowledgeBaseId);
        log.info("uploadRagFile: {}", ragFileDTO);
        List<RagFileVO> ragFileVOList = knowledgeBaseService.uploadRagFile(ragFileDTO, userId);
        return Result.success(ragFileVOList);
    }

    @PostMapping("/rag/articles")
    public Result<List<RagFileVO>> uploadRagArticles(@RequestBody RagFileDTO ragFileDTO) {
        Integer userId = UserContext.getUserId();
        log.info("uploadRagArticles: {}", ragFileDTO);
        List<RagFileVO> ragFileVOList = knowledgeBaseService.uploadRagArticle(ragFileDTO, userId);
        return Result.success(ragFileVOList);
    }
}
