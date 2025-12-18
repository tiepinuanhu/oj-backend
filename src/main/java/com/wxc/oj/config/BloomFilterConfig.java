package com.wxc.oj.config;

import com.google.common.base.Preconditions;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class BloomFilterConfig {

    /**
     * 预估插入数量（根据业务调整）
     */
    private static final int EXPECTED_INSERTIONS = 100000;

    /**
     * 误判率（如0.01表示1%的误判率）
     */
    private static final double FPP = 0.01;

    /**
     * 注册布隆过滤器Bean（示例：存储用户ID，String类型）
     */
    @Bean
    public BloomFilter<String> userBloomFilter() {
        // Funnels.stringFunnel：指定元素类型为String，字符集UTF-8
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP
        );

        // 可选：初始化时预加载数据（如从数据库加载已存在的用户ID）
        // loadInitData(bloomFilter);

        return bloomFilter;
    }

    /**
     * 示例：预加载数据库中的用户ID到布隆过滤器
     */
    private void loadInitData(BloomFilter<String> bloomFilter) {
        // 模拟从数据库查询所有用户ID
        // List<String> userIds = userMapper.listAllUserIds();
        // userIds.forEach(bloomFilter::put);
    }
}