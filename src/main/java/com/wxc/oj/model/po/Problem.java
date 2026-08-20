package com.wxc.oj.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.lettuce.core.output.ListOfGenericMapsOutput;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName problem
 */
@TableName(value ="problem")
@Data
public class Problem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;


    /**
     * 难度
     */
    private Integer level;


    /**
     * 提交数
     */
    private Integer submittedNum;
    /**
     * 通过数
     */
    private Integer acceptedNum;

    /**
     * 测试配置
     */
    private String judgeConfig;
    /**
     * 题目创建者
     */
    private Long userId;

    private Date createTime;

    private Date updateTime;


    private Integer isDeleted;

    private Integer isPublic;

    /**
     * C++ 标程源码（仅管理员可见/可改）
     */
    private String standardCode;

    private static final long serialVersionUID = 1L;
}