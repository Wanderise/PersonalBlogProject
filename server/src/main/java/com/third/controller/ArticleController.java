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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/article")
@Tag(name = "Article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @PostMapping("/add")
    @Operation(summary = "Add article")
    public Result<ArticleVO> addArticle(@RequestBody ArticleAddDTO article) {
        Integer userId = UserContext.getUserId();
        ArticleVO articleVO = articleService.addArticle(article, userId);
        return Result.success(articleVO);
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> getArticle(@PathVariable("id") Integer id) {
        ArticleVO articleVO = articleService.getArticleById(id);
        return Result.success(articleVO);
    }

    @GetMapping("/list")
    public Result<ArticleListVO> getArticleList(int page, int size, String tag, String keyword) {
        ArticleListVO articleListVO = articleService.getArticleList(page, size, tag, keyword);
        return Result.success(articleListVO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable("id") Integer id) {
        Integer userId = UserContext.getUserId();
        articleService.deleteArticle(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateArticle(@PathVariable("id") Integer id,
                                      @RequestBody ArticleAddDTO articleAddDTO) {
        Integer userId = UserContext.getUserId();
        articleService.updateArticleById(id, articleAddDTO, userId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<ArticleListVO> getMyArticle(int page, int size) {
        Integer userId = UserContext.getUserId();
        ArticleListVO articleListVO = articleService.getMyArticleList(page, size, userId);
        return Result.success(articleListVO);
    }

    @GetMapping("/{id}/versions")
    public Result<List<ArticleVersionVO>> getVersions(@PathVariable("id") Integer id) {
        Integer userId = UserContext.getUserId();
        List<ArticleVersionVO> articleVersionVOS = articleService.getVersions(id, userId);
        return Result.success(articleVersionVOS);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public Result<ArticleVersionVO> getVersion(@PathVariable("id") Integer id,
                                               @PathVariable("versionId") Integer versionId) {
        Integer userId = UserContext.getUserId();
        ArticleVersionVO articleVersionVO = articleService.getVersion(id, versionId, userId);
        return Result.success(articleVersionVO);
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    public Result<ArticleVersionVO> rollbackVersion(@PathVariable("id") Integer id,
                                                    @PathVariable("versionId") Integer versionId) {
        Integer userId = UserContext.getUserId();
        ArticleVersionVO articleVersionVO = articleService.rollbackVersion(id, versionId, userId);
        return Result.success(articleVersionVO);
    }
}
