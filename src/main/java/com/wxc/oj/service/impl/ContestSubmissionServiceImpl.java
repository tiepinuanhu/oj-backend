package com.wxc.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.enums.submission.SubmissionLanguageEnum;
import com.wxc.oj.enums.submission.SubmissionStatusEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.mapper.ContestSubmissionMapper;
import com.wxc.oj.model.req.contest.ContestSubmissionListDTO;
import com.wxc.oj.model.req.contest.SubmitInContestDTO;
import com.wxc.oj.model.judge.JudgeCaseResult;
import com.wxc.oj.model.po.*;
import com.wxc.oj.model.submission.SubmissionResult;
import com.wxc.oj.model.vo.contest.ContestSubmissionVO;
import com.wxc.oj.model.queueMessage.SubmissionMessage;
import com.wxc.oj.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 王新超
* @description 针对表【contest_submission】的数据库操作Service实现
* @createDate 2025-05-06 14:30:36
*/
@Service
@Slf4j(topic = "ContestSubmissionServiceImpl🛴🛴🛴🛴🛴🛴🛴")
public class ContestSubmissionServiceImpl extends ServiceImpl<ContestSubmissionMapper, ContestSubmission>
    implements ContestSubmissionService{

    @Resource
    private RabbitTemplate rabbitTemplate;


    @Resource
    ContestSubmissionMapper mapper;
    @Resource
    ContestProblemService contestProblemService;

    public static final String ROUTING_KEY = "submission_routing_key";
    /**
     * 默认的直连交换机
     */
    public static final String EXCHANGE = "contest_exchange";

    @Resource
    private ProblemService problemService;


    @Resource
    private UserService userService;



    /**
     * 提交代码
     * 并生成submission到rocketmq
     * 因为用户提交代码后, 后端异步地调用判题服务, 所以此时给用户返回地判题结果为空
     * @return 插入的submission的id
     */
    @Override
    public ContestSubmissionVO submitCode(SubmitInContestDTO submitInContestDTO) {

        if (submitInContestDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(submitInContestDTO.getSourceCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }



        // 检查编程语言是否存在
        String language = submitInContestDTO.getLanguage();
        log.info("language = " + language);
        List<String> submissionLanguages = SubmissionLanguageEnum.getValues();
        if (!submissionLanguages.contains(language)) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }
        // 判断提交的题目是否存在
        Long problemId = submitInContestDTO.getProblemId();
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_EXIST);
        }

        // 创建并初始化待插入数据库的submission和submissionResult
        ContestSubmission submission = new ContestSubmission();
        SubmissionResult submissionResult = new SubmissionResult();

        submission.setContestId(submitInContestDTO.getContestId());
        submission.setProblemId(problemId);
        submission.setUserId(submitInContestDTO.getUserId());
        submission.setSourceCode(submitInContestDTO.getSourceCode());
        submission.setLanguage(submitInContestDTO.getLanguage());

        // 初始化判题状态为 NOT_SUBMITTED
//        submission.setStatus(SubmissionStatus.SUBMITTED.getStatus());

        submissionResult.setStatus(SubmissionStatusEnum.SUBMITTED.getStatus());
        submissionResult.setStatusDescription(SubmissionStatusEnum.SUBMITTED.getDescription());
        submissionResult.setScore(0);

        submission.setSubmissionResult(JSONUtil.toJsonStr(submissionResult));
        submission.setStatus(SubmissionStatusEnum.SUBMITTED.getStatus());
        submission.setStatusDescription(SubmissionStatusEnum.SUBMITTED.getDescription());
        submission.setScore(0);

        // 初始submission保存到数据库
        boolean save = this.save(submission);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long id1 = submission.getId();
        // 获取插入数据库后的submissionID
        ContestSubmission submission1 = this.getById(id1);
        // submission发送到消息队列后，submission的状态为 PENDING
        // ❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗❗
        Long id = submission1.getId();
        if (id != null) {
            SubmissionMessage submissionMessage = new SubmissionMessage();
            submissionMessage.setId(id);
            log.info("发送submissionId: " + id);
            //
            // 使用convertAndSend方法一步到位，参数基本和之前是一样的
            //最后一个消息本体可以是Object类型，真是大大的方便
            // 发送消息到直连交换机, 指定路由键
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, submissionMessage);
//            submission1.setStatus(SubmissionStatus.PENDING.getStatus());
            submissionResult.setStatus(SubmissionStatusEnum.PENDING.getStatus());
            submissionResult.setStatusDescription(SubmissionStatusEnum.PENDING.getDescription());
        }
        this.updateById(submission1);
        // 异步化了, 所以返回的还是刚开始的初始的submission
        ContestSubmission submission2 = this.getById(submission.getId());
        ContestSubmissionVO submissionVO = this.contestSubmissionToVO(submission2);
        return submissionVO;
    }

    @Override
    public ContestSubmissionVO contestSubmissionToVO(ContestSubmission contestSubmission) {
        Long contestId = contestSubmission.getContestId();
        ContestSubmissionVO contestSubmissionVO = new ContestSubmissionVO();
        BeanUtils.copyProperties(contestSubmission, contestSubmissionVO);
        // 将submissionResult的json字符串转换为对象
        String submissionResult = contestSubmission.getSubmissionResult();
        SubmissionResult bean = JSONUtil.toBean(submissionResult, SubmissionResult.class);
        String judgeCaseResultsStr = contestSubmission.getJudgeCaseResults();
        List<JudgeCaseResult> judgeCaseResults = JSONUtil.toList(judgeCaseResultsStr, JudgeCaseResult.class);
        contestSubmissionVO.setJudgeCaseResults(judgeCaseResults);
        contestSubmissionVO.setSubmissionResult(bean);


        Problem problem = problemService.getById(contestSubmission.getProblemId());
        contestSubmissionVO.setProblemTitle(problem.getTitle());
        contestSubmissionVO.setContestId(contestId);
        User user = userService.getById(contestSubmission.getUserId());
        contestSubmissionVO.setUserAccount(user.getUserAccount());

        contestSubmissionVO.setCodeLength(contestSubmission.getSourceCode().length());

        LambdaQueryWrapper<ContestProblem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problem.getId());
        ContestProblem contestProblem = contestProblemService.getOne(queryWrapper);
        if (contestProblem != null) {
            Integer pindex = contestProblem.getPindex();
            contestSubmissionVO.setProblemIndex(pindex);
        }


        return contestSubmissionVO;
    }


    /**
     * 根据Submission的DTO获取LambdaQueryWrapper
     * @return
     */
