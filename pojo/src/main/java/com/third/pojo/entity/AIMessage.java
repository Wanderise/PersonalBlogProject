package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer conversationId;
    private String role;
    private String content;
    private LocalDateTime gmtCreate;
}
