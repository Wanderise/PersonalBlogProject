package com.third.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    ArticleMapper articleMapper;

    @Autowired
    ArticleVersionMapper articleVersionMapper;

    @Autowired
    FileService fileService;

    /**
     * 文章到vo
     *
     * @param article 文章
     * @return {@link ArticleVO }
     */
    private ArticleVO articleToVO(Article article) {
        return articleToVO(article, true);
    }

    /**
     * 文章到vo
     *
     * @param article      文章
     * @param generateUrls 生成URL
     * @return {@link ArticleVO }
     */
    private ArticleVO articleToVO(Article article, boolean generateUrls) {
        if (article == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        articleVO.setTag(articleMapper.getTagsByArticleId(article.getId()));
        if (article.getImage() != null && !article.getImage().isEmpty()) {
            List<String> images = JSON.parseArray(article.getImage(), String.class);
            articleVO.setImage(images);
            if (generateUrls) {
                List<String> imageUrls = new ArrayList<>();
                for (String image : images) {
                    imageUrls.add(fileService.getDownloadPresignedUrl(image));
                }
                articleVO.setImageUrls(imageUrls);
            } else if (!images.isEmpty()) {
                List<String> coverOnly = new ArrayList<>();
                coverOnly.add(fileService.getDownloadPresignedUrl(images.get(0)));
                articleVO.setImageUrls(coverOnly);
            }
        }
        String content = article.getContent() == null ? "" : article.getContent();
        String summary = content.length() > 200 ? content.substring(0, 200) : content;
        articleVO.setSummary(summary);
        return articleVO;
    }

    /**
     * 构建文章列表vo
     *
     * @param pageInfo 页面信息
     * @return {@link ArticleListVO }
     */
    private ArticleListVO buildArticleListVO(PageInfo<Article> pageInfo) {
        ArticleListVO vo = new ArticleListVO();
        vo.setPage(pageInfo.getPageNum());
        vo.setTotal((int) pageInfo.getTotal());
        vo.setSize(pageInfo.getPageSize());
        vo.setArticles(pageInfo.getList().stream().map(a -> articleToVO(a, false)).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 简单添加标签
     *
     * @param ArticleId 文章ID
     * @param tags      标签
     */
    private void simpleAddTags(Integer ArticleId, List<String> tags){
        if (tags == null) {
            return;
        }
        for (String tag : tags.stream().distinct().toList()) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
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

    /**
     * 简单删除标签
     *
     * @param ArticleId 文章ID
     * @param tags      标签
     */
    private void simpleDeleteTags(Integer ArticleId, List<String> tags){
        articleMapper.deleteArticleTagById(ArticleId);
        if (tags == null) {
            return;
        }

        for (String tag : tags) {
            Tag tagByName = articleMapper.getTagByName(tag);
            if (tagByName == null) {
                continue;
            }
            Integer tagId = tagByName.getId();
            int articleCount = articleMapper.getArticleCountByTagId(tagId);
            if(articleCount == 0) {
                articleMapper.deleteTagById(tagId);
            }
        }
    }

    /**
     * 简单添加版本
     *
     * @param article 文章
     * @param tags    标签
     */
    public void simpleAddVersion(Article article, List<String> tags) {
        ArticleVersion articleVersion = new ArticleVersion();
        articleVersion.setContent(article.getContent());
        articleVersion.setTitle(article.getTitle());
        articleVersion.setVersion(article.getVersion());
        articleVersion.setTag(JSON.toJSONString(tags));
        articleVersion.setArticleId(article.getId());
        articleVersion.setGmtCreate(LocalDate.now());
        articleVersionMapper.addArticleVersion(articleVersion);

    }

    /**
     * 添加文章
     *
     * @param articleDTO 文章dto
     * @param userId     用户ID
     * @return {@link ArticleVO }
     */
    @Override
    @Transactional
    public ArticleVO addArticle(ArticleAddDTO articleDTO, Integer userId) {
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        if (article.getVersion() == null) {
            article.setVersion(1.0);
        }
        article.setImage(JSON.toJSONString(articleDTO.getImage()));
        article.setGmtCreate(LocalDateTime.now());
        article.setGmtModified(LocalDateTime.now());
        article.setWriterId(userId);

        articleMapper.addArticle(article);
        Integer articleId = article.getId();
        simpleAddTags(articleId, articleDTO.getTag());
        simpleAddVersion(article, articleDTO.getTag());
        ArticleVO articleVO = new ArticleVO();
        articleVO.setId(articleId);

        return articleVO;
    }

    /**
     * 按id获取文章
     *
     * @param id ID
     * @return {@link ArticleVO }
     */
    @Override
    @Cacheable(value = "article:info", key = "#id")
    public ArticleVO getArticleById(Integer id) {
        Article article = articleMapper.getArticleById(id);
        ArticleVO articleVO = articleToVO(article);
        return articleVO;
    }

    /**
     * 获取文章列表
     *
     * @param page    页
     * @param size    尺寸
     * @param tag     标签
     * @param keyword 关键词
     * @return {@link ArticleListVO }
     */
    @Override
    public ArticleListVO getArticleList(int page, int size, String tag, String keyword) {
        PageHelper.startPage(page, size);
        List<Article> articles = articleMapper.getArticleList(tag, keyword);
        PageInfo<Article> pageInfo = new PageInfo<>(articles);
        return buildArticleListVO(pageInfo);
    }

    /**
     * 删除文章
     *
     * @param id     ID
     * @param userId 用户ID
     */
    @Override
    @Transactional
    @CacheEvict(value = "article:info", key = "#id")
    public void deleteArticle(Integer id, Integer userId) {
        Article article = baseMapper.selectById(id);
        if (article == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        if (!Objects.equals(userId, article.getWriterId())) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
        List<String> tags = articleMapper.getTagsByArticleId(id);
        articleMapper.deleteArticleById(id);
        simpleDeleteTags(id, tags);

        if (article.getImage() != null && !article.getImage().isEmpty()) {
            List<String> imageKeys = JSON.parseArray(article.getImage(), String.class);
            for (String imageKey : imageKeys) {
                fileService.deleteObject(imageKey);
            }
        }

        LambdaQueryWrapper<ArticleVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleVersion::getArticleId, id);
        articleVersionMapper.delete(queryWrapper);
    }

    /**
     * 按id更新文章
     *
     * @param id            ID
     * @param articleAddDTO 文章添加到
     * @param userId        用户ID
     */
    @Override
    @Transactional
    @CacheEvict(value = "article:info", key = "#id")
    public void updateArticleById(Integer id, ArticleAddDTO articleAddDTO, Integer userId) {
        Article article = articleMapper.getArticleById(id);
        if (article == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        if (!userId.equals(article.getWriterId()))
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        List<String> oldTags = articleMapper.getTagsByArticleId(article.getId());
        simpleAddVersion(article, oldTags);
        Double oldVersion = article.getVersion() == null ? 1.0 : article.getVersion();
        BeanUtils.copyProperties(articleAddDTO, article);
        if (articleAddDTO.getVersion() == null)
            article.setVersion(BigDecimal.valueOf(oldVersion).add(new BigDecimal("0.1")).doubleValue());
        article.setImage(JSON.toJSONString(articleAddDTO.getImage()));
        article.setGmtModified(LocalDateTime.now());
        articleMapper.updateArticle(article);
        articleMapper.deleteArticleTagById(id);
        List<String> tags = articleAddDTO.getTag();
        simpleDeleteTags(id, oldTags);
        simpleAddTags(id, tags);

    }

    /**
     * 获取我的文章列表
     *
     * @param page   页
     * @param size   尺寸
     * @param userId 用户ID
     * @return {@link ArticleListVO }
     */
    @Override
    public ArticleListVO getMyArticleList(int page, int size, Integer userId) {
        PageHelper.startPage(page, size);
        List<Article> articles = articleMapper.getMyArticleList(userId);
        PageInfo<Article> pageInfo = new PageInfo<>(articles);
        return buildArticleListVO(pageInfo);
    }

    /**
     * 获取版本
     *
     * @param id     ID
     * @param userId 用户ID
     * @return {@link List }<{@link ArticleVersionVO }>
     */
    @Override
    public List<ArticleVersionVO> getVersions(Integer id, Integer userId) {
        Article article = articleMapper.getArticleById(id);
        if (article == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        if (!Objects.equals(userId, article.getWriterId()))
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

    /**
     * 回滚版本
     *
     * @param id        ID
     * @param versionId 版本id
     * @param userId    用户ID
     * @return {@link ArticleVersionVO }
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "article:info", key = "#id"),
            @CacheEvict(value = "article:version:info", allEntries = true)
    })
    public ArticleVersionVO rollbackVersion(Integer id, Integer versionId, Integer userId) {
        Article article = articleMapper.getArticleById(id);
        if (article == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        if (!userId.equals(article.getWriterId()))
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        ArticleVersionVO articleVersion = getVersion(id, versionId, userId);
        article.setTitle(articleVersion.getTitle());
        article.setContent(articleVersion.getContent());
        article.setVersion(articleVersion.getVersion());
        article.setGmtModified(LocalDateTime.now());
        articleMapper.updateArticle(article);
        List<String> oldTags = articleMapper.getTagsByArticleId(id);
        articleMapper.deleteArticleTagById(id);
        simpleDeleteTags(id, oldTags);
        List<String> rollbackTags = JSON.parseArray(articleVersion.getTag(), String.class);
        simpleAddTags(id, rollbackTags);
        return articleVersion;
    }

    /**
     * 获取版本
     *
     * @param id        ID
     * @param versionId 版本id
     * @return {@link ArticleVersionVO }
     */
    @Override
    @Cacheable(value = "article:version:info", key = "#userId + ':' + #id + ':' + #versionId")
    public ArticleVersionVO getVersion(Integer id, Integer versionId, Integer userId) {
        Article article = articleMapper.getArticleById(id);
        if (article == null) {
            throw new NoSuchArticle(RespondCode.NOT_FOUND);
        }
        if (!Objects.equals(userId, article.getWriterId())) {
            throw new NoAuthorization(RespondCode.FORBIDDEN);
        }
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
