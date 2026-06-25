package com.third.pojo.vo;

import lombok.Data;

/**
 * 用户登录vo
 *
 * @author 123
 * @date 2026/06/14
 */
@Data
public class UserLoginVO {
    private String token;
    private UserVO user;
}
