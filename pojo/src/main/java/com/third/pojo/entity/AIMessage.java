package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AIMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer conversationId;
    private String role;
    private String content;
    private LocalDateTime gmtCreate;
}
