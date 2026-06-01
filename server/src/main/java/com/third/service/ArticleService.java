package com.third.service;

import com.third.pojo.dto.ArticleAddDTO;
import com.third.pojo.entity.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.third.pojo.vo.ArticleListVO;
import com.third.pojo.vo.ArticleVO;

import java.util.List;

/**
 * <p>
 * 文章 服务类
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
public interface ArticleService extends IService<Article> {

    ArticleVO addArticle(ArticleAddDTO article);

    ArticleVO getArticleById(Integer id);

    ArticleListVO getArticleList(int page, int size, String tag, String keyword);

    void deleteArticle(Integer id);
}
