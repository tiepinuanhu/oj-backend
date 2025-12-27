package com.wxc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.constant.RabbitConstant;
import com.wxc.oj.constant.RedisConstant;
import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import com.wxc.oj.enums.submission.SubmissionStatusEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.mapper.SubmissionMapper;
import com.wxc.oj.model.queueMessage.SubmissionStatusMessage;
import com.wxc.oj.model.submission.SubmissionResult;
import com.wxc.oj.model.queueMessage.SubmissionMessage;
import com.wxc.oj.model.dto.submission.SubmissionAddRequest;
import com.wxc.oj.model.dto.submission.SubmissionQueryDTO;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.vo.submission.ListSubmissionVO;
import com.wxc.oj.model.vo.submission.ProblemStatisticsVO;
import com.wxc.oj.model.vo.submission.SubmissionStatusCount;
import com.wxc.oj.service.SubmissionService;
import com.wxc.oj.model.vo.problem.ProblemVO;
import com.wxc.oj.model.vo.submission.SubmissionVO;
import com.wxc.oj.model.vo.UserVO;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.service.UserService;
import com.wxc.oj.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static cn.hutool.core.bean.BeanUtil.copyProperties;
import static com.wxc.oj.constant.RedisConstant.AC_PROBLEMS_KEY;
import static com.wxc.oj.constant.RedisConstant.AC_RANK_KEY;

/**
* @author 王新超
* @description 针对表【submission】的数据库操作Service实现
* @createDate 2024-02-28 10:33:17
*/
@Service
@Slf4j(topic = "💕💕💕")
public class SubmissionServiceImpl extends ServiceImpl<SubmissionMapper, Submission> implements SubmissionService {


    @Resource
    RabbitTemplate rabbitTemplate;
    @Resource
    ProblemService problemService;
    @Resource
    UserService userService;
    @Resource
    SubmissionMapper submissionMapper;


    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;



