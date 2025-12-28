package com.wxc.oj.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.annotation.AuthCheck;
import com.wxc.oj.common.BaseResponse;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.common.PageRequest;
import com.wxc.oj.common.ResultUtils;
import com.wxc.oj.enums.contest.ContestStatusEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.req.contest.*;
import com.wxc.oj.model.po.Contest;
import com.wxc.oj.model.po.ContestProblem;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.vo.*;
import com.wxc.oj.model.vo.contest.ContestProblemSimpleVO;
import com.wxc.oj.model.vo.contest.ContestProblemVO;
import com.wxc.oj.model.vo.contest.ContestSubmissionVO;
import com.wxc.oj.model.vo.contest.ContestVO;
import com.wxc.oj.model.vo.rank.RankListVO;
import com.wxc.oj.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static com.wxc.oj.enums.UserRoleEnum.ADMIN;
import static org.springframework.beans.BeanUtils.copyProperties;
@RestController
@RequestMapping("contest")
@Slf4j(topic = "ContestController🤣🤣🤣🤣🤣")
public class ContestController {

    @Resource
    ContestService contestService;


    @Resource
    ContestProblemService contestProblemService;


    @Resource
    ContestSubmissionService contestSubmissionService;


    @Resource
    ProblemService problemService;


    @Resource
    UserService userService;

    /**
     * 创建比赛并从题库选择题目添加到比赛中
     * @param request
     * @return
     */
//    @PostMapping("add")
//    @AuthCheck(mustRole = ADMIN)
//    public BaseResponse<Boolean> addContest(@RequestBody ContestAddRequest request) {
//        contestService.contestInStatus_0(request);
//        return ResultUtils.success(true);
//    }


    @PostMapping("add")
    @AuthCheck(mustRole = ADMIN)
    public BaseResponse addContest(@RequestBody ContestAddRequest request) {
        contestService.addContestWithBaseInfo(request);
        return ResultUtils.success(true);
    }

    @PostMapping("update")
    @AuthCheck(mustRole = ADMIN)
    public BaseResponse<ContestVO> updateContest(@RequestBody ContestUpdateRequest request) {
        ContestVO contestVO = contestService.updateContestBaseInfo(request);
        return ResultUtils.success(contestVO);
    }
    @PostMapping("update/base")
    @AuthCheck(mustRole = ADMIN)
    public BaseResponse<ContestVO> updateContestBaseInfo(@RequestBody ContestBaseUpdateRequest request) {
        ContestVO contestVO = contestService.updateContestBaseInfo(request);
        return ResultUtils.success(contestVO);
    }

    /**
     *  修改比赛的题目
     * @param
     * @return
     * @Date
     */
    @PostMapping("UpdateContestProblem")
    @AuthCheck(mustRole = ADMIN)
    public BaseResponse updateContestProblems(@RequestBody ContestProblemUpdateRequest
                                                          request) {

        Long contestId = request.getContestId();
        List<ContestProblemDTO> problems = request.getProblems();
        // 删除该比赛的所有题目
        LambdaQueryWrapper<ContestProblem> q1 = new LambdaQueryWrapper<>();
        q1.eq(ContestProblem::getContestId, contestId);
        contestProblemService.remove(q1);
        // 添加题目到比赛中
        for (ContestProblemDTO problem : problems) {
            ContestProblem contestProblem = new ContestProblem();
            contestProblem.setContestId(contestId);
            contestProblem.setProblemId(problem.getProblemId());
            contestProblem.setFullScore(problem.getFullScore());
            contestProblem.setPindex(problem.getProblemIndex());
            contestProblemService.save(contestProblem);
        }
        return ResultUtils.success(true);
    }



    /**
     * 比赛报名接口
     * TODO:
     *  1. 验证contest的status == 0, 只能报名未开始的比赛
     *  2. 验证用户是否已经报名了该比赛, 避免重复报名
     *  3. 获取当前用户User的ID
     *  4. 创建比赛报名记录: ContestRegistration并插入数据库
     *  5. 返回ok
     *  用户比赛报名接口
     * @return
     */
    @PostMapping("register")
    public BaseResponse<?> register(@RequestBody RegisterDTO registerDTO) {
        boolean register = contestService.register(registerDTO.getUserId(), registerDTO.getContestId());
        return ResultUtils.success(register);
    }

    @DeleteMapping("cancelReg")
    public BaseResponse<?> cancelReg(@RequestBody RegisterDTO registerDTO) {
        boolean register = contestService.cancelRegistration(registerDTO.getUserId(), registerDTO.getContestId());
        return ResultUtils.success(register);
    }

