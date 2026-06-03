package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConversationsVO {
    private Long id;
    private String title;
    private Long agentId;
    private LocalDate gmtCreate;
    private LocalDate gmtModified;
}
