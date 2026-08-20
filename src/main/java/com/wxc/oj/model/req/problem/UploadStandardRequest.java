package com.wxc.oj.model.req.problem;

import lombok.Data;

import java.io.Serializable;

@Data
public class UploadStandardRequest implements Serializable {

    private Long problemId;

    /**
     * C++ 标程源码（语言固定为 cpp，无需传 language）
     */
    private String sourceCode;

    private static final long serialVersionUID = 1L;
}
