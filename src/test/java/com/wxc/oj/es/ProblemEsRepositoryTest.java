package com.wxc.oj.es;

import com.wxc.oj.model.es.ProblemEsDocument;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
class ProblemEsRepositoryTest {

    private static final Long TEST_ID = 999999L;

    @Resource
    private ProblemEsRepository problemEsRepository;

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void setUpIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProblemEsDocument.class);
        // 按 @Document / @Field 创建索引并写入 mapping；已存在则跳过
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
    }

    @AfterEach
    void cleanup() {
        problemEsRepository.deleteById(TEST_ID);
    }

    @Test
    void testSaveAndFindById() {
        ProblemEsDocument doc = buildDoc("Two Sum Given an array of integers, return indices of two numbers.");
        problemEsRepository.save(doc);

        Optional<ProblemEsDocument> found = problemEsRepository.findById(TEST_ID);
        assertTrue(found.isPresent());
        assertEquals("Two Sum Given an array of integers, return indices of two numbers.", found.get().getContent());
        System.out.println("saved doc = " + found.get());
    }

    @Test
    void testFindByContentContaining() {
        problemEsRepository.save(buildDoc("Two Sum easy problem"));

        List<ProblemEsDocument> list = problemEsRepository.findByContentContaining("Two Sum");
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(d -> TEST_ID.equals(d.getId())));
        System.out.println("findByContentContaining = " + list);
    }

    @Test
    void testSearchByContent() {
        problemEsRepository.save(buildDoc("Two Sum Given an array of integers nums and an integer target."));
        // 等待刷新，保证可检索
        elasticsearchOperations.indexOps(ProblemEsDocument.class).refresh();

        Criteria criteria = new Criteria("content").matches("Sum");
        SearchHits<ProblemEsDocument> hits = elasticsearchOperations.search(
                new CriteriaQuery(criteria),
                ProblemEsDocument.class
        );

        List<ProblemEsDocument> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        assertTrue(results.stream().anyMatch(d -> TEST_ID.equals(d.getId())));
        System.out.println("searchByContent = " + results);
    }

    @Test
    void testNativeMatchQuery() {
        problemEsRepository.save(buildDoc("Binary Search Implement binary search algorithm."));
        elasticsearchOperations.indexOps(ProblemEsDocument.class).refresh();

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m.field("content").query("binary search")))
                .build();

        SearchHits<ProblemEsDocument> hits = elasticsearchOperations.search(query, ProblemEsDocument.class);
        assertTrue(hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .anyMatch(d -> TEST_ID.equals(d.getId())));
        System.out.println("nativeMatch hits = " + hits.getTotalHits());
    }

    private ProblemEsDocument buildDoc(String content) {
        ProblemEsDocument doc = new ProblemEsDocument();
        doc.setId(TEST_ID);
        doc.setContent(content);
        return doc;
    }
}
