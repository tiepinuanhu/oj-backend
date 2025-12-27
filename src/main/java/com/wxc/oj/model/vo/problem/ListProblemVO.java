package com.wxc.oj.model.vo.problem;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wxc.oj.model.judge.JudgeConfig;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Tag;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static cn.hutool.core.bean.BeanUtil.copyProperties;

/**
 * 用于列表展示题目的单个题目VO对象
 * @author wangxinchao
 * @date 2025/12/27 14:39
 */
@Data
public class ListProblemVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;


    private List<Tag> tags;

    private Integer level;

    private Integer submittedNum;

    private Integer acceptedNum;

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
