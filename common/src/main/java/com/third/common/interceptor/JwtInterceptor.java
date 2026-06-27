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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (isPublicRequest(request)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new NoAuthorization(RespondCode.UNAUTHORIZED);
        }

        String token = authorization.substring(7);
        try {
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
            log.warn("token parse failed");
            throw new NoAuthorization(RespondCode.UNAUTHORIZED);
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.matches("/article/\\d+");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
