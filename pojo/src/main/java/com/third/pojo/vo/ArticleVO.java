package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {

    private int id;
    private Integer writerId;
    private String writerName;
    private String title;
    private String content;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private List<String> image;
    private List<String> imageUrls;
    private List<String> tag;
    private String summary;

}