    /**
     * 获取当前正在进行/比赛结束的比赛的用户排名
     * todo:
     *  1. 限制contest的status = 1 或 2
     *
     * @param contestId
     * @return
     */
    @GetMapping("playersRank")
    public BaseResponse<?> getPlayersRank(@RequestParam Long contestId) {

        return ResultUtils.success(null);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ContestVO>> listProblemVOByPage(@RequestBody PageRequest pageRequest) {
        Page<ContestVO> contestVOPage = contestService.getContestVOPage(pageRequest);
        return ResultUtils.success(contestVOPage);
    }

    @GetMapping("/get/vo")
    public BaseResponse<ContestVO> getContestVOById(@RequestParam("id") Long contestId) {
        ContestVO contestVO = contestService.getContestVOByContestId(contestId);
        return ResultUtils.success(contestVO);
    }

    @PostMapping("user/isReg")
    public BaseResponse<Boolean> isReg(@RequestBody RegisterDTO registerDTO) {
        boolean reg = contestService.findUserInContest(registerDTO.getUserId(),  registerDTO.getContestId());
        return ResultUtils.success(reg);
    }
    @PostMapping("user/canReg")
    public BaseResponse<Boolean> canReg(@RequestBody RegisterDTO registerDTO) {
        boolean reg = contestService.canRegister(registerDTO.getUserId(),  registerDTO.getContestId());
        return ResultUtils.success(reg);
    }


    /**
     * 获取某个contest下的所有题目
     * @param contestId
     * @return
     */
    @GetMapping("problems")
    public BaseResponse<List<ContestProblemVO>> getContestProblems(@RequestParam Long contestId, @
            RequestParam Long userId) {
        List<ContestProblemVO> problemVOListByContestId
                = contestService.getContestProblemVOListByContestId(contestId, userId);
        return ResultUtils.success(problemVOListByContestId);
    }



    @GetMapping("problem/get")
    public BaseResponse<ContestProblemVO> getContestProblemByIndex(
                                    @RequestParam Long contestId,
                                    @RequestParam Integer index) {
        ContestProblemVO contestProblemVO
                = contestService.getContestProblemByIndex(contestId, index);
        return ResultUtils.success(contestProblemVO);
    }


    /**
     * todo:
     *      根据contestId获取比赛现有题目列表
     *      返回题目的id，index，title，publisherId，发布人名称， 发布时间
     * @param contestId
     * @return
     */
    @GetMapping("/problemss")
    public BaseResponse<List<ContestProblemSimpleVO>> getProblemsByContestId(@RequestParam Long contestId) {
        LambdaQueryWrapper<ContestProblem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContestProblem::getContestId, contestId)
                .select(ContestProblem::getProblemId,
                        ContestProblem::getPindex,
                        ContestProblem::getProblemId,
                        ContestProblem::getFullScore)
                .orderByAsc(ContestProblem::getPindex);
        List<ContestProblem> contestProblemList = contestProblemService.list(queryWrapper);
        List<ContestProblemSimpleVO> problemVOList = new ArrayList<>();
        for (ContestProblem contestProblem : contestProblemList) {
            Long problemId = contestProblem.getProblemId();
            Problem problem = problemService.getById(problemId);
            Long userId = problem.getUserId();
            User publisher = userService.getById(userId);

            ContestProblemSimpleVO contestProblemVO = new ContestProblemSimpleVO();
            // modify
            contestProblemVO.setProblemId(problemId);
            contestProblemVO.setProblemIndex(contestProblem.getPindex());
            contestProblemVO.setFullScore(contestProblem.getFullScore());
            contestProblemVO.setTitle(problem.getTitle());
            contestProblemVO.setPublisherName(publisher.getUserAccount());
            contestProblemVO.setPublisherId(publisher.getId());
            contestProblemVO.setCreateTime(problem.getCreateTime());
            contestProblemVO.setIsPublic(problem.getIsPublic());
            problemVOList.add(contestProblemVO);
        }
        return ResultUtils.success(problemVOList);
    }



    /**
     * 返回用户对于比赛的权限
     * @param contestId
     * @param userId
     * @return
     */
    @GetMapping("auth")
    public BaseResponse<UserAuthInContestVO> getUserAuthInContest(@RequestParam Long contestId, @RequestBody Long userId) {
        return null;
    }


    /**
     * 在比赛中提交代码
     * todo:
     *  1. 只有在比赛进行中才能提交代码
     */
    @PostMapping("problem/submit")
    public BaseResponse<ContestSubmissionVO> submit(
                                    @RequestBody SubmitInContestDTO submitInContestDTO) {
        Long contestId = submitInContestDTO.getContestId();
        Contest contest = contestService.getById(contestId);
        if (contest.getStatus() != ContestStatusEnum.RUNNING.getCode()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,  "不能提交");
        }
        ContestSubmissionVO contestSubmissionVO
                = contestSubmissionService.submitCode(submitInContestDTO);
        return ResultUtils.success(contestSubmissionVO);
    }



    /**
     * 获取一个contest的所有submission
     * @param
     * @return
     */
    @PostMapping("submissions")
    public BaseResponse<Page<ContestSubmissionVO>> getContestSubmissions(
            @RequestBody ContestSubmissionListDTO contestSubmissionListDTO) {
        Page<ContestSubmissionVO> ans = contestService.listSubmissions(contestSubmissionListDTO);
        return ResultUtils.success(ans);
    }

    @GetMapping("submission/get")
    public BaseResponse<ContestSubmissionVO> getContestSubmissionById(@RequestParam Long id) {
        ContestSubmissionVO contestSubmissionVO = contestSubmissionService.getContestSubmissionById(id);
        return ResultUtils.success(contestSubmissionVO);
    }


    @GetMapping("rank")
    public BaseResponse<RankListVO> getContestRank(@RequestParam Long contestId) {
        RankListVO rankList = contestService.getRankList(contestId);
        return ResultUtils.success(rankList);
    }
}


