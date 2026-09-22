package com.likeu.word.common.interceptor;

import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.common.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 鉴权拦截器
 * 拦截除白名单外的所有请求，校验token并设置userId到请求属性
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 固定白名单URL（不需要登录即可访问）
     *
     * <p>{@code /actuator/health} 供存活探针使用，只返回 UP/DOWN（`show-details=never`）；
     * {@code /actuator/info} 未列入白名单，避免对外泄露构建信息。</p>
     */
    private static final List<String> FIXED_WHITE_LIST = Arrays.asList(
            "/user/login",
            "/favicon.ico",
            "/error",
            "/actuator/health"
    );

    /**
     * 额外的免登录路径，逗号分隔。
     * 仅用于开发调试（如 dev 的 /h2-console），生产必须保持为空，
     * 否则等于把调试入口暴露给所有人。
     */
    @Value("${likeu.auth.white-list:}")
    private String extraWhiteList;

    @Value("${jwt.redis-prefix}")
    private String redisPrefix;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    private List<String> whiteList;

    @PostConstruct
    void initWhiteList() {
        List<String> paths = new ArrayList<>(FIXED_WHITE_LIST);
        if (StringUtils.hasText(extraWhiteList)) {
            for (String path : extraWhiteList.split(",")) {
                String trimmed = path.trim();
                if (!trimmed.isEmpty()) {
                    paths.add(trimmed);
                }
            }
        }
        this.whiteList = paths;
        log.info("鉴权白名单: {}", whiteList);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getServletPath() != null ? request.getServletPath() : request.getRequestURI();

        // 白名单放行
        for (String white : whiteList) {
            if (uri.startsWith(white)) {
                return true;
            }
        }

        // 获取token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少token: uri={}", uri);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
            return false;
        }

        String token = authHeader.substring(7);

        // 解析JWT
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            log.warn("token无效: uri={}", uri);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效\"}");
            return false;
        }

        // 校验Redis中token是否一致（防止token被覆盖/已登出）
        String redisKey = redisPrefix + userId;
        String cachedToken = redisUtil.get(redisKey);
        if (cachedToken == null || !cachedToken.equals(token)) {
            log.warn("token已过期或已失效: userId={}", userId);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        // 将userId注入请求属性，Controller中通过@RequestAttribute获取
        request.setAttribute("userId", userId);
        return true;
    }
}