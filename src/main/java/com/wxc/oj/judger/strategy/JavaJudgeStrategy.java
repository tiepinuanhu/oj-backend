package com.wxc.oj.judger.strategy;

import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.judger.AbstractJudgeStrategy;
import org.springframework.stereotype.Service;

/**
 * Java判题策略
 */
@Service
public class JavaJudgeStrategy extends AbstractJudgeStrategy {



    @Override
    public LanguageConfigEnum getLanguageConfig() {
        return LanguageConfigEnum.JAVA;
    }

    @Override
    protected boolean needCompile() {
        return true;
    }
}