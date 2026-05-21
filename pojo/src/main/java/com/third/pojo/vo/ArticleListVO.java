package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleListVO {

    private int id;
    private Integer writerId;
    private String title;
    private String content;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private List<String> tag;

}
