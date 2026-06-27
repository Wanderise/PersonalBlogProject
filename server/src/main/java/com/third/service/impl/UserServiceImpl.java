package com.third.service.impl;

import com.third.common.enumerate.RespondCode;
import com.third.common.exception.UserCountNotExist;
import com.third.common.exception.UserNameHasExist;
import com.third.common.exception.WrongPassword;
import com.third.common.utools.JJWTUtil;
import com.third.mapper.UserMapper;
import com.third.pojo.dto.UserDTO;
import com.third.pojo.entity.User;
import com.third.pojo.vo.UserLoginVO;
import com.third.pojo.vo.UserVO;
import com.third.service.FileService;
import com.third.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_BCRYPT_PASSWORD_LENGTH = 72;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FileService fileService;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static String requireUserName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 30) {
            throw new IllegalArgumentException("username is too long");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("username contains invalid characters");
        }
        return trimmed;
    }

    private static String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_BCRYPT_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password length must be between 6 and 72 characters");
        }
        return password;
    }


    /**
     * 注册
     *
     * @param user 用户
     */
    @Override
    public void register(UserDTO user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        String name = requireUserName(user.getName());
        String password = requirePassword(user.getPassword());
        if (userMapper.getUserInfoByName(name) != null) {
            throw new UserNameHasExist(RespondCode.NAME_EXIST);
        }
        User u = new User();
        BeanUtils.copyProperties(user,u);
        password = encoder.encode(password);
        u.setName(name);
        u.setPassword(password);
        u.setGmtCreate(LocalDateTime.now());
        u.setGmtModified(LocalDateTime.now());
        u.setLevel(0);
        userMapper.registerUser(u);
    }

    /**
     * 登录
     *
     * @param userDTO 用户dto
     * @return {@link UserLoginVO }
     */
    @Override
    public UserLoginVO login(UserDTO userDTO) {
        if (userDTO == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        String name = requireUserName(userDTO.getName());
        String password = requirePassword(userDTO.getPassword());
        userDTO.setName(name);
        User user = userMapper.login(userDTO);
        if (user == null) {
            throw new UserCountNotExist(RespondCode.NOT_FOUND);
        }
        if (!encoder.matches(password, user.getPassword())) {
            throw new WrongPassword(RespondCode.NOT_FOUND);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setUser(userVO);
        Map<String, Object> map = new HashMap<>();
        map.put("UserName", name);
        map.put("userId", user.getId());
        String token = JJWTUtil.createJWT(map);
        loginVO.setToken(token);
        return loginVO;
    }

    /**
     * 用户信息
     *
     * @param userId 用户ID
     * @return {@link UserVO }
     */
    @Override
    public UserVO userInfo(Integer userId) {
        User user = userMapper.getUserInfoById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

    /**
     * 更新用户信息
     *
     * @param userDTO  用户dto
     * @param userName 用户名
     * @return {@link UserVO }
     */
    @Override
    public UserVO updateUserInfo(UserDTO userDTO, Integer userId) {
        if (userDTO == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        String name = requireUserName(userDTO.getName());
        User user = userMapper.getUserInfoByName(name);
        if (user != null && !user.getId().equals(userId)) {
            throw new UserNameHasExist(RespondCode.NAME_EXIST);
        }
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateUserName(userId, name, now);
        user = userMapper.getUserInfoById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

    /**
     * 更新用户头像
     *
     * @param userDTO  用户dto
     * @param userName 用户名
     */
    @Override
    public void updateUserAvatar(UserDTO userDTO, Integer userId) {
        if (userDTO == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        FileService.validateObjectKey(userDTO.getObjectKey(), "avatars/");
        User user = userMapper.getUserInfoById(userId);
        String oldKey = user.getImage();
        user.setImage(userDTO.getObjectKey());
        userMapper.updateUserAvatar(user);
        if (oldKey != null && !oldKey.isEmpty()) {
            fileService.deleteObject(oldKey);
        }
    }
}