    @Override
    public ProblemStatisticsVO getProblemStatisticsVO(Long problemId) {
        ProblemStatisticsVO problemStatisticsVO = new ProblemStatisticsVO();
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        List<SubmissionStatusCount> statusDistribution
                = submissionMapper.getStatusDistribution(problemId);

        problemStatisticsVO.setResultDistributions(statusDistribution);
        problemStatisticsVO.setProblemId(problemId);
        problemStatisticsVO.setSubmittedCount(problem.getSubmittedNum());

        Map<Integer, Integer> map = new HashMap<>();
        LambdaQueryWrapper<Submission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Submission::getProblemId, problemId);
        List<Submission> submissions = this.list(queryWrapper);
        for (Submission submission : submissions) {
            String submissionResultStr = submission.getSubmissionResult();
            SubmissionResult submissionResult =
                    JSONUtil.toBean(submissionResultStr, SubmissionResult.class);
            Long totalTime = submissionResult.getTotalTime();
            if (submissionResult.getStatus() != SubmissionStatusEnum.ACCEPTED.getStatus()) {
                continue;
            }
            int score = (int) (totalTime / 100);
            map.put(score, map.getOrDefault(score, 0) + 1);
        }
        List<Integer> timeCount = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            if (map.containsKey(i) && map.get(i) > 0) {
                timeCount.add(map.get(i));
            } else {
                timeCount.add(0);
            }
        }
        Collections.reverse(timeCount);
        problemStatisticsVO.setTimeCount(timeCount);
        return problemStatisticsVO;
    }

    @Override
    public Page<ListSubmissionVO> listByPage(SubmissionQueryDTO submissionQueryDTO) {
        long current = submissionQueryDTO.getCurrent();
        long size = submissionQueryDTO.getPageSize();
        Long problemId = submissionQueryDTO.getProblemId();
        Long userId = submissionQueryDTO.getUserId();
        String language = submissionQueryDTO.getLanguage();
        String judgeResult = submissionQueryDTO.getJudgeResult();
        log.info("❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗judge = " + judgeResult);

        LambdaQueryWrapper<Submission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjectUtils.isNotEmpty(problemId), Submission::getProblemId, problemId)
                .eq(ObjectUtils.isNotEmpty(userId), Submission::getUserId, userId)
                .eq(StringUtils.isNotBlank(language), Submission::getLanguage, language)
                .eq(StringUtils.isNotEmpty(judgeResult) && !judgeResult.equals("不限结果"),Submission::getStatusDescription, judgeResult);
        queryWrapper.orderByDesc(Submission::getCreateTime);
        Page<Submission> submissionPage = this.page(new Page<>(current, size), queryWrapper);
        Page<ListSubmissionVO> submissionVOPage = this.getSubmissionVOPage(submissionPage);
        return submissionVOPage;
    }


    /**
     * 监听ac.rank.queue
     * 根据提交结果修改用户AC统计信息
     * @param submissionStatusMessage
     */
    @RabbitListener(queues = RabbitConstant.SUBMISSION_STATUS_AC_QUEUE, messageConverter = "jacksonConverter")
    public void changeRank(SubmissionStatusMessage submissionStatusMessage) {
        log.info("收到消息");
        int status = submissionStatusMessage.getStatus();
        if (status != SubmissionStatusEnum.ACCEPTED.getStatus()) {
            return;
        }
        Long userId = submissionStatusMessage.getUserId();
        Long problemId = submissionStatusMessage.getProblemId();
        String currentDateStr = DateUtils.getCurrentDateStr();

        String dedupKey = AC_PROBLEMS_KEY + currentDateStr + ":" + userId;

        Boolean firstAc = redisTemplate.opsForSet()
                .add(dedupKey, problemId.toString()) == 1;

        if (!firstAc) {
            return; // 已经 AC 过，不计数
        }
        String rankKey = AC_RANK_KEY + currentDateStr;
        redisTemplate.opsForZSet()
                .incrementScore(rankKey, userId, 1);
//        redisTemplate.expire(dedupKey, Duration.ofDays(1));
    }


    /**
     * 根据提交结果修改的统计信息
     * 统计信息包括：题目的提交数和通过数，用户通过的题目和AC的数量
     * 会有并发问题吧：
     */
    @RabbitListener(queues = RabbitConstant.SUBMISSION_STATUS_QUEUE, messageConverter = "jacksonConverter")
    public void changeProblem(SubmissionStatusMessage submissionStatusMessage) {
        log.info("收到消息");
        Long problemId = submissionStatusMessage.getProblemId();
        Long submissionId = submissionStatusMessage.getSubmissionId();

        Submission submission = this.getById(submissionId);
        SubmissionVO submissionVO = this.submissionToVO(submission);
        SubmissionResult submissionResult = submissionVO.getSubmissionResult();
        if (submissionResult == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer score = submissionResult.getScore();
        if (score == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 更新题目信息
        Problem problem = problemService.getById(problemId);
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        if (score == 100) {
            // 这块可能有并发问题, 如果大量用户对同一个题目提交，都是正确答案
            // score都是100，最终增加的AC数量可能会有误
            updateWrapper.set(Problem::getAcceptedNum, problem.getAcceptedNum() + 1)
                        .set(Problem::getSubmittedNum, problem.getSubmittedNum() + 1);
        } else {
            updateWrapper.eq(Problem::getId, problemId)
                    .set(Problem::getSubmittedNum, problem.getSubmittedNum() + 1);
        }
        problemService.update(updateWrapper);
        stringRedisTemplate.delete(RedisConstant.CACHE_PROBLEM_KEY + problem.getId());
    }



    /**
     * 提交代码
     * 并生成submission到rocketmq
     * 因为用户提交代码后, 后端异步地调用判题服务, 所以此时给用户返回地判题结果为空
     * @param submissionAddRequest
     * @param
     * @return 插入的submission的id
     */
    @Override
    public SubmissionVO submitCode(SubmissionAddRequest submissionAddRequest) {
        if (submissionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(submissionAddRequest.getSourceCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查编程语言是否存在
        String language = submissionAddRequest.getLanguage();
        log.info("language = " + language);
        List<String> submissionLanguages = SubmissionLanguageEnum.getValues();
        if (!submissionLanguages.contains(language)) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }
        // 判断提交的题目是否存在
        Long problemId = submissionAddRequest.getProblemId();
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_EXIST);
        }

        // 创建并初始化待插入数据库的submission和submissionResult
        Submission submission = new Submission();
        SubmissionResult submissionResult = new SubmissionResult();

        submission.setProblemId(problemId);
        submission.setUserId(submissionAddRequest.getUserId());
        submission.setSourceCode(submissionAddRequest.getSourceCode());
        submission.setLanguage(submissionAddRequest.getLanguage());

        // 初始化判题状态为 NOT_SUBMITTED

        submissionResult.setStatus(SubmissionStatusEnum.SUBMITTED.getStatus());
        submissionResult.setStatusDescription(SubmissionStatusEnum.SUBMITTED.getDescription());
        submissionResult.setScore(0);

        submission.setSubmissionResult(JSONUtil.toJsonStr(submissionResult));

        // 初始submission保存到数据库
        boolean save = this.save(submission);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }

        // 获取插入数据库后的submissionID
        Submission submission1 = this.getById(submission.getId());
        // submission发送到消息队列后，submission的状态为 PENDING
        // ❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗
        Long id = submission1.getId();
        if (id != null) {
            SubmissionMessage submissionMessage = new SubmissionMessage();
            submissionMessage.setId(id);
            rabbitTemplate.convertAndSend(RabbitConstant.SUBMISSION_EXCHANGE, RabbitConstant.SUBMISSION_ROUTING_KEY,
                    submissionMessage);
            submissionResult.setStatus(SubmissionStatusEnum.PENDING.getStatus());
            submissionResult.setStatusDescription(SubmissionStatusEnum.PENDING.getDescription());
        }
        this.updateById(submission1);
        // 异步化了, 所以返回的还是刚开始的初始的submission
        Submission submission2 = this.getById(submission.getId());
        SubmissionVO submissionVO = this.submissionToVO(submission2);
        return submissionVO;
    }


    /**
     * 从数据库中得submission
     * 进行数据脱敏和数据库id扩展
     */
    @Override
    public SubmissionVO submissionToVO(Submission submission) {
        // pojo的基础数据映射
        SubmissionVO submissionVO = SubmissionVO.objToVo(submission);
        // 解析pojo对象的JSON字符串为对象
        String submissionResultStr = submission.getSubmissionResult();
        SubmissionResult submissionResult = JSONUtil.toBean(submissionResultStr, SubmissionResult.class);
        submissionVO.setSubmissionResult(submissionResult);

        // 设置submission的题目具体信息
        Problem byId = problemService.getById(submissionVO.getProblemId());
        ProblemVO problemVO = problemService.problem2VO(byId);
        submissionVO.setProblemId(problemVO.getId());
        submissionVO.setProblemTitle(problemVO.getTitle());

        // 设置submission的提交User信息
        User user = userService.getById(submissionVO.getUserId());
        UserVO userVO = UserVO.objToVo(user);
        submissionVO.setUserId(userVO.getId());
        submissionVO.setUserAccount(userVO.getUserAccount());

        return submissionVO;
    }

    /**
     * 生成分页的VO对象
     * @return
     */
    @Override
    public Page<ListSubmissionVO> getSubmissionVOPage(Page<Submission> submissionPage) {
        // 获取当前页面的所有submission
        List<Submission> submissionList = submissionPage.getRecords();
        // 创建一个空的页面 存储 submissionVO
        Page<ListSubmissionVO> submissionVOPage = new Page<>(submissionPage.getCurrent(),
                submissionPage.getSize(), submissionPage.getTotal());
        // 不进行用户关联
        List<ListSubmissionVO> submissionVOList = submissionList.stream().map(submission -> {
            return this.submission2ListVO(submission);
        }).collect(Collectors.toList());
        submissionVOPage.setRecords(submissionVOList);
        return submissionVOPage;
    }


    public ListSubmissionVO submission2ListVO(Submission submission) {
        ListSubmissionVO listSubmissionVO = new ListSubmissionVO();
        copyProperties(submission, listSubmissionVO);
        Long problemId = submission.getProblemId();
        Problem problem = problemService.getById(problemId);
        String userAccount = userService.getById(submission.getUserId()).getUserAccount();
        listSubmissionVO.setProblemTitle(problem.getTitle());
        listSubmissionVO.setProblemId(problemId);
        listSubmissionVO.setUserAccount(userAccount);
        String submissionResult = submission.getSubmissionResult();
        listSubmissionVO.setCodeLength(submission.getSourceCode().length());
        SubmissionResult result = JSONUtil.toBean(submissionResult, SubmissionResult.class);
        listSubmissionVO.setScore(result.getScore());
        listSubmissionVO.setTotalTime(result.getTotalTime());
        listSubmissionVO.setMemoryUsed(result.getMemoryUsed());
        return listSubmissionVO;
    }
}




