package com.wxc.oj.model.req.problem;

import com.wxc.oj.model.judge.JudgeConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 普通用户可以对一个题目的修改
 * 点赞数, 收藏数
 */
@Data
public class ProblemEditRequest implements Serializable {

    private Long id;

    private String title;

    private String content;

    private List<Integer> tags;

    private Integer level;

    private JudgeConfig judgeConfig;

    private Long userId;

    private Long publisherId;

    private Boolean isPublic;

    private static final long serialVersionUID = 1L;
}