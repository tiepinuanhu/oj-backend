package com.wxc.oj.config;

import com.wxc.oj.interceptor.LoginProtectInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置拦截器
 */
@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

    @Resource
    private LoginProtectInterceptor loginProtectInterceptor;


    /**
     * 除了用户登陆和注册
     * 其余功能都要校验先token
     * 用户登录和用户注册api方向，其它接口必须校验token，在拦截器中校验token
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加拦截器, 并配置拦截路径
        // 路径是请求路径
        registry.addInterceptor(loginProtectInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/v3/**");
    }


}