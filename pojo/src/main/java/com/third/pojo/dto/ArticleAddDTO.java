package com.third.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleAddDTO {
    private String title;
    private Integer writerId;
    private String content;
    private List<String> tag;
}
