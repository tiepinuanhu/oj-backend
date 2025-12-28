package com.wxc.oj.model.req.contest;


import lombok.Data;


@Data
public class ContestUpdateRequest extends ContestAddRequest{

    private Long contestId;

}
