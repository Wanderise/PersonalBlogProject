package com.third.config;

import com.third.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class AccessLogAspect {

    @Around("within(com.third.controller..*)")
    public Object logControllerAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String handler = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.info("http request method={} uri={} handler={} userId={} ip={} status={} costMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    handler,
                    UserContext.getUserId(),
                    getClientIp(request),
                    response == null ? "-" : response.getStatus(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("http request failed method={} uri={} handler={} userId={} ip={} error={} costMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    handler,
                    UserContext.getUserId(),
                    getClientIp(request),
                    ex.getClass().getSimpleName(),
                    System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
