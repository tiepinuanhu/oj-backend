package com.wxc.oj.es;

import com.wxc.oj.model.es.ProblemEsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProblemEsRepository extends ElasticsearchRepository<ProblemEsDocument, Long> {

    List<ProblemEsDocument> findByContentContaining(String content);
}
