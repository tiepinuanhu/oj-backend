package com.wxc.oj.model.req.problem;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GenerateCasesRequest implements Serializable {

    private Long problemId;

    /**
     * 默认 true；false 且目录已有 config.json 时拒绝覆盖
     */
    private Boolean overwrite;

    private List<GenerateCaseItemRequest> cases;

    private static final long serialVersionUID = 1L;
}
