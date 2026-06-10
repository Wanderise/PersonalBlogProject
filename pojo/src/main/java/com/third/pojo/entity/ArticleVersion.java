package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ArticleVersion {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer articleId;
    private Double version;
    private String title;
    private String content;
    private String tag;
    private LocalDate gmtCreate;

}
