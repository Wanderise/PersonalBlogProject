package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConversationsVO {
    private Integer id;
    private String title;
    private Integer agentId;
    private LocalDate gmtCreate;
    private LocalDate gmtModified;
}
