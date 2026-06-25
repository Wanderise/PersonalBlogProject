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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FileService fileService;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    /**
     * 注册
     *
     * @param user 用户
     */
    @Override
    public void register(UserDTO user) {
        User u = new User();
        BeanUtils.copyProperties(user,u);
        String password = user.getPassword();
        password = encoder.encode(password);
        u.setPassword(password);
        u.setGmtCreate(LocalDateTime.now());
        u.setGmtModified(LocalDateTime.now());
        u.setLevel(0);
        log.info(u.toString());
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
        User user = userMapper.login(userDTO);
        if (user == null) {
            throw new UserCountNotExist(RespondCode.NOT_FOUND);
        }
        String password = userDTO.getPassword();
        if (!encoder.matches(password, user.getPassword())) {
            throw new WrongPassword(RespondCode.NOT_FOUND);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setUser(userVO);
        Map<String, Object> map = new HashMap<>();
        map.put("UserName", userDTO.getName());
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
        log.info("userVO:{}", userVO);
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
        String name = userDTO.getName();
        User user = userMapper.getUserInfoByName(name);
        if (user != null && !user.getId().equals(userId)) {
            throw new UserNameHasExist(RespondCode.NAME_EXIST);
        }
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateUserName(userId, name, now);
        user = userMapper.getUserInfoById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        log.info("userVO:{}", userVO);
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
        User user = userMapper.getUserInfoById(userId);
        String oldKey = user.getImage();
        user.setImage(userDTO.getObjectKey());
        userMapper.updateUserAvatar(user);
        if (oldKey != null && !oldKey.isEmpty()) {
            fileService.deleteObject(oldKey);
        }
    }
}
