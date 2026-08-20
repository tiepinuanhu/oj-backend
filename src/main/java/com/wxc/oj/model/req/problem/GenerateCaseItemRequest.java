package com.wxc.oj.model.req.problem;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateCaseItemRequest implements Serializable {

    private Integer index;

    private String input;

    /**
     * 可选；未传时按均分满分 100 计算
     */
    private Integer fullScore;

    private static final long serialVersionUID = 1L;
}
