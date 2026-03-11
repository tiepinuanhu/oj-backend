package com.wxc.oj.judger;

import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 判题策略工厂（自动注册，符合开闭原则）
 * <p>
 * 新增语言时只需：
 * 1. 在 SubmissionLanguageEnum 中新增枚举值
 * 2. 创建对应的 XxxJudgeStrategy 实现类并标注 @Service
 * 无需修改本工厂类
 * </p>
 */
@Component
public class JudgeStrategyFactory {

    private final Map<LanguageConfigEnum, JudgeStrategy> strategyMap = new EnumMap<>(LanguageConfigEnum.class);

    /**
     * Spring 自动注入所有 JudgeStrategy 实现类
     */
    public JudgeStrategyFactory(List<AbstractJudgeStrategy> strategies) {
        for (AbstractJudgeStrategy strategy : strategies) {
            strategyMap.put(strategy.getLanguageConfig(), strategy);
        }
    }

    /**
     * 根据语言获取策略
     */
    public JudgeStrategy getStrategy(LanguageConfigEnum languageEnum) {
        JudgeStrategy strategy = strategyMap.get(languageEnum);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的提交语言: " + languageEnum.getValue());
        }
        return strategy;
    }
}