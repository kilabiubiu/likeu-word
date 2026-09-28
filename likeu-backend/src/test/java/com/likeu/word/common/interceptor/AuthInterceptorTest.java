package com.likeu.word.common.interceptor;

import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.modules.user.service.UserService;
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
 * 鉴权拦截器单测：白名单、云托管 openid 通道、无状态 token 通道、缺身份拒绝
 */
class AuthInterceptorTest {

    private static final String TOKEN = "valid-token";

    private JwtUtil jwtUtil;

    private UserService userService;

    private AuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userService = mock(UserService.class);
        interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(interceptor, "userService", userService);
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

    private MockHttpServletRequest openidRequest(String servletPath, String openid) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(servletPath);
        request.addHeader(AuthInterceptor.WX_OPENID_HEADER, openid);
        return request;
    }

    @Test
    @DisplayName("固定白名单（/user/login）无身份直接放行")
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
    @DisplayName("既无 openid 也无 token -> 401 未登录")
    void missingIdentityIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", null), response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("未登录或登录已过期"));
    }

    @Test
    @DisplayName("云托管注入 openid -> 自动解析用户并放行")
    void wxOpenidHeaderIsTrusted() throws Exception {
        when(userService.resolveUserIdByOpenid("o-abc")).thenReturn(7L);
        MockHttpServletRequest request = openidRequest("/study/new-words", "o-abc");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(7L, request.getAttribute("userId"));
    }

    @Test
    @DisplayName("openid 无法解析为用户 -> 401，不放行")
    void unresolvableOpenidIsRejected() throws Exception {
        when(userService.resolveUserIdByOpenid("o-abc")).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(openidRequest("/study/new-words", "o-abc"), response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("openid 通道优先于 token 通道")
    void openidTakesPrecedenceOverToken() throws Exception {
        when(userService.resolveUserIdByOpenid("o-abc")).thenReturn(9L);
        MockHttpServletRequest request = openidRequest("/study/new-words", "o-abc");
        request.addHeader("Authorization", "Bearer bad-token");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(9L, request.getAttribute("userId"));
    }

    @Test
    @DisplayName("非 Bearer 认证头 -> 401")
    void nonBearerHeaderIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", "Basic abc"), response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("token 签名非法或已过期 -> 401")
    void invalidTokenIsRejected() throws Exception {
        when(jwtUtil.getUserId("bad-token")).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request("/study/new-words", "Bearer bad-token"), response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录已过期"));
    }

    @Test
    @DisplayName("token 有效 -> 放行并注入 userId")
    void validTokenInjectsUserId() throws Exception {
        when(jwtUtil.getUserId(TOKEN)).thenReturn(7L);
        MockHttpServletRequest request = request("/study/new-words", "Bearer " + TOKEN);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(7L, request.getAttribute("userId"));
    }
}
