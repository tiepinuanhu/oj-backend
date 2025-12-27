package com.wxc.oj.model.vo.dayRank;

import lombok.Data;

import java.io.Serializable;

@Data
public class DailyRankVO {
    private Integer rank;     // 排名
    private Long userId;      // 用户 ID
    private String userAccount;
    private Integer acCount;  // 当天 AC 数
}
