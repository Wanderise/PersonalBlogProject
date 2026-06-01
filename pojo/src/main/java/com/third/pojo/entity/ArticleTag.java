package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 文章标签关系表
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("article_tag")
@ApiModel(value = "ArticleTag对象", description = "文章标签关系表")
public class ArticleTag implements Serializable {

        private static final long serialVersionUID = 1L;

      /**
     * 文章id
     */
      @ApiModelProperty("文章id")
    private Integer articleId;

      /**
     * 标签id
     */
      @ApiModelProperty("标签id")
    private Integer tagId;
}
