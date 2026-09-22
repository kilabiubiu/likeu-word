package com.likeu.word.common.interceptor;

import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.common.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 鉴权拦截器
 * 拦截除白名单外的所有请求，校验token并设置userId到请求属性
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 白名单URL（不需要登录即可访问） */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/h2-console",
            "/doc.html",
            "/swagger-resources",
            "/v3/api-docs",
            "/webjars/",
            "/favicon.ico",
            "/error"
    );

    @Value("${jwt.redis-prefix}")
    private String redisPrefix;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getServletPath() != null ? request.getServletPath() : request.getRequestURI();

        // 白名单放行
        for (String white : WHITE_LIST) {
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