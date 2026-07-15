package com.wxc.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.model.es.ProblemEsDocument;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.req.problem.ProblemEsQueryRequest;
import com.wxc.oj.model.vo.problem.ListProblemVO;

/**
 * 题目 Elasticsearch 检索 / 同步
 */
public interface ProblemEsService {

    /**
     * 全文检索题目，按相关度分页，返回与列表页一致的 VO
     */
    Page<ListProblemVO> search(ProblemEsQueryRequest request);

    /**
     * 将 MySQL 题目写入 / 更新到 ES
     */
    void saveOrUpdate(Problem problem);

    /**
     * 按题目 id 删除 ES 文档
     */
    void deleteById(Long problemId);

    ProblemEsDocument toEsDocument(Problem problem);
}
