package com.wxc.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxc.oj.model.req.problem.ProblemAddRequest;
import com.wxc.oj.model.req.problem.ProblemEditRequest;
import com.wxc.oj.model.req.problem.ProblemQueryRequest;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.model.vo.problem.ProblemVO;

import java.util.List;

/**
* @author 王新超
* @description 针对表【problem】的数据库操作Service
* @createDate 2024-02-28 14:24:47
*/
public interface ProblemService extends IService<Problem> {
    ProblemVO getProblemVOById(Long problemId);

    /**
     * 校验
     *
     * @param post
     * @param add
     */
    void validProblem(Problem post, boolean add);

    /**
     * 获取查询条件
     *
     * @param postQueryRequest
     * @return
     */
    LambdaQueryWrapper<Problem> getQueryWrapper(ProblemQueryRequest postQueryRequest);



    List<ProblemVO> getAllProblemNotPublic();

    ProblemVO problem2VO(Problem problem);

    Page<ListProblemVO> listProblemVO(ProblemQueryRequest problemQueryRequest);


    List<ListProblemVO> getProblemVOListByProblemList(List<Problem> problemList);

    ProblemVO editProblem(ProblemEditRequest request);


    Boolean addProblem(ProblemAddRequest request);
}
