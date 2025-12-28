package com.wxc.oj.model.req.contest;

import lombok.Data;

@Data
public class ContestBaseUpdateRequest {
    private Long contestId;

    private String title;

    private String description;

//    private Date startTime;

    private Integer duration;

    private Boolean isPublic;

    private Long hostId;
}
