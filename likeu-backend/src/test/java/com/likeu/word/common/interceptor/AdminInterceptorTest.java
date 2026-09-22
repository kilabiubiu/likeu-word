package com.likeu.word.common.interceptor;

import com.likeu.word.mapper.UserMapper;
import com.likeu.word.modules.user.entity.UserEntity;
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
 * 管理员拦截器单测：白名单未配置、命中、不命中、未登录用户
 */
class AdminInterceptorTest {

    private UserMapper userMapper;

    private AdminInterceptor interceptor;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        interceptor = new AdminInterceptor();
        ReflectionTestUtils.setField(interceptor, "userMapper", userMapper);
    }

    private void initWith(String adminOpenids) {
        ReflectionTestUtils.setField(interceptor, "adminOpenids", adminOpenids);
        interceptor.initAdminSet();
    }

    private MockHttpServletRequest requestWithUser(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/admin/import/words");
        if (userId != null) {
            request.setAttribute("userId", userId);
        }
        return request;
    }

    private UserEntity user(Long id, String openid) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setOpenid(openid);
        return entity;
    }

    @Test
    @DisplayName("未配置管理员白名单 -> 全部拒绝（403）")
    void emptyWhiteListRejectsEveryone() throws Exception {
        initWith("");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(requestWithUser(1L), response, new Object()));
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("无权限"));
    }

    @Test
    @DisplayName("openid 命中白名单 -> 放行")
    void adminOpenidIsAllowed() throws Exception {
        initWith("openid_admin");
        when(userMapper.selectById(1L)).thenReturn(user(1L, "openid_admin"));

        assertTrue(interceptor.preHandle(requestWithUser(1L), new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("白名单支持逗号分隔并自动去除空格")
    void whiteListIsTrimmed() throws Exception {
        initWith(" openid_a , openid_b ");
        when(userMapper.selectById(2L)).thenReturn(user(2L, "openid_b"));

        assertTrue(interceptor.preHandle(requestWithUser(2L), new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("已登录但 openid 不在白名单 -> 拒绝（403）")
    void nonAdminOpenidIsRejected() throws Exception {
        initWith("openid_admin");
        when(userMapper.selectById(3L)).thenReturn(user(3L, "openid_normal"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(requestWithUser(3L), response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("请求属性中没有 userId（未通过登录校验）-> 拒绝（403）")
    void missingUserIdIsRejected() throws Exception {
        initWith("openid_admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(requestWithUser(null), response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("用户不存在（已被删除）-> 拒绝（403）")
    void unknownUserIsRejected() throws Exception {
        initWith("openid_admin");
        when(userMapper.selectById(9L)).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(requestWithUser(9L), response, new Object()));
        assertEquals(403, response.getStatus());
    }
}
