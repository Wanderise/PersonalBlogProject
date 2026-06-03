package com.third.common.context;

import com.third.pojo.vo.UserVO;

public class UserContext {
    public static final ThreadLocal<UserVO> userVOThreadLocal = new ThreadLocal<>();

    public static void setUser(UserVO userVO) {
        userVOThreadLocal.set(userVO);
    }

    public static Integer getUserId() {
        UserVO userVO = userVOThreadLocal.get();
        return userVO != null ? userVO.getId() : null;
    }

    public static String getUserName() {
        UserVO userVO = userVOThreadLocal.get();
        return userVO.getName();
    }

    public static void clear() {
        userVOThreadLocal.remove();
    }
}
