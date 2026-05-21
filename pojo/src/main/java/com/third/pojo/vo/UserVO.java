package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserVO {
    private int id;
    private String name;
    private String image;
    private int level;
    private LocalDateTime gmtCreate;
}
