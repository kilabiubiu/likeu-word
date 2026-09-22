package com.likeu.word.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 全局跨域配置
 *
 * <p>小程序请求不受浏览器同源策略约束，因此跨域仅对 Web 端有意义。
 * 出于安全考虑不再默认放开所有来源：只有显式配置了
 * {@code likeu.cors.allowed-origins} 白名单时才返回跨域响应头，
 * 且默认不携带凭据（allowCredentials=false）。</p>
 */
@Configuration
public class CorsConfig {

    /** 允许跨域的来源白名单，多个用逗号分隔；留空表示不启用跨域 */
    @Value("${likeu.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        if (StringUtils.hasText(allowedOrigins)) {
            config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
            config.addAllowedHeader("*");
            config.addAllowedMethod("*");
            config.setMaxAge(3600L);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
