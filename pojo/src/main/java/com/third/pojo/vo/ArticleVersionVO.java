package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ArticleVersionVO {
    private Integer id;
    private Integer articleId;
    private Double version;
    private String title;
    private String content;
    private String tag;
    private LocalDate gmtCreate;

}
