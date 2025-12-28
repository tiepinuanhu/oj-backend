package com.wxc.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.model.req.submission.SubmissionAddRequest;
import com.wxc.oj.model.req.submission.SubmissionQueryDTO;
import com.wxc.oj.model.po.Submission;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxc.oj.model.vo.submission.ListSubmissionVO;
import com.wxc.oj.model.vo.submission.ProblemStatisticsVO;
import com.wxc.oj.model.vo.submission.SubmissionVO;

/**
* @author 王新超
* @description 针对表【submission】的数据库操作Service
* @createDate 2024-02-28 10:33:17
*/
public interface SubmissionService extends IService<Submission> {


    ProblemStatisticsVO getProblemStatisticsVO(Long problemId);

    Page<ListSubmissionVO> listByPage(SubmissionQueryDTO submissionQueryDTO);

    SubmissionVO submitCode(SubmissionAddRequest submissionAddRequest);



    SubmissionVO submissionToVO(Submission submission);

    Page<ListSubmissionVO> getSubmissionVOPage(Page<Submission> submissionPage);
}
