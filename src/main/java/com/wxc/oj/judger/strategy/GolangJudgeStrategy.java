package com.wxc.oj.judger.strategy;

import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.judger.AbstractJudgeStrategy;
import org.springframework.stereotype.Service;

/**
 * Golang判题策略
 */
@Service
public class GolangJudgeStrategy extends AbstractJudgeStrategy {

    @Override
    protected LanguageConfigEnum getLanguageConfig() {
        return LanguageConfigEnum.GOLANG;
    }

    @Override
    protected boolean needCompile() {
        return true;
    }

}