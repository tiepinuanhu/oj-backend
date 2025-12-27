package com.wxc.oj.model.queueMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangxinchao
 * @date 2025/12/21 21:16
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionStatusMessage {
    private Long submissionId;
    private int status;
    private Long userId;
    private Long problemId;
}
