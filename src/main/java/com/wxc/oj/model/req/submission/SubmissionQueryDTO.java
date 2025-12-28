package com.wxc.oj.model.req.submission;
import com.wxc.oj.common.PageRequest;
import lombok.Data;

import java.io.Serializable;


/**
 * 查询提交的DTO
 * 供前端进行筛选查询，可以根据
 * submission的ID，用户，语言，评测结果（ACCEPTED, WRONG ANSWER...）
 */
@Data
public class SubmissionQueryDTO extends PageRequest implements Serializable {

    private Long problemId;

    private Long userId;

    private String language;

    private String judgeResult;

    private static final long serialVersionUID = 1L;
}
