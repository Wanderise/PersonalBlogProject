package com.third.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.third.common.context.UserContext;
import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.pojo.dto.ArticleAddDTO;
import com.third.pojo.entity.Article;
import com.third.mapper.ArticleMapper;
import com.third.pojo.entity.Tag;
import com.third.pojo.vo.ArticleListVO;
import com.third.pojo.vo.ArticleVO;
import com.third.service.ArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.third.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.endpoints.internal.Substring;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 文章 服务实现类
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    ArticleMapper articleMapper;

    @Autowired
    FileService fileService;

    private ArticleVO articleToVO(Article article) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        articleVO.setTag(articleMapper.getTagsByArticleId(article.getId()));
        if (article.getImage() != null && !article.getImage().isEmpty()) {
            List<String> images = JSON.parseArray(article.getImage(), String.class);
            List<String> imageUrls = new ArrayList<>();
            for (String image : images) {
                imageUrls.add(fileService.getDownloadPresignedUrl(image));
            }
            articleVO.setImageUrls(imageUrls);
            articleVO.setImage(images);
        }
        String summary = article.getContent().length() >= 200 ? article.getContent().substring(0, 201) : article.getContent();
        articleVO.setSummary(summary);
        return articleVO;
    }
//    添加关联表和Tag表
    private void simpleAddTags(Integer ArticleId, List<String> tags){
        for (String tag : tags.stream().distinct().toList()) {
            Tag tagByName = articleMapper.getTagByName(tag);
            if (tagByName == null) {
                tagByName = new Tag();
                tagByName.setName(tag);
                tagByName.setGmtCreate(LocalDateTime.now());
                articleMapper.addTag(tagByName);
            }
            articleMapper.addArticleTag(ArticleId, tagByName.getId());
        }
    }

    private void simpleDeleteTags(Integer ArticleId, List<String> tags){
        articleMapper.deleteArticleTagById(ArticleId);

        for (String tag : tags) {
            Tag tagByName = articleMapper.getTagByName(tag);
            Integer tagId = tagByName.getId();
            int articleCount = articleMapper.getArticleCountByTagId(tagId);
            if(articleCount == 0) {
                articleMapper.deleteTagById(tagId);
            }
        }
    }

    @Override
    public ArticleVO addArticle(ArticleAddDTO articleDTO) {
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        article.setImage(JSON.toJSONString(articleDTO.getImage()));
        article.setGmtCreate(LocalDateTime.now());
        article.setGmtModified(LocalDateTime.now());
        Integer userId = UserContext.getUserId();
        article.setWriterId(userId);
        log.info("article:{}", article);

        articleMapper.addArticle(article);
        Integer articleId = article.getId();
        simpleAddTags(articleId, articleDTO.getTag());

        ArticleVO articleVO = new ArticleVO();
        articleVO.setId(articleId);
        return articleVO;
    }

    @Override
    public ArticleVO getArticleById(Integer id) {
        Article article = articleMapper.getArticleById(id);
        ArticleVO articleVO = articleToVO(article);
        log.info("articleVO:{}", articleVO);
        return articleVO;
    }

    @Override
    public ArticleListVO getArticleList(int page, int size, String tag, String keyword) {
        ArticleListVO articleListVO = new ArticleListVO();
        PageHelper.startPage(page, size);
        List<Article> Articles = articleMapper.getArticleList(tag, keyword);
        PageInfo<Article> pageInfo = new PageInfo<>(Articles);
        log.info("Articles:{}", Articles);
        articleListVO.setPage(pageInfo.getPageNum());
        articleListVO.setTotal(pageInfo.getPages());
        articleListVO.setSize(pageInfo.getSize());
        List<ArticleVO> articleVOList = new ArrayList<>();
        for (Article article : Articles) {
            ArticleVO articleVO = articleToVO(article);
            articleVOList.add(articleVO);
        }
        articleListVO.setArticles(articleVOList);
        log.info("articleListVO:{}", articleListVO);
        return articleListVO;


    }

    @Override
    @Transactional
    public void deleteArticle(Integer id) {
        Integer userId = UserContext.getUserId();
        ArticleVO article = getArticleById(id);
        if (userId != article.getWriterId()) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
        articleMapper.deleteArticleById(id);
        List<String> tags = article.getTag();
        simpleDeleteTags(id, tags);

        if(article.getImage() != null) {
            List<String> imageUrls = article.getImage();
            for (String imageUrl : imageUrls) {
                fileService.deleteObject(imageUrl);
            }
        }
    }

    @Override
    @Transactional
    public void updateArticleById(Integer id, ArticleAddDTO articleAddDTO) {
        Integer userId = UserContext.getUserId();
        Article article = articleMapper.getArticleById(id);
        if (userId != article.getWriterId()) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
        BeanUtils.copyProperties(articleAddDTO, article);
        article.setImage(JSON.toJSONString(articleAddDTO.getImage()));
        article.setGmtModified(LocalDateTime.now());
        log.info("updating_article:{}", article);
        articleMapper.updateArticle(article);
        articleMapper.deleteArticleTagById(id);
        List<String> tags = articleAddDTO.getTag();
        simpleAddTags(id, tags);


    }

    @Override
    public ArticleListVO getMyArticleList(int page, int size) {
        Integer userId = UserContext.getUserId();
        PageHelper.startPage(page, size);
        List<Article> articles = articleMapper.getMyArticleList(userId);
        PageInfo<Article> pageInfo = new PageInfo<>(articles);
        log.info("articles:{}", articles);
        ArticleListVO articleListVO = new ArticleListVO();
        articleListVO.setPage(pageInfo.getPageNum());
        articleListVO.setTotal(pageInfo.getPages());
        articleListVO.setSize(pageInfo.getPages());
        List<ArticleVO> articleVOList = new ArrayList<>();
        for (Article article : articles) {
            ArticleVO articleVO = articleToVO(article);
            articleVOList.add(articleVO);
        }
        articleListVO.setArticles(articleVOList);
        log.info("articleListVO:{}", articleListVO);
        return articleListVO;
    }
}
