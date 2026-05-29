package com.third.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class ArticleListVO {
    private List<ArticleVO> articles;
    private int total;
    private int page;
    private int size;
}
