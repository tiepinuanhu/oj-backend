package com.wxc.oj.model.vo.problem;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wxc.oj.model.judge.JudgeConfig;
import com.wxc.oj.model.po.Tag;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 返回给前端的关于Problem的数据封装类
 * 用户给前端表格展示的题目,不需要返回content大文本
 */
@Data
public class ProblemVO implements Serializable {


    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String content;

    private List<Tag> tags;

    private Integer level;

    private Integer submittedNum;

    private Integer acceptedNum;


    /**
     * 测试配置也要返回给前端用户
     * 不然用户怎么知道限制要求, 就不会约束自己
     */
    private JudgeConfig judgeConfig;


    /**
     * 只给前端返回到日期, 不要返回到具体时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone="GMT+8")
    private Date createTime;



    private Long publisherId;

    private String publisherName;

    private Boolean isPublic;

    private static final long serialVersionUID = 1L;
}