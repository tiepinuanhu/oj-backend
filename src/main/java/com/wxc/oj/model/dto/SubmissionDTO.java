package com.wxc.oj.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author wangxinchao
 * @date 2025/12/28 20:00
 */
@Data
public class SubmissionDTO {
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
