package com.wxc.oj.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import feign.RequestInterceptor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Encoder;
import org.springframework.beans.factory.annotation.Qualifier; // 导入 Qualifier 注解

import java.nio.charset.StandardCharsets;

@Configuration
public class FeignConfig {

    /**
     * Feign 专用 ObjectMapper（Long 不转字符串）
     * 方法名 feignObjectMapper 作为 Bean 的名称，后续用 @Qualifier 引用
     */
    @Bean
    public ObjectMapper feignObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        // 注意：这里不添加 Long 转字符串的序列化器！
        // 可同步全局配置的其他规则（如日期格式），但移除 Long 转字符串
        objectMapper.registerModule(module);
        return objectMapper;
    }

    /**
     * Feign 专用 Encoder：通过 @Qualifier 精准引用 feignObjectMapper
     */
    @Bean
    public Encoder feignEncoder(
            @Qualifier("feignObjectMapper") ObjectMapper feignObjectMapper // 精准指定 Bean 名称
    ) {
        // 绑定 Feign 专用 ObjectMapper 到转换器
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(feignObjectMapper);
        // 构建 HttpMessageConverters
        ObjectFactory<HttpMessageConverters> messageConverters = () -> new HttpMessageConverters(jacksonConverter);
        // 返回 SpringEncoder
        return new SpringEncoder(messageConverters);
    }

    // 保留之前的 Feign 日志拦截器（用于验证请求体）
    @Bean
    public RequestInterceptor logRequestInterceptor() {
        return template -> {
            if (template.url().contains("/run")) {
                byte[] body = template.body();
                if (body != null) {
                    String json = new String(body, StandardCharsets.UTF_8);
                    System.out.println("Feign 请求体：" + json); // 验证 cpuLimit 是否为数字
                }
            }
        };
    }
}