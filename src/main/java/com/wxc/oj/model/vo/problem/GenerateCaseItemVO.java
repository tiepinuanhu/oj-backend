package com.wxc.oj.model.vo.problem;

import lombok.Data;

@Data
public class GenerateCaseItemVO {
    private Integer index;
    private Boolean ok;
    private String output;
    private Long timeCost;
    private Long memoryUsed;
    private String status;
    private String error;
}
