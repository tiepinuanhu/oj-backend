package com.wxc.oj.model.vo.submission;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.submission.SubmissionResult;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 返回给前端的关于Problem的数据封装类
 */
@Data
public class SubmissionVO implements Serializable {

    private static final long serialVersionUID = 1L;


    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long userId;

    private String userAccount;

    private Long problemId;

    private String problemTitle;

    private String sourceCode;

    /**
     * 从sourceCode计算长度 多少 B
     * 返回KB为单位
     */
    private Integer codeLength;


    private String language;

    private Date createTime;
    /**
     * 返回多组测试用例的判题信息
     * 查询某个submission时使用
     */
    private SubmissionResult submissionResult;
    /**
     * vo -> pojo
     */
    public static Submission voToObj(SubmissionVO submissionVO) {
        if (submissionVO == null) {
            return null;
        }
        Submission submission = new Submission();
        BeanUtils.copyProperties(submissionVO, submission);
        // 将submissionVO的JudgeInfo 转为 字符串 存到entity
        SubmissionResult submissionResult1 = submissionVO.getSubmissionResult();
        if (submissionResult1 != null) {
            submission.setSubmissionResult(JSONUtil.toJsonStr(submissionResult1));
        }
        return submission;
    }


    /**
     * pojo -> vo
     * codeLength 返回KB为单位
     * 数据库中的JSON字符串解析为对象
     */
    public static SubmissionVO objToVo(Submission submission) {
        if (submission == null) {
            return null;
        }
        SubmissionVO submissionVO = new SubmissionVO();
        // 核心❗❗❗
        BeanUtils.copyProperties(submission, submissionVO);

        submissionVO.setCodeLength(submission.getSourceCode().length());
        return submissionVO;
    }
}