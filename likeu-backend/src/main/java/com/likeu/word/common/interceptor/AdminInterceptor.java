package com.likeu.word.common.interceptor;

import com.likeu.word.mapper.UserMapper;
import com.likeu.word.modules.user.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 管理员拦截器
 *
 * <p>只拦截 {@code /admin/**}，必须注册在 {@link AuthInterceptor} 之后：
 * 先由 AuthInterceptor 完成登录校验并把 userId 写入请求属性，这里再判断该用户是否管理员。</p>
 *
 * <p>管理员按配置 {@code likeu.admin.openids}（逗号分隔的 openid）识别，未配置时**全部拒绝**，
 * 避免上线忘配导致导入接口裸奔；后续需要多人管理再迁到 DB 字段。</p>
 */
@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {

    /**
     * 管理员 openid 白名单，逗号分隔。
     * 留空表示无人是管理员（导入接口对所有人关闭）。
     */
    @Value("${likeu.admin.openids:}")
    private String adminOpenids;

    @Resource
    private UserMapper userMapper;

    private Set<String> adminSet = Collections.emptySet();

    @PostConstruct
    void initAdminSet() {
        Set<String> set = new HashSet<>();
        if (StringUtils.hasText(adminOpenids)) {
            for (String openid : adminOpenids.split(",")) {
                String trimmed = openid.trim();
                if (!trimmed.isEmpty()) {
                    set.add(trimmed);
                }
            }
        }
        this.adminSet = set;
        if (set.isEmpty()) {
            log.warn("未配置 likeu.admin.openids，/admin/** 对所有用户关闭");
        } else {
            log.info("管理员 openid 白名单已加载, 数量={}", set.size());
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null || !isAdmin(userId)) {
            log.warn("非管理员访问受限接口: uri={}, userId={}", request.getServletPath(), userId);
            reject(response);
            return false;
        }
        return true;
    }

    private boolean isAdmin(Long userId) {
        if (adminSet.isEmpty()) {
            return false;
        }
        UserEntity user = userMapper.selectById(userId);
        return user != null && adminSet.contains(user.getOpenid());
    }

    /**
     * 鉴权类失败沿用 {@link AuthInterceptor} 的做法返回真实状态码（而非 200 + body code），
     * 便于网关/日志按状态码直接识别越权访问
     */
    private void reject(HttpServletResponse response) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"无权限\"}");
    }
}
