package com.wxc.oj.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName submission
 */
@TableName(value ="submission")
@Data
public class Submission implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long problemId;

    private String sourceCode;
    /**
     * JSON字符串
     */
    private String submissionResult;
    private Integer status;

    private Integer codeLength;

    private Integer score;

    private String statusDescription;

    private String language;

    private Date createTime;

    private Date updateTime;

    private Integer isDeleted;

    private Long memoryUsed;

    private Long totalTime;

    private static final long serialVersionUID = 1L;
}