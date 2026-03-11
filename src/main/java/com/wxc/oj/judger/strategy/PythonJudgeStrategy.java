package com.wxc.oj.judger.strategy;

import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.judger.AbstractJudgeStrategy;
import org.springframework.stereotype.Service;

/**
 * Python判题策略
 */
@Service
public class PythonJudgeStrategy extends AbstractJudgeStrategy {


    @Override
    public LanguageConfigEnum getLanguageConfig() {
        return LanguageConfigEnum.PYTHON;
    }

    @Override
    protected boolean needCompile() {
        return false;
    }

}