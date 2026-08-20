package com.wxc.oj.model.problem;

import lombok.Data;

/**
 * 题目标程，持久化为 ans/{problemId}/standard.json
 */
@Data
public class StandardSolution {
    private String language;
    private String sourceCode;
}
