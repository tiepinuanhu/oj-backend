package com.wxc.oj.judger.strategy;

import com.wxc.oj.constant.LanguageConfigs;
import com.wxc.oj.judger.AbstractJudgeStrategy;
import com.wxc.oj.model.req.sandbox.LanguageConfig;
import org.springframework.stereotype.Service;

/**
 * C++判题策略
 */
@Service
public class PythonJudgeStrategy extends AbstractJudgeStrategy {


    @Override
    public LanguageConfig getLanguageConfig() {
        return LanguageConfigs.PYTHON;
    }

    @Override
    protected boolean needCompile() {
        return false;
    }

}