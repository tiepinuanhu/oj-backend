package com.wxc.oj.model.vo.contest;

import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.model.vo.problem.ProblemVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @TableName contest
 */
@Data
public class ContestVO implements Serializable {

    private Long id;

    private String title;

    private String description;


    private Date startTime;

    /**
     * 数据库中不存在次字段，通过计算得出
     */
    private Date endTime;
    /**
     * 返回单位秒
     */
    private Integer duration;

    private Integer status;

    private Boolean isPublic;

    /**
     * 题目列表
     */
    private List<ListProblemVO> problemVOList;

    private Integer playerCount;

    private String hostName;

    private Long  hostId;


    private Integer canRegister;

    /**
     * 用来保存当前登录用户是否报名过该比赛
     */
    private boolean isRegistered;

    private static final long serialVersionUID = 1L;
}