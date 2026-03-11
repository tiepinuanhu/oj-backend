package com.wxc.oj.judger;

import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;

/**
 * 判题策略接口（定义每种语言的特定行为）
 */
public interface JudgeStrategy {

    void doJudge(Submission submission, Problem problem);
}