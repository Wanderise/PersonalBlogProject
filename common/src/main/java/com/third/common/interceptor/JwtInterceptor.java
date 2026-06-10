package com.third.common.interceptor;

import com.third.common.context.UserContext;
import com.third.common.enumerate.RespondCode;
import com.third.common.exception.NoAuthorization;
import com.third.common.utools.JJWTUtil;
import com.third.pojo.vo.UserVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检请求，浏览器CORS跨域会先发OPTIONS探测
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new NoAuthorization(RespondCode.UNAUTHORIZED);
        }
        String token = authorization.substring(7);
        try {
            // 从JWT中提取用户标识，写入ThreadLocal供本次请求使用
            Claims claims = JJWTUtil.parseJWT(token);
            String userName = (String) claims.get("UserName");
            Object userIdClaim = claims.get("userId");
            if (userName == null || userIdClaim == null) {
                throw new NoAuthorization(RespondCode.UNAUTHORIZED);
            }
            Integer userId = ((Number) userIdClaim).intValue();
            UserVO userVO = new UserVO();
            userVO.setId(userId);
            userVO.setName(userName);
            UserContext.setUser(userVO);
            return true;
        } catch (NoAuthorization e) {
            throw e;
        } catch (Exception e) {
            log.error("token解析失败", e);
            throw new NoAuthorization(RespondCode.UNAUTHORIZED);
        }
    }
    // 请求结束后必须清理ThreadLocal，防止线程池复用时用户信息串号
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
