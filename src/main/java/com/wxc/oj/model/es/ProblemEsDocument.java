package com.wxc.oj.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 题目 Elasticsearch 文档（用于检索）
 * content 为 title + 题面正文合并后的全文
 */
@Data
@Document(indexName = "problem")
public class ProblemEsDocument implements Serializable {

    @Id
    private Long id;

    /**
     * 全文检索字段。
     * analyzer：索引时分词，ik_max_word 细粒度切分，提升召回。
     * searchAnalyzer：查询时分词，ik_smart 粗粒度切分，提升精准度。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    private static final long serialVersionUID = 1L;
}
