package com.likeu.word.common.interceptor;

import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.common.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 鉴权拦截器单测：白名单、缺失/非法/失效 token、放行后注入 userId
 */
class AuthInterceptorTest {

    private static final String TOKEN = "valid-token";

    private JwtUtil jwtUtil;

    private RedisUtil redisUtil;

    private AuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        redisUtil = mock(RedisUtil.class);
        interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(interceptor, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(interceptor, "redisPrefix", "likeu:token:");
        ReflectionTestUtils.setField(interceptor, "extraWhiteList", "/h2-console, /v3/api-docs");
        interceptor.initWhiteList();
    }

    private MockHttpServletRequest request(String servletPath, String authHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(servletPath);
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        return request;
    }

    @Test
    @DisplayName("固定白名单（/user/login）无 token 直接放行")
    void fixedWhiteListIsAllowed() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request("/user/login", null), response, new Object()));
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("配置化白名单（/h2-console、/v3/api-docs）生效")
    void extraWhiteListIsAllowed() throws Exception {
        assertTrue(interceptor.preHandle(request("/h2-console", null), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("/v3/api-docs", null), new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("健康检查端点免登录放行，/actuator/info 仍需鉴权")
    void actuatorHealthIsOpenButInfoIsNot() throws Exception {
        assertTrue(interceptor.preHandle(request("/actuator/health", null), new MockHttpServletResponse(), new Object()));

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request("/actuator/info", null), response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("缺少 Authorization 头 -> 401 未登录")
    void missingTokenIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", null), response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("未登录或登录已过期"));
    }

    @Test
    @DisplayName("非 Bearer 认证头 -> 401")
    void nonBearerHeaderIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", "Basic abc"), response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("token 签名非法 -> 401 Token无效")
    void invalidTokenIsRejected() throws Exception {
        when(jwtUtil.getUserId("bad-token")).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", "Bearer bad-token"), response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token无效"));
    }

    @Test
    @DisplayName("token 未在 Redis 中（已登出或过期）-> 401")
    void tokenAbsentInRedisIsRejected() throws Exception {
        when(jwtUtil.getUserId(TOKEN)).thenReturn(7L);
        when(redisUtil.get("likeu:token:7")).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", "Bearer " + TOKEN), response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录已过期"));
    }

    @Test
    @DisplayName("Redis 中 token 与请求不一致（已被新登录覆盖）-> 401")
    void tokenMismatchIsRejected() throws Exception {
        when(jwtUtil.getUserId(TOKEN)).thenReturn(7L);
        when(redisUtil.get("likeu:token:7")).thenReturn("another-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", "Bearer " + TOKEN), response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("校验通过时放行并注入 userId")
    void validTokenInjectsUserId() throws Exception {
        when(jwtUtil.getUserId(TOKEN)).thenReturn(7L);
        when(redisUtil.get("likeu:token:7")).thenReturn(TOKEN);
        MockHttpServletRequest request = request("/study/new-words", "Bearer " + TOKEN);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(7L, request.getAttribute("userId"));
    }
}
