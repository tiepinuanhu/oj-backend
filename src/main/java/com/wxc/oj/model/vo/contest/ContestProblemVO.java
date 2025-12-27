package com.wxc.oj.model.vo.contest;

import com.wxc.oj.model.vo.problem.ProblemVO;
import lombok.Data;

import java.io.Serializable;

@Data
public class ContestProblemVO extends ProblemVO implements Serializable  {

    private Integer pindex;



    private Integer gainScore;
    /**
     * 比赛中题目的满分分值
     */
    private Integer fullScore;


    private static final long serialVersionUID = 1L;
}
