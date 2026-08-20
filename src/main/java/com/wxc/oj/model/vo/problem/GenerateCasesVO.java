package com.wxc.oj.model.vo.problem;

import lombok.Data;

import java.util.List;

@Data
public class GenerateCasesVO {
    private Long problemId;
    /**
     * 是否已写入磁盘（全部成功才为 true）
     */
    private Boolean persisted;
    private Integer successCount;
    private Integer failCount;
    private List<GenerateCaseItemVO> cases;
}
