package com.wxc.oj.model.req.contest;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ContestAddRequest implements Serializable {
    private String title;

    private String description;

    private Date startTime;

    private Integer duration;

    private Boolean isPublic;

    private Long hostId;

    private List<ContestProblemDTO> problems;

    private static final long serialVersionUID = 1L;
}
