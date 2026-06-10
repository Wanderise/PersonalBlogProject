package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {
    private Integer id;
    private String name;
    private String description;
    private LocalDateTime gmtCreate;
    private long docCount;
}
