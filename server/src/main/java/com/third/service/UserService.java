package com.third.service;

import com.third.pojo.dto.UserDTO;
import com.third.pojo.vo.UserLoginVO;
import com.third.pojo.vo.UserVO;

public interface UserService {
    void register(UserDTO user);

    UserLoginVO login(UserDTO user);

    UserVO userInfo(Integer userId);

    UserVO updateUserInfo(UserDTO userDTO, Integer userId);

    void updateUserAvatar(UserDTO userDTO, Integer userId);
}
