package com.wxc.oj.judger;

import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.RabbitConstant;
import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.queueMessage.SubmissionMessage;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.service.SubmissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {
    @Resource
    private JudgeStrategyFactory judgeStrategyFactory;
    @Resource
    private SubmissionService submissionService;
    @Resource
    private ProblemService problemService;

    @RabbitListener(queues = RabbitConstant.SUBMISSION_QUEUE, messageConverter = "jacksonConverter", concurrency = "20")
    public void listenSubmission(SubmissionMessage message) throws IOException {
        Long submissionId = message.getId();
        doJudge(submissionId);
    }

    @Override
    public void doJudge(Long submissionId) throws IOException {
        // 1. 获取提交和题目信息
        Submission submission = submissionService.getById(submissionId);
        Problem problem = problemService.getById(submission.getProblemId());
        if (submission == null || problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String language = submission.getLanguage();
        SubmissionLanguageEnum languageEnum = SubmissionLanguageEnum.from(language);
        // 2. 获取语言策略
        JudgeStrategy strategy = judgeStrategyFactory.getStrategy(languageEnum);

        // 3. 执行判题（模板方法）
        if (strategy instanceof AbstractJudgeStrategy abstractStrategy) {
            abstractStrategy.doJudge(submission, problem);
        }
    }
}