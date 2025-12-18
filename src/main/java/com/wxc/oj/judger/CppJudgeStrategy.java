package com.wxc.oj.judger;

import com.wxc.oj.constant.LanguageConfigs;
import com.wxc.oj.model.dto.sandbox.LanguageConfig;
import org.springframework.stereotype.Service;

/**
 * C++判题策略
 */
@Service
public class CppJudgeStrategy extends AbstractJudgeStrategy {

    @Override
    protected String getExecutableFileName() {
        return "main";
    }

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