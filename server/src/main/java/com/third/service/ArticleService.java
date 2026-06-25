package com.third.service;

import com.third.pojo.dto.ArticleAddDTO;
import com.third.pojo.entity.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.third.pojo.vo.ArticleListVO;
import com.third.pojo.vo.ArticleVO;
import com.third.pojo.vo.ArticleVersionVO;

import java.util.List;

public interface ArticleService extends IService<Article> {

    ArticleVO addArticle(ArticleAddDTO article, Integer userId);

    ArticleVO getArticleById(Integer id);

    ArticleListVO getArticleList(int page, int size, String tag, String keyword);

    void deleteArticle(Integer id, Integer userId);

    void updateArticleById(Integer id, ArticleAddDTO articleAddDTO, Integer userId);

    ArticleListVO getMyArticleList(int page, int size, Integer userId);

    List<ArticleVersionVO> getVersions(Integer id, Integer userId);

    ArticleVersionVO rollbackVersion(Integer id, Integer versionId, Integer userId);

    ArticleVersionVO getVersion(Integer id, Integer versionId, Integer userId);
}
