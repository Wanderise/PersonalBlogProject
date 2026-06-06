package com.third.pojo.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ArticleVersion {
    private Integer id;
    private Integer articleId;
    private Double version;
    private String title;
    private String content;
    private String tag;
    private LocalDate gmtCreate;

}
