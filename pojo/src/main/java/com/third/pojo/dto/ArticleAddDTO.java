package com.third.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArticleAddDTO {
    private String title;
    private Integer writerId;
    private String content;
    private Double version;
    private List<String> image;
    private List<String> tag;
}
