package com.wxc.oj.model.req.problem;

import com.wxc.oj.model.judge.JudgeConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新请求
 * 管理员可以对题目的修改
 */
@Data
public class ProblemUpdateRequest implements Serializable {


    private Long id;

    private String title;

    private String content;

    private List<String> tags;

    private String level;

    private String solution;


//    private List<JudgeCase> judgeCase;

    private JudgeConfig judgeConfig;

    private static final long serialVersionUID = 1L;
}