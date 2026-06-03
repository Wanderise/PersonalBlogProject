package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentVO {
    private Integer id;
    private String name;
    private String systemPrompt;
    private String icon;
    private LocalDateTime gmtCreate;

}
