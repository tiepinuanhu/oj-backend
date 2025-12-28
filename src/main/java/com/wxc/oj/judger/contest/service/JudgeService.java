package com.wxc.oj.judger.contest.service;

import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.req.sandbox.LanguageConfig;

import java.io.IOException;

public interface JudgeService {
    void doJudge(Long submissionId) throws IOException;



    Result compileCode(String sourceCode, LanguageConfig languageConfig) throws IOException;
}
