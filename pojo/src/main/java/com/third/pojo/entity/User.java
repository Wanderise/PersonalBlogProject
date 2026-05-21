package com.third.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class User {
    private int id;
    private String name;
    private String password;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private int level;
    private String image;
}
