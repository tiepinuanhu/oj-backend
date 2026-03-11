package com.wxc.oj.judger.strategy;

import com.wxc.oj.constant.LanguageConfigs;
import com.wxc.oj.judger.AbstractJudgeStrategy;
import com.wxc.oj.model.req.sandbox.LanguageConfig;
import org.springframework.stereotype.Service;

/**
 * C++判题策略
 */
@Service
public class CppJudgeStrategy extends AbstractJudgeStrategy {

    @Override
    public LanguageConfig getLanguageConfig() {
        // 从配置中读取C++的配置（替代硬编码的LanguageConfigs.CPP）
        return LanguageConfigs.CPP;
    }

    @Override
    protected boolean needCompile() {
        return true;
    }

}