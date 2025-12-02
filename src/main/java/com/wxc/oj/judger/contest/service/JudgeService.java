package com.wxc.oj.judger.contest.service;

import com.wxc.oj.model.judge.JudgeCaseResult;
import com.wxc.oj.sandbox.dto.Result;
import com.wxc.oj.sandbox.dto.SandBoxResponse;
import com.wxc.oj.sandbox.model.LanguageConfig;

import java.io.IOException;
import java.util.List;

public interface JudgeService {
    void doJudge(Long submissionId) throws IOException;



    Result compileCode(String sourceCode, LanguageConfig languageConfig) throws IOException;
}
