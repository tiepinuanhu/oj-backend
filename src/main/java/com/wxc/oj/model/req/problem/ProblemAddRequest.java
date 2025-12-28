package com.wxc.oj.model.req.problem;

import com.wxc.oj.model.judge.JudgeConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ProblemAddRequest implements Serializable {
    private String title;

    private String content;

    private List<Integer> tags;

    private Integer level;

    private JudgeConfig judgeConfig;

    private Long publisherId;

    private Boolean isPublic;

    private static final long serialVersionUID = 1L;
}
