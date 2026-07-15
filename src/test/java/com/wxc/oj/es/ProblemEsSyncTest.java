package com.wxc.oj.es;

import com.wxc.oj.model.es.ProblemEsDocument;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.service.ProblemService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 将 MySQL 中的题目同步到 Elasticsearch。
 * content = title + 题面正文（合并全文检索字段）。
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
class ProblemEsSyncTest {

    @Resource
    private ProblemService problemService;

    @Resource
    private ProblemEsRepository problemEsRepository;

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void ensureIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProblemEsDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
    }

    @Test
    void syncProblemsFromMysqlToEs() {
        List<Problem> problems = problemService.list();
        assertFalse(problems.isEmpty(), "MySQL 中没有题目数据，无法同步");

        List<ProblemEsDocument> docs = new ArrayList<>(problems.size());
        for (Problem problem : problems) {
            docs.add(toEsDocument(problem));
        }

        problemEsRepository.saveAll(docs);
        elasticsearchOperations.indexOps(ProblemEsDocument.class).refresh();

        long esCount = problemEsRepository.count();
        System.out.println("MySQL problem count = " + problems.size());
        System.out.println("ES document count = " + esCount);
        assertEquals(problems.size(), esCount);

        // 抽样打印一条，便于人工核对
        ProblemEsDocument sample = docs.get(0);
        System.out.println("sample id = " + sample.getId());
        System.out.println("sample content = " + StringUtils.abbreviate(sample.getContent(), 200));
    }

    private ProblemEsDocument toEsDocument(Problem problem) {
        ProblemEsDocument doc = new ProblemEsDocument();
        doc.setId(problem.getId());
        doc.setContent(mergeContent(problem.getTitle(), problem.getContent()));
        return doc;
    }

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
