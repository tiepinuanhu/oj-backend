package com.wxc.oj.model.req.contest;

import com.wxc.oj.common.PageRequest;
import lombok.Data;

import java.io.Serializable;


@Data
public class ContestSubmissionListDTO extends PageRequest implements Serializable {
    private Long contestId;
    private Long userId;
    private Boolean selfOnly;
    private static final long serialVersionUID = 1L;
}
