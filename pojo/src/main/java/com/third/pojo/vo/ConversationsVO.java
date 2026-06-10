package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationsVO {
    private Integer id;
    private String title;
    private Integer agentId;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
