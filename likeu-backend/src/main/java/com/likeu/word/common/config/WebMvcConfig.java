package com.likeu.word.common.config;

import com.likeu.word.common.interceptor.AdminInterceptor;
import com.likeu.word.common.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置（注册拦截器）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Resource
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**");

        // 管理员校验必须在登录校验之后：依赖 AuthInterceptor 注入的 userId
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}