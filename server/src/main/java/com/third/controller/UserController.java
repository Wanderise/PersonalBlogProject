package com.third.controller;

import com.third.common.context.UserContext;
import com.third.common.result.Result;
import com.third.pojo.dto.UserDTO;
import com.third.pojo.vo.UserLoginVO;
import com.third.pojo.vo.UserVO;
import com.third.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name="用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "注册")
    public Result register(@RequestBody UserDTO user) {
        userService.register(user);
        return Result.success();
    }
    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserDTO user) {
        UserLoginVO res = userService.login(user);
        return Result.success(res);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<UserVO> userInfo() {
        Integer userId = UserContext.getUserId();
        UserVO userVO = userService.userInfo(userId);
        return Result.success(userVO);
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<UserVO> updateUserInfo(@RequestBody UserDTO userDTO) {
        Integer userId = UserContext.getUserId();
        UserVO userVO = userService.updateUserInfo(userDTO, userId);
        return Result.success(userVO);
    }

    @PutMapping("/avatar")
    public Result updateUserAvatar(@RequestBody UserDTO userDTO) {
        Integer userId = UserContext.getUserId();
        userService.updateUserAvatar(userDTO, userId);
        return Result.success();
    }

}
