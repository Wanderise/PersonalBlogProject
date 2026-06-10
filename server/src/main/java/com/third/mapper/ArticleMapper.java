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


    @Insert("insert into blog.article(writer_id, gmt_create, gmt_modified, content, title, image, version) "+
            "values (#{writerId}, #{gmtCreate}, #{gmtModified}, #{content}, #{title}, #{image}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addArticle(Article articleEntity);

    @Insert("insert ignore into blog.article_tag(article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    void addArticleTag(Integer articleId, Integer tagId);

    @Select("select t.name from blog.tag t inner join blog.article_tag at on t.id = at.tag_id where at.article_id = #{articleId}")
    List<String> getTagsByArticleId(Integer articleId);

    @Select("select a.id, a.writer_id AS writerId, a.gmt_create AS gmtCreate, a.gmt_modified AS gmtModified, a.content, a.title, a.image, a.version, u.name AS writerName from blog.article a INNER JOIN blog.user u ON a.writer_id = u.id where a.id = #{id}")
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
    int getArticleCountByTagId(Integer tagId);

    @Delete("delete from blog.tag where id = #{tagId}")
    void deleteTagById(Integer tagId);

    @Update("update blog.article set title = #{title}, content = #{content}, image = #{image}, gmt_modified = #{gmtModified}, version = #{version} where id = #{id}")
    void updateArticle(Article article);

    List<Article> getMyArticleList(Integer userId);


}
