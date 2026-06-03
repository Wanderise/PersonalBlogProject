package com.third.pojo.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AIMessage {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime gmtCreate;
}
