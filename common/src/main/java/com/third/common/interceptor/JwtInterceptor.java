package com.third.common.interceptor;

import com.third.common.context.UserContext;
import com.third.common.utools.JJWTUtil;
import com.third.pojo.vo.UserVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authorization.substring(7);
        try {
            Claims claims = JJWTUtil.parseJWT(token);
            String userName = claims.get("UserName").toString();
            Integer userId = ((Number) claims.get("userId")).intValue();
            UserVO userVO = new UserVO();
            userVO.setId(userId);
            userVO.setName(userName);
            UserContext.setUser(userVO);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("token无效或过期");
        }
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
