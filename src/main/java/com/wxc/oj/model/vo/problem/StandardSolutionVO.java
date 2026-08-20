package com.wxc.oj.model.vo.problem;

import lombok.Data;

@Data
public class StandardSolutionVO {
    private Long problemId;
    /**
     * 固定为 cpp
     */
    private String language;
    private String sourceCode;
}
