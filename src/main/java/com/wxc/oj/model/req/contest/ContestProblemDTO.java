package com.wxc.oj.model.req.contest;


import lombok.Data;


@Data
public class ContestProblemDTO {
    private Long problemId;
    private Integer problemIndex;
    private Integer fullScore;
}
