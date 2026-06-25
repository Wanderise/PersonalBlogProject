package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户vo
 *
 * @author 123
 * @date 2026/06/14
 */
@Data
public class UserVO {
    private Integer id;
    private String name;
    private String image;
    private int level;
    private LocalDateTime gmtCreate;
}
