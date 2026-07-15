package com.wxc.oj.model.req.problem;

import com.wxc.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 基于 Elasticsearch 的题目全文检索请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProblemEsQueryRequest extends PageRequest implements Serializable {

    /**
     * 检索关键词（匹配 title + 题面合并后的 content 字段）
     */
    private String searchText;

    /**
     * 当前用户 id（用于权限：普通用户仅可见公开题）
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