//    @Override
//    public LambdaQueryWrapper<Submission> getQueryWrapper(SubmissionQueryDTO submissionQueryDTO) {
//        var queryWrapper = new QueryWrapper<Submission>();
//        if (submissionQueryDTO == null) {
//            return queryWrapper.lambda();
//        }
//        Long problemId = submissionQueryDTO.getProblemId();
//        Long userId = submissionQueryDTO.getUserId();
//        String language = submissionQueryDTO.getLanguage();
//
////        String sortField = "createTime";
////        String sortOrder = CommonConstant.SORT_ORDER_DESC;
//
//        String judgeResult = submissionQueryDTO.getJudgeResult();
//
//        queryWrapper.eq(ObjectUtils.isNotEmpty(problemId), "problemId", problemId)
//                .eq(ObjectUtils.isNotEmpty(userId), "userId", userId)
//                .eq(StringUtils.isNotBlank(language), "language", language)
//                .like(StringUtils.isNotBlank(judgeResult), "judgeInfo", judgeResult);
//        return queryWrapper.lambda();
//    }
    /**
     * 从数据库中得submission
     * 进行数据脱敏和数据库id扩展
     */
//    @Override
//    public ContestSubmissionVO submissionToVO(ContestSubmission submission) {
//        ContestSubmissionVO contestSubmissionVO = new ContestSubmissionVO();
//
//        return submission;
//    }

    /**
     * 生成分页的VO对象
     * @return
     */
//    @Override
//    public Page<SubmissionVO> getSubmissionVOPage(Page<Submission> submissionPage) {
//        // 获取当前页面的所有submission
//        List<Submission> submissionList = submissionPage.getRecords();
//        // 创建一个空的页面 存储 submissionVO
//        Page<SubmissionVO> submissionVOPage = new Page<>(submissionPage.getCurrent(),
//                submissionPage.getSize(), submissionPage.getTotal());
//        // 不进行用户关联
//        List<SubmissionVO> submissionVOList = submissionList.stream().map(submission -> {
//            return submissionToVO(submission);
//        }).collect(Collectors.toList());
//        submissionVOPage.setRecords(submissionVOList);
//        return submissionVOPage;
//    }


    @Override
    public Page<ContestSubmissionVO> getContestSubmissionVOPageByContestSubmissionPage(
            Page<ContestSubmission> contestSubmissionPage) {
        List<ContestSubmission> records = contestSubmissionPage.getRecords();
        Page<ContestSubmissionVO> contestSubmissionVOPage = new Page<>(
                contestSubmissionPage.getCurrent(),contestSubmissionPage.getSize(),contestSubmissionPage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return contestSubmissionVOPage;
        }
        List<ContestSubmissionVO> contestSubmissionVOList = records.stream().map(contestSubmission -> {
            return contestSubmissionToVO(contestSubmission);
        }).collect(Collectors.toList());
        contestSubmissionVOPage.setRecords(contestSubmissionVOList);
        return contestSubmissionVOPage;
    }

    @Override
    public Page<ContestSubmissionVO> listSubmissions(ContestSubmissionListDTO contestSubmissionListDTO) {
        return null;
    }


    @Override
    public List<ContestSubmissionVO> listSubmissionsByContestId(Long contestId) {
        LambdaQueryWrapper<ContestSubmission> queryWrapper = new LambdaQueryWrapper<>();
        if (contestId != null) {
            queryWrapper.eq(ContestSubmission::getContestId, contestId);
        }
        queryWrapper.orderByDesc(ContestSubmission::getSubmissionTime);
        List<ContestSubmission> list = this.list(queryWrapper);
        List<ContestSubmissionVO> listVO = list.stream().map(contestSubmission -> {
            return contestSubmissionToVO(contestSubmission);
        }).collect(Collectors.toList());
        return listVO;
    }
    @Override
    public ContestSubmissionVO getContestSubmissionById(Long id) {
        ContestSubmission contestSubmission = this.getById(id);
        return contestSubmissionToVO(contestSubmission);
    }



    @Override
    public List<ContestSubmissionVO> getMaxScoreSubmissionsByContestAndProblem(Long contestId) {
        List<ContestSubmission> contestSubmissions
                = mapper.selectMaxScoreSubmissionsByContest(contestId);
        List<ContestSubmissionVO> contestSubmissionVOS = new ArrayList<>();
        for (ContestSubmission contestSubmission : contestSubmissions) {
            ContestSubmissionVO contestSubmissionVO = contestSubmissionToVO(contestSubmission);
            contestSubmissionVOS.add(contestSubmissionVO);
        }
        return contestSubmissionVOS;
    }
}




