package com.third.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBase {
    private Integer id;
    private Integer userId;
    private String name;
    private String description;
    private LocalDateTime gmtCreate;
}
