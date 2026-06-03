package com.third.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Agent {
    private Integer id;
    private Integer userId;
    private String name;
    private String systemPrompt;
    private String icon;
    private LocalDateTime gmtCreate;
}
