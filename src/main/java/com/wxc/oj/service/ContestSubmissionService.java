package com.wxc.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.model.req.contest.ContestSubmissionListDTO;
import com.wxc.oj.model.req.contest.SubmitInContestDTO;
import com.wxc.oj.model.po.ContestSubmission;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxc.oj.model.vo.contest.ContestSubmissionVO;

import java.util.List;

/**
* @author 王新超
* @description 针对表【contest_submission】的数据库操作Service
* @createDate 2025-05-06 14:30:36
*/
public interface ContestSubmissionService extends IService<ContestSubmission> {

    ContestSubmissionVO submitCode(SubmitInContestDTO submitInContestDTO);

    ContestSubmissionVO contestSubmissionToVO(ContestSubmission submission);

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


    Page<ContestSubmissionVO> getContestSubmissionVOPageByContestSubmissionPage(
            Page<ContestSubmission> contestSubmissionPage);

    Page<ContestSubmissionVO> listSubmissions(ContestSubmissionListDTO contestSubmissionListDTO);

    List<ContestSubmissionVO> listSubmissionsByContestId(Long contestId);

    ContestSubmissionVO getContestSubmissionById(Long id);

    List<ContestSubmissionVO> getMaxScoreSubmissionsByContestAndProblem(Long contestId);
}
