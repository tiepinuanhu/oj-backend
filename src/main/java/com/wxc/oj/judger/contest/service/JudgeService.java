package com.wxc.oj.judger.contest.service;

import com.wxc.oj.model.dto.sandbox.Result;
import com.wxc.oj.model.dto.sandbox.LanguageConfig;

import java.io.IOException;

public interface JudgeService {
    void doJudge(Long submissionId) throws IOException;



    Result compileCode(String sourceCode, LanguageConfig languageConfig) throws IOException;
}
