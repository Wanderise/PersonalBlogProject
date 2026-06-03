package com.third.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIMessage {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime gmtCreate;
}
