package com.wxc.oj.judger;

import com.wxc.oj.model.dto.sandbox.LanguageConfig;
import com.wxc.oj.model.dto.sandbox.Result;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;

import java.io.IOException;

public interface JudgeService {
    void doJudge(Long submissionId) throws IOException;




}
