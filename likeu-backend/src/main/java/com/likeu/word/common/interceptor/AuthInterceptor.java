package com.likeu.word.common.interceptor;

import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.modules.user.service.UserService;
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
 * 鉴权拦截器
 *
 * <p>支持两种身份来源，优先级从高到低：</p>
 * <ol>
 *   <li><b>云托管注入的 openid</b>：小程序通过 {@code wx.cloud.callContainer} 调用时，
 *       微信网关会在请求头写入 {@value #WX_OPENID_HEADER}（openid）等信息，属于可信来源。
 *       这条链路不需要 {@code wx.login}、不需要 code2Session，也不需要自建登录态。</li>
 *   <li><b>无状态 JWT</b>：本地开发（wx.request 直连 localhost）或非 callContainer 客户端使用，
 *       只校验签名与过期时间，不再依赖服务端存储。</li>
 * </ol>
 *
 * <p>两者都不满足时按未登录处理（401）。校验通过后把 userId 写入请求属性，
 * Controller 通过 {@code @RequestAttribute Long userId} 获取，管理员校验见 {@link AdminInterceptor}。</p>
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 云托管通过 callContainer 调用时注入的用户身份请求头。
     *
     * <p>注意：该请求头由微信网关在服务端注入，只有在<b>关闭公网访问</b>、
     * 仅允许小程序/公众号内网调用时才可信；生产环境务必在「服务设置」中关闭公网访问。</p>
     */
    public static final String WX_OPENID_HEADER = "X-WX-OPENID";

    /** 校验通过后写入请求属性的 userId 键名 */
    public static final String USER_ID_ATTRIBUTE = "userId";

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

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

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

        // 通道一：云托管注入的 openid（可信）
        String openid = request.getHeader(WX_OPENID_HEADER);
        if (StringUtils.hasText(openid)) {
            Long userId = userService.resolveUserIdByOpenid(openid.trim());
            if (userId == null) {
                log.error("openid 无法解析为用户: uri={}", uri);
                reject(response, "用户初始化失败，请稍后重试");
                return false;
            }
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
            return true;
        }

        // 通道二：无状态 JWT（本地开发 / 普通 HTTP 客户端）
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少身份信息（既无 {} 也无 token）: uri={}", WX_OPENID_HEADER, uri);
            reject(response, "未登录或登录已过期");
            return false;
        }

        Long userId = jwtUtil.getUserId(authHeader.substring(7));
        if (userId == null) {
            log.warn("token无效或已过期: uri={}", uri);
            reject(response, "登录已过期，请重新登录");
            return false;
        }

        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }

    /**
     * 鉴权类失败返回真实状态码（而非 200 + body code），便于网关/日志按状态码直接识别越权访问
     */
    private void reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
