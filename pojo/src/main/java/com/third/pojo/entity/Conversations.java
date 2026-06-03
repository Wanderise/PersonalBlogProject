package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("ai_conversation")
public class Conversations {
    private Long id;
    private Long userId;
    private String title;
    private Long agentId;
    private LocalDate gmtCreate;
    private LocalDate gmtModified;
}
