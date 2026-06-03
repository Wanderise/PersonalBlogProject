package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIMessageVO {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime gmtCreate;
}
