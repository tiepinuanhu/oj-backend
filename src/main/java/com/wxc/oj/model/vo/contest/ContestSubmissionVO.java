package com.wxc.oj.model.vo.contest;


import com.wxc.oj.model.judge.JudgeCaseResult;
import com.wxc.oj.model.vo.submission.SubmissionVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ContestSubmissionVO extends SubmissionVO implements Serializable {
    private Long contestId;
    private Date submissionTime;
    private Integer problemIndex;

    private Integer status;
    private String statusDescription;
    private Integer score;
    private Long totalTime;
    private Long memoryUsed;
    private List<JudgeCaseResult> judgeCaseResults;
    private static final long serialVersionUID = 1L;
}
