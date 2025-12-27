package com.wxc.oj.model.vo.submission;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wxc.oj.model.submission.SubmissionResult;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对SubmissionVO字段精简，仅返回部分字段
 * @author wangxinchao
 * @date 2025/12/27 15:37
 */
@Data
public class ListSubmissionVO implements Serializable {
    private static final long serialVersionUID = 1L;


    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long userId;

    private String userAccount;

    private Long problemId;

    private String problemTitle;


    private Integer codeLength;

    private String language;

    private Date createTime;




    private Integer status;

    private String statusDescription;

    private Integer score;


    private Long totalTime;

    private Long memoryUsed;
}
