package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("ai_conversation")
public class Conversations {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String title;
    private Integer agentId;
    private LocalDate gmtCreate;
    private LocalDate gmtModified;
}
