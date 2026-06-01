package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.Article;
import com.third.pojo.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * <p>
 * 文章 Mapper 接口
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {


    @Insert("insert into blog.article(writer_id, tag, gmt_create, gmt_modified, content, title, image) "+
            "values (#{writerId}, #{tag}, #{gmtCreate}, #{gmtModified}, #{content}, #{title}, #{image})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addArticle(Article articleEntity);

    @Insert("insert into blog.article_tag(article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    void addArticleTag(int articleId, int tagId);

    @Select("select *, blog.user.name AS writerName from blog.article INNER JOIN blog.user ON blog.article.writer_id = blog.user.id where blog.article.id = #{id}")
    Article getArticleById(Integer id);

    List<Article> getArticleList(String tag, String keyword);

    @Delete("delete from blog.article where id = #{id}")
    void deleteArticleById(Integer id);

    @Delete("delete from blog.article_tag where article_id = #{id}")
    void deleteArticleTagById(Integer id);

    @Select("select * from blog.tag where name = #{tag}")
    Tag getTagByName(String tag);

    @Insert("insert into blog.tag(name, gmt_create) values (#{name}, #{gmtCreate})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addTag(Tag tag);

    @Select("select count(article_id) from blog.article_tag where tag_id = #{tagId}")
    int getArticleTagByTagId(Integer tagId);

    @Delete("delete from blog.tag where id = #{tagId}")
    void deleteTagById(Integer tagId);
}
