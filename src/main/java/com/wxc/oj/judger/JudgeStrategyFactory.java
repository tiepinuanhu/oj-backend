package com.wxc.oj.judger;

import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 判题策略工厂
 */
@Component
public class JudgeStrategyFactory {
    @Resource
    private CppJudgeStrategy cppJudgeStrategy;
    @Resource
    private JavaJudgeStrategy javaJudgeStrategy;
    @Resource
    private PythonJudgeStrategy pythonJudgeStrategy;

    /**
     * 根据语言获取策略
     */
    public JudgeStrategy getStrategy(SubmissionLanguageEnum languageEnum) {
        return switch (languageEnum) {
            case CPP -> cppJudgeStrategy;
            case JAVA -> javaJudgeStrategy;
            case PYTHON -> pythonJudgeStrategy;
            default -> throw new IllegalArgumentException("不支持的提交语言: " + languageEnum.getValue());
        };
    }
}