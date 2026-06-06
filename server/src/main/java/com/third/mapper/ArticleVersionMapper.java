package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.Article;
import com.third.pojo.entity.ArticleVersion;
import com.third.pojo.vo.ArticleVersionVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleVersionMapper extends BaseMapper<ArticleVersion> {

    @Insert("insert into blog.article_version(id, article_id, version, title, content, tag, gmt_create) " +
            "VALUES(#{id}, #{articleId}, #{version}, #{title}, #{content}, #{tag}, #{gmtCreate}) ")
    void addArticleVersion(ArticleVersion articleVersion);
}
