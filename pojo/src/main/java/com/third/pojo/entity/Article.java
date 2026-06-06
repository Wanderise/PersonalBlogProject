package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文章
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@ApiModel(value = "Article对象", description = "文章")
public class Article implements Serializable {

        private static final long serialVersionUID = 1L;

      /**
     * 文章id
     */
      @ApiModelProperty("文章id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

      /**
     * 作者id
     */
      @ApiModelProperty("作者id")
    private Integer writerId;

    private String writerName;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private String content;

    private Double version;

    private String image;

    private String title;
}
