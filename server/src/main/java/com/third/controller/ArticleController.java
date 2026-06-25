package com.third.controller;

import com.third.common.context.UserContext;
import com.third.common.result.Result;
import com.third.pojo.dto.ArticleAddDTO;
import com.third.pojo.vo.ArticleListVO;
import com.third.pojo.vo.ArticleVO;
import com.third.pojo.vo.ArticleVersionVO;
import com.third.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/article")
@Slf4j
@Tag(name = "文章管理")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @PostMapping("/add")
    @Operation(summary = "添加文章")
    public Result<ArticleVO> addArticle(@RequestBody ArticleAddDTO article) {
        Integer userId = UserContext.getUserId();
        log.info("接收到添加请求" + article);
        ArticleVO articleVO = articleService.addArticle(article, userId);
        return Result.success(articleVO);
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> getArticle(@PathVariable("id") Integer id) {
        log.info("获取文章:" + id);
        ArticleVO articleVO = articleService.getArticleById(id);
        return Result.success(articleVO);
    }

    @GetMapping("/list")
    public Result<ArticleListVO> getArticleList(int page, int size, String tag, String keyword) {
        log.info("page:" + page + " size:" + size + " tag:" + tag + " keyword:" + keyword);
        ArticleListVO articleListVO = articleService.getArticleList(page, size, tag, keyword);
        return Result.success(articleListVO);
    }

    @DeleteMapping("/{id}")
    public Result deleteArticle(@PathVariable("id") Integer id) {
        Integer userId = UserContext.getUserId();
        log.info("删除文章:" + id);
        articleService.deleteArticle(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result updateArticle(@PathVariable("id") Integer id, @RequestBody ArticleAddDTO articleAddDTO) {
        Integer userId = UserContext.getUserId();
        log.info("更新文章:" + id);
        articleService.updateArticleById(id, articleAddDTO, userId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<ArticleListVO> getMyArticle(int page, int size) {
        Integer userId = UserContext.getUserId();
        log.info("page:" + page + " size:" + size);
        ArticleListVO articleListVO = articleService.getMyArticleList(page, size, userId);
        return Result.success(articleListVO);
    }

    @GetMapping("/{id}/versions")
    public Result<List<ArticleVersionVO>> getVersions(@PathVariable("id") Integer id) {
        Integer userId = UserContext.getUserId();
        log.info("获取文章{}历史版本。", id);
        List<ArticleVersionVO> articleVersionVOS = articleService.getVersions(id, userId);
        return Result.success(articleVersionVOS);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public Result<ArticleVersionVO> getVersion(@PathVariable("id") Integer id,  @PathVariable("versionId") Integer versionId) {
        Integer userId = UserContext.getUserId();
        log.info("正在获取文章{}的{}版本", id, versionId);
        ArticleVersionVO articleVersionVO = articleService.getVersion(id, versionId, userId);
        return Result.success(articleVersionVO);
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    public Result<ArticleVersionVO> rollbackVersion(@PathVariable("id") Integer id, @PathVariable("versionId") Integer versionId) {
        Integer userId = UserContext.getUserId();
        log.info("正在回滚文章{}，{}版本", id, versionId);
        ArticleVersionVO articleVersionVO = articleService.rollbackVersion(id, versionId, userId);
        return Result.success(articleVersionVO);
    }

}
