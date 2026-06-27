package com.third.common.interceptor;

import com.third.common.context.UserContext;
import com.third.common.exception.NoAuthorization;
import com.third.pojo.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtInterceptorTest {

    private final JwtInterceptor interceptor = new JwtInterceptor();

    @AfterEach
    void cleanUp() {
        UserContext.clear();
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        NoAuthorization error = assertThrows(NoAuthorization.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));

        assertEquals(401, error.getCode());
    }

    @Test
    void allowsPublicArticleDetailGetWithoutBearerToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/article/1");
        when(request.getContextPath()).thenReturn("");

        boolean allowed = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        assertEquals(true, allowed);
    }

    @Test
    void rejectsArticleMutationWithoutBearerToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/article/1");

        NoAuthorization error = assertThrows(NoAuthorization.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));

        assertEquals(401, error.getCode());
    }

    @Test
    void clearsUserContextAfterRequestCompletion() throws Exception {
        UserVO user = new UserVO();
        user.setId(7);
        UserContext.setUser(user);

        interceptor.afterCompletion(mock(HttpServletRequest.class),
                mock(HttpServletResponse.class), new Object(), null);

        assertNull(UserContext.getUserId());
    }
}
