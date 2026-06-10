package com.third.service.impl;

import com.third.common.context.UserContext;
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

    @Override
    public void register(UserDTO user) {
        User u = new User();
        BeanUtils.copyProperties(user,u);
        // BCrypt内置随机盐，比MD5安全，不可逆
        String password = user.getPassword();
        password = encoder.encode(password);
        u.setPassword(password);
        u.setGmtCreate(LocalDateTime.now());
        u.setGmtModified(LocalDateTime.now());
        u.setLevel(0);
        log.info(u.toString());
        userMapper.registerUser(u);
    }

    @Override
    public UserLoginVO login(UserDTO userDTO) {
        User user = userMapper.login(userDTO);
        if (user == null) {
            throw new UserCountNotExist(RespondCode.NOT_FOUND);
        }
        String password = userDTO.getPassword();
        // BCrypt密文不可逆比较，必须用matches而非equals
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
        log.info("token:{}", token);
        loginVO.setToken(token);
        return loginVO;
    }

    @Override
    public UserVO userInfo() {
        Integer userId = UserContext.getUserId();
        User user = userMapper.getUserInfoById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        log.info("userVO:{}", userVO);
        return userVO;
    }

    @Override
    public UserVO updateUserInfo(UserDTO userDTO) {
        String userName = UserContext.getUserName();
        String name = userDTO.getName();
        User user = userMapper.getUserInfoByName(name);
        if (user != null && !name.equals(userName)) {
            throw new UserNameHasExist(RespondCode.NAME_EXIST);
        }
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateUserName(userName, name, now);
        user = userMapper.getUserInfoByName(name);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        log.info("userVO:{}", userVO);
        return userVO;
    }

    @Override
    public void updateUserAvatar(UserDTO userDTO) {
        String userName = UserContext.getUserName();
        User user = userMapper.getUserInfoByName(userName);
        String oldKey = user.getImage();
        // 先更新DB再删S3旧头像，防止DB失败导致S3文件已丢失
        user.setImage(userDTO.getObjectKey());
        userMapper.updateUserAvatar(user);
        if (oldKey != null && !oldKey.isEmpty()) {
            fileService.deleteObject(oldKey);
        }
    }
}
