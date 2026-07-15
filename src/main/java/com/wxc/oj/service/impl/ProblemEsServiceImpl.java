package com.wxc.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.es.ProblemEsRepository;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.es.ProblemEsDocument;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.req.problem.ProblemEsQueryRequest;
import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.service.ProblemEsService;
import com.wxc.oj.service.ProblemService;
import com.wxc.oj.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题目 Elasticsearch 检索实现。
 * <p>
 * 检索流程：ES match 全文命中（按相关度排序）→ 取出题目 id → MySQL 补全详情 → 权限过滤 → 封装 ListProblemVO。
 * <p>
 * 索引侧：题目增删改时同步维护 ES 文档（saveOrUpdate / deleteById）。
 */
@Service
@Slf4j(topic = "ProblemEsServiceImpl")
public class ProblemEsServiceImpl implements ProblemEsService {

    /** ES 原生查询（NativeQuery / SearchHits） */
    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    /** ES Repository，用于文档增删改 */
    @Resource
    private ProblemEsRepository problemEsRepository;

    /** MySQL 题目服务，用于按 id 补全实体及转 VO */
    @Resource
    private ProblemService problemService;

    /** 用户服务，用于判断管理员可见范围 */
    @Resource
    private UserService userService;

    /**
     * 基于关键词的全文检索（匹配 ES 字段 content = title + 题面）。
     *
     * @param request 含 searchText、userId、分页参数；关键词与 userId 均必填
     * @return 与列表页一致的 Page&lt;ListProblemVO&gt;，顺序与 ES 相关度一致
     */
    @Override
    public Page<ListProblemVO> search(ProblemEsQueryRequest request) {
        // —— 参数校验 ——
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id未填写");
        }
        String searchText = StringUtils.trimToEmpty(request.getSearchText());
        if (StringUtils.isBlank(searchText)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        // Spring Data 分页从 0 开始；业务 current 从 1 开始
        int current = Math.max(request.getCurrent(), 1);
        int pageSize = Math.max(request.getPageSize(), 1);

        // —— 1. ES：对 content 做 match 全文检索，按相关度分页 ——
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m.field("content").query(searchText)))
                .withPageable(PageRequest.of(current - 1, pageSize))
                .build();

        SearchHits<ProblemEsDocument> hits = elasticsearchOperations.search(query, ProblemEsDocument.class);
        long total = hits.getTotalHits();

        Page<ListProblemVO> resultPage = new Page<>(current, pageSize, total);
        if (total == 0 || CollUtil.isEmpty(hits.getSearchHits())) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // —— 2. 保留 ES 返回的 id 顺序（相关度从高到低） ——
        List<Long> orderedIds = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProblemEsDocument::getId)
                .filter(Objects::nonNull)
                .toList();

        if (orderedIds.isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // —— 3. MySQL：批量查出题目实体，便于按 id 回填 ——
        List<Problem> problems = problemService.listByIds(orderedIds);
        Map<Long, Problem> problemMap = problems.stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity(), (a, b) -> a));

        // —— 4. 按相关度重排 + 可见性过滤（非管理员仅可见公开题） ——
        User user = userService.getById(request.getUserId());
        boolean isAdmin = user != null && user.getUserRole() != null && user.getUserRole() >= 1;

        List<Problem> orderedProblems = new ArrayList<>();
        for (Long id : orderedIds) {
            Problem problem = problemMap.get(id);
            // ES 与 MySQL 可能短暂不同步：ES 有 id、MySQL 已删 → 跳过
            if (problem == null) {
                continue;
            }
            // isPublic == 1 表示公开；普通用户不可见私有题
            if (!isAdmin && (problem.getIsPublic() == null || problem.getIsPublic() != 1)) {
                continue;
            }
            orderedProblems.add(problem);
        }

        // —— 5. 转为列表页 VO ——
        resultPage.setRecords(problemService.getProblemVOListByProblemList(orderedProblems));
        return resultPage;
    }

    /**
     * 将 MySQL 题目写入或更新到 ES（用于题目新增 / 编辑后的索引同步）。
     */
    @Override
    public void saveOrUpdate(Problem problem) {
        if (problem == null || problem.getId() == null) {
            return;
        }
        problemEsRepository.save(toEsDocument(problem));
    }

    /**
     * 按题目 id 删除 ES 文档（用于题目删除后的索引同步）。
     */
    @Override
    public void deleteById(Long problemId) {
        if (problemId == null) {
            return;
        }
        problemEsRepository.deleteById(problemId);
    }

    /**
     * MySQL Problem → ES 文档。
     * content 字段为 title 与题面正文的合并，供全文检索使用。
     */
    @Override
    public ProblemEsDocument toEsDocument(Problem problem) {
        ProblemEsDocument doc = new ProblemEsDocument();
        doc.setId(problem.getId());
        doc.setContent(mergeContent(problem.getTitle(), problem.getContent()));
        return doc;
    }

    /**
     * 合并标题与题面为一个检索字符串；任一方为空时仅保留另一方。
     */
    private String mergeContent(String title, String content) {
        String t = StringUtils.defaultString(title).trim();
        String c = StringUtils.defaultString(content).trim();
        if (t.isEmpty()) {
            return c;
        }
        if (c.isEmpty()) {
            return t;
        }
        return t + " " + c;
    }
}
