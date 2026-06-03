package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private String password;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private int level;
    private String image;
}
