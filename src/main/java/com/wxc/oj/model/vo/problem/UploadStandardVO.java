package com.wxc.oj.model.vo.problem;

import lombok.Data;

@Data
public class UploadStandardVO {
    private Long problemId;
    private String language;
    private Boolean saved;
    /**
     * 若目录已有 .in，是否已用新标程重跑并落盘 .out
     */
    private Boolean outsRegenerated;
    /**
     * 有已有样例但重跑未全部成功时的明细
     */
    private GenerateCasesVO regenerateResult;
}
