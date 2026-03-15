package com.wxc.oj.judger;

import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.RabbitMQConstant;
import com.wxc.oj.constant.RedisConstant;
import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.queueMessage.SubmissionMessage;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.service.SubmissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {
    @Resource
    private JudgeStrategyFactory judgeStrategyFactory;
    @Resource
    private SubmissionService submissionService;
    @Resource
    private ProblemService problemService;
    @Resource
    private RedissonClient redissonClient;

    /**
     * concurrency = "20": 同一个 Listener 最多会同时启动 20 个消费者线程来并行消费该队列中的消息。
     */
    @RabbitListener(queues = RabbitMQConstant.SUBMISSION_QUEUE,
            messageConverter = "jacksonConverter", concurrency = "20", ackMode = "AUTO")
    public void listenSubmission(SubmissionMessage message) {
        boolean acquired = false;
        String lockKey = RedisConstant.SUBMISSION_LOCK_KEY + message.getId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取分布式锁
            acquired = lock.tryLock(0, RedisConstant.LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                // 已有其他线程在处理，直接返回；AUTO 模式会自动确认该消息
                log.info("[Judge] 重复消费，直接返回, submissionId={}", message.getId());
                return;
            }

            Long submissionId = message.getId();
            log.info("[Judge] 获取锁成功，开始处理, submissionId={}", submissionId);
            doJudge(submissionId);
//            throw new RuntimeException("[Judge] 模拟异常，测试消息重试机制, submissionId=" + submissionId);
            log.info("[Judge] 处理成功, submissionId={}", submissionId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[Judge] 获取锁被中断, submissionId=" + message.getId(), e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("[Judge] 释放锁失败, submissionId={}, error: {}", message.getId(), e.getMessage());
                }
            }
        }

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
        LanguageConfigEnum languageEnum = LanguageConfigEnum.fromValue(language);
        // 2. 获取语言策略
        JudgeStrategy strategy = judgeStrategyFactory.getStrategy(languageEnum);

        strategy.doJudge(submission, problem);
    }
}
