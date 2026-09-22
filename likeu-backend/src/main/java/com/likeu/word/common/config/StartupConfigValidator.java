package com.likeu.word.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 启动期配置校验
 *
 * <p>密钥类配置一律通过环境变量注入（application.yml 中只保留空占位符），
 * 启动时若仍为空、或仍是代码库里的示例默认值，则直接拒绝启动，
 * 避免服务带着公开的默认密钥跑起来。</p>
 */
@Slf4j
@Component
public class StartupConfigValidator {

    /** 代码库中的示例占位值，命中即视为未配置 */
    private static final List<String> PLACEHOLDER_VALUES = Arrays.asList(
            "your-jwt-secret-key-change-me",
            "your-secret-here",
            "your-appid-here",
            "test-secret"
    );

    /** 开发模拟登录使用的 appid，此时不会调用微信接口，无需真实 secret */
    private static final String MOCK_APPID = "wx-test-appid";

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${wx.mini.appid:}")
    private String wxAppid;

    @Value("${wx.mini.secret:}")
    private String wxSecret;

    @PostConstruct
    public void validate() {
        requireConfigured("jwt.secret", jwtSecret, "JWT_SECRET");

        // 只有使用真实 appid 调用微信接口时才要求配置 appid 与 secret
        if (!MOCK_APPID.equals(wxAppid)) {
            requireConfigured("wx.mini.appid", wxAppid, "WX_MINI_APPID");
            requireConfigured("wx.mini.secret", wxSecret, "WX_MINI_SECRET");
        }

        log.info("启动配置校验通过");
    }

    private void requireConfigured(String key, String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("配置项 " + key + " 未设置，请通过环境变量 " + envName + " 注入");
        }
        if (PLACEHOLDER_VALUES.contains(value)) {
            throw new IllegalStateException(
                    "配置项 " + key + " 仍为示例默认值，请通过环境变量 " + envName + " 注入真实值");
        }
    }
}
