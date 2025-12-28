package com.wxc.oj.model.req.contest;

import com.wxc.oj.model.req.submission.SubmissionAddRequest;
import lombok.Data;

@Data
public class SubmitInContestDTO extends SubmissionAddRequest {

    private Long contestId;
}
