package com.wxc.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.common.PageRequest;
import com.wxc.oj.model.req.contest.ContestAddRequest;
import com.wxc.oj.model.req.contest.ContestBaseUpdateRequest;
import com.wxc.oj.model.req.contest.ContestSubmissionListDTO;
import com.wxc.oj.model.req.contest.ContestUpdateRequest;
import com.wxc.oj.model.po.Contest;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxc.oj.model.po.ContestProblem;
import com.wxc.oj.model.vo.contest.ContestProblemVO;
import com.wxc.oj.model.vo.contest.ContestSubmissionVO;
import com.wxc.oj.model.vo.contest.ContestVO;
import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.model.vo.rank.RankListVO;

import java.util.List;

/**
* @author 王新超
* @description 针对表【contest】的数据库操作Service
* @createDate 2025-03-24 21:58:05
*/
public interface ContestService extends IService<Contest> {
    ContestVO updateContestBaseInfo(ContestBaseUpdateRequest request);

    ContestVO updateContestBaseInfo(ContestUpdateRequest request);

    boolean addContestWithBaseInfo(ContestAddRequest request);

    ContestVO getContestVOWithoutProblemListByContest(Contest contest);
    ContestVO getContestVOWithProblemListByContest(Contest contest);
    Page<ContestVO> getContestVOPage(PageRequest pageRequest);

    ContestVO getContestVOByContestId(Long contestId);


    boolean register(Long userId, Long contestId);

    Page<ContestSubmissionVO> listSubmissions(ContestSubmissionListDTO contestSubmissionListDTO);

    boolean cancelRegistration(Long userId, Long contestId);

    boolean findUserInContest(Long userId, Long contestId);

    boolean canRegister(Long userId, Long contestId);

    List<ListProblemVO> getProblemVOListByContestId(Long contestId);

    ContestProblemVO contestProblemToVO(ContestProblem contestProblem);


    List<ContestProblemVO> getContestProblemVOListByContestId(Long contestId, Long userId);

    ContestProblemVO getContestProblemByIndex(Long contestId, Integer index);

    RankListVO getRankList(Long contestId);
}
