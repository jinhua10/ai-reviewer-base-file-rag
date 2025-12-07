package top.yumbo.ai.rag.spring.boot.strategy.search;

import lombok.Builder;
import lombok.Data;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * 检索上下文（Search Context）
 *
 * <p>封装检索所需的所有上下文信息</p>
 * <p>Encapsulates all context information needed for search</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Data
@Builder
public class SearchContext {

    /**
     * 原始查询问题（Original query question）
     */
    private String question;

    /**
     * 扩展后的查询（Expanded query）
     */
    private String expandedQuestion;

    /**
     * 提取的关键词（Extracted keywords）
     */
    private String keywords;

    /**
     * RAG 实例（RAG instance）
     */
    private LocalFileRAG rag;

    /**
     * 嵌入引擎（Embedding engine）
     */
    private LocalEmbeddingEngine embeddingEngine;

    /**
     * 向量索引引擎（Vector index engine）
     */
    private SimpleVectorIndexEngine vectorIndexEngine;

    /**
     * 检索参数（Search parameters）
     */
    @Builder.Default
    private SearchParameters parameters = new SearchParameters();

    /**
     * 额外属性（Extra attributes）
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 获取额外属性（Get extra attribute）
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 设置额外属性（Set extra attribute）
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 检索参数（Search Parameters）
     */
    @Data
    public static class SearchParameters {
        /**
         * Lucene 检索数量（Lucene search limit）
         */
        private int luceneTopK = 100;

        /**
         * 向量检索数量（Vector search limit）
         */
        private int vectorTopK = 50;

        /**
         * 混合检索最终数量（Hybrid search final limit）
         */
        private int hybridTopK = 30;

        /**
         * 最小分数阈值（Minimum score threshold）
         */
        private float minScoreThreshold = 0.06f;

        /**
         * 相似度阈值（Similarity threshold）
         */
        private float similarityThreshold = 0.5f;

        /**
         * Lucene 权重（Lucene weight）
         */
        private double luceneWeight = 0.3;

        /**
         * 向量权重（Vector weight）
         */
        private double vectorWeight = 0.7;
    }
}

