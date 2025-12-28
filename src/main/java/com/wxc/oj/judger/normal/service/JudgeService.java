package com.wxc.oj.judger.normal.service;

import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.req.sandbox.LanguageConfig;

import java.io.IOException;

public interface JudgeService {
    void doJudge(Long submissionId) throws IOException;


    void pythonJudge(Submission submission, Problem problem) throws IOException;

    Result compileCode(String sourceCode, LanguageConfig languageConfig) throws IOException;

}
