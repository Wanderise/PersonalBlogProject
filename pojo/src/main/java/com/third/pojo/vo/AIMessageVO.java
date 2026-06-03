package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIMessageVO {
    private Integer id;
    private String role;
    private String content;
    private LocalDateTime gmtCreate;
}
