package com.wxc.oj.judger;

import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.RabbitMQConstant;
import com.wxc.oj.enums.LanguageConfigEnum;
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

    /**
     * concurrency = "20": 同一个 Listener 最多会同时启动 20 个消费者线程来并行消费该队列中的消息。
     * @param message
     */
    @RabbitListener(queues = RabbitMQConstant.SUBMISSION_QUEUE, messageConverter = "jacksonConverter", concurrency = "20")
    public void listenSubmission(SubmissionMessage message) {
        Long submissionId = message.getId();
        doJudge(submissionId);
    }

    @Override
    public void doJudge(Long submissionId) {
        // 1. 获取提交和题目信息
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Problem problem = problemService.getById(submission.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String language = submission.getLanguage();
        LanguageConfigEnum languageEnum = LanguageConfigEnum.valueOf(language);
        // 2. 获取语言策略
        JudgeStrategy strategy = judgeStrategyFactory.getStrategy(languageEnum);

        strategy.doJudge(submission, problem);
    }
}