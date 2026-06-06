package com.third.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.third.common.context.UserContext;
import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.common.exception.NoSuchArticle;
import com.third.mapper.ArticleVersionMapper;
import com.third.pojo.dto.ArticleAddDTO;
import com.third.pojo.entity.Article;
import com.third.mapper.ArticleMapper;
import com.third.pojo.entity.ArticleVersion;
import com.third.pojo.entity.Tag;
import com.third.pojo.vo.ArticleListVO;
import com.third.pojo.vo.ArticleVO;
import com.third.pojo.vo.ArticleVersionVO;
import com.third.service.ArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.third.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.endpoints.internal.Substring;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    ArticleVersionMapper articleVersionMapper;

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

    public void simpleAddVersion(Article article, List<String> tags) {
        ArticleVersion articleVersion = new ArticleVersion();
        articleVersion.setContent(article.getContent());
        articleVersion.setTitle(article.getTitle());
        articleVersion.setVersion(article.getVersion());
        articleVersion.setTag(JSON.toJSONString(tags));
        articleVersion.setArticleId(article.getId());
        articleVersion.setGmtCreate(LocalDate.now());
        log.info("updating aritcle: {}", articleVersion);
        articleVersionMapper.addArticleVersion(articleVersion);

    }

    @Override
    @Transactional
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
        simpleAddVersion(article, articleDTO.getTag());
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

        LambdaQueryWrapper<ArticleVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleVersion::getArticleId, id);
        articleVersionMapper.delete(queryWrapper);
    }

    @Override
    @Transactional
    public void updateArticleById(Integer id, ArticleAddDTO articleAddDTO) {
        Integer userId = UserContext.getUserId();
        Article article = articleMapper.getArticleById(id);
        if (!userId.equals(article.getWriterId()))
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        // 先归档当前版本
        List<String> oldTags = articleMapper.getTagsByArticleId(article.getId());
        simpleAddVersion(article, oldTags);
        // 更新 article 字段
        BeanUtils.copyProperties(articleAddDTO, article);
        if (articleAddDTO.getVersion() == null)
            article.setVersion(article.getVersion() + 0.1d);
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

    @Override
    public List<ArticleVersionVO> getVersions(Integer id) {
        Article article = articleMapper.getArticleById(id);
        Integer userId = UserContext.getUserId();
        if (userId != article.getWriterId())
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        LambdaQueryWrapper<ArticleVersion> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ArticleVersion::getArticleId, id)
                .orderByDesc(ArticleVersion::getVersion);
        return articleVersionMapper.selectList(lambdaQueryWrapper).stream().map(articleVersion -> {
            ArticleVersionVO articleVersionVO = new ArticleVersionVO();
            BeanUtils.copyProperties(articleVersion, articleVersionVO);
            return articleVersionVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ArticleVersionVO rollbackVersion(Integer id, Integer versionId) {
        Article article = articleMapper.getArticleById(id);
        Integer userId = UserContext.getUserId();
        if (!userId.equals(article.getWriterId()))
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        // 获取目标版本
        ArticleVersionVO articleVersion = getVersion(id, versionId);
        // 直接用目标版本内容覆盖 article，回到目标版本号
        article.setTitle(articleVersion.getTitle());
        article.setContent(articleVersion.getContent());
        article.setVersion(articleVersion.getVersion());
        article.setGmtModified(LocalDateTime.now());
        articleMapper.updateArticle(article);
        // 重建 tag 关联
        articleMapper.deleteArticleTagById(id);
        List<String> rollbackTags = JSON.parseArray(articleVersion.getTag(), String.class);
        simpleAddTags(id, rollbackTags);
        return articleVersion;
    }

    @Override
    public ArticleVersionVO getVersion(Integer id, Integer versionId) {
        LambdaQueryWrapper<ArticleVersion> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ArticleVersion::getArticleId, id)
                .eq(ArticleVersion::getId, versionId);
        ArticleVersion articleVersion = articleVersionMapper.selectOne(lambdaQueryWrapper);
        if (articleVersion == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        ArticleVersionVO articleVersionVO = new ArticleVersionVO();
        BeanUtils.copyProperties(articleVersion, articleVersionVO);
        return articleVersionVO;
    }
}
