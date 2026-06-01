package com.third.controller;

import com.third.common.result.Result;
import com.third.pojo.dto.ArticleAddDTO;
import com.third.pojo.vo.ArticleListVO;
import com.third.pojo.vo.ArticleVO;
import com.third.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 文章 前端控制器
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
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
        log.info("接收到添加请求" + article);
        ArticleVO articleVO = articleService.addArticle(article);
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
        log.info("删除文章:" + id);
        articleService.deleteArticle(id);
        return Result.success();
    }

    @PutMapping("{id}")
    public Result updateArticle(@PathVariable("id") Integer id, @RequestBody ArticleAddDTO articleAddDTO) {
        log.info("更新文章:" + id);
        articleService.updateArticleById(id, articleAddDTO);
        return Result.success();
    }

}
