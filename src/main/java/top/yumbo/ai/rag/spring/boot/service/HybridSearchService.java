package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * 结合 Lucene 关键词检索和向量语义检索
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class HybridSearchService {

    private final KnowledgeQAProperties properties;

    public HybridSearchService(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * 混合检索：结合 Lucene 关键词检索和向量语义检索
     *
     * @param question 查询问题
     * @param rag RAG 实例
     * @param embeddingEngine 嵌入引擎
     * @param vectorIndexEngine 向量索引引擎
     * @return 检索到的文档列表
     */
    public List<Document> hybridSearch(String question, LocalFileRAG rag,
                                      LocalEmbeddingEngine embeddingEngine,
                                      SimpleVectorIndexEngine vectorIndexEngine) {
        try {
            long startTime = System.currentTimeMillis();

            // 1. Lucene 关键词检索（快速粗筛）
            String keywords = extractKeywords(question);
            log.info("🔍 提取关键词: {}", keywords);

            int luceneLimit = properties.getVectorSearch().getTopK() * 2; // Lucene 返回更多候选
            SearchResult luceneResult = rag.search(Query.builder()
                .queryText(keywords)
                .limit(luceneLimit)
                .build());

            log.info("📚 Lucene检索找到 {} 个文档 (总命中: {})",
                luceneResult.getDocuments().size(),
                luceneResult.getTotalHits());

            // 显示 Lucene Top-10
            if (!luceneResult.getDocuments().isEmpty()) {
                log.info("   Lucene Top-10 文档:");
                luceneResult.getDocuments().stream().limit(10).forEach(doc ->
                    log.info("      - {} ({} 字符)", doc.getTitle(), doc.getContent().length())
                );
            }

            // 2. 向量检索（语义精排）
            float[] queryVector = embeddingEngine.embed(question);
            float threshold = properties.getVectorSearch().getSimilarityThreshold();

            List<SimpleVectorIndexEngine.VectorSearchResult> vectorResults =
                vectorIndexEngine.search(queryVector, luceneLimit, threshold);

            log.info("🎯 向量检索找到 {} 个文档", vectorResults.size());

            // 显示向量 Top-10
            if (!vectorResults.isEmpty()) {
                log.info("   向量 Top-10 文档:");
                vectorResults.stream().limit(10).forEach(result -> {
                    Document doc = rag.getDocument(result.getDocId());
                    if (doc != null) {
                        log.info("      - {} (相似度: {:.3f})",
                            doc.getTitle(), result.getSimilarity());
                    }
                });
            }

            // 3. 混合评分：融合两种检索结果
            Map<String, Double> hybridScores = new HashMap<>();

            // Lucene 结果（权重 0.3）
            List<Document> luceneDocs = luceneResult.getDocuments();
            for (int i = 0; i < luceneDocs.size(); i++) {
                String docId = luceneDocs.get(i).getId();
                // 归一化排名分数（第1名=1.0，逐步降低）
                double normalizedScore = 1.0 - (i * 1.0 / luceneDocs.size());
                hybridScores.put(docId, 0.3 * normalizedScore);
            }

            // 向量结果（权重 0.7）
            for (SimpleVectorIndexEngine.VectorSearchResult result : vectorResults) {
                String docId = result.getDocId();
                double currentScore = hybridScores.getOrDefault(docId, 0.0);
                hybridScores.put(docId, currentScore + 0.7 * result.getSimilarity());
            }

            // 4. 按混合分数排序，并过滤低分文档
            int topK = properties.getVectorSearch().getTopK();
            float minScore = properties.getVectorSearch().getMinScoreThreshold();

            List<Map.Entry<String, Double>> sortedScores = hybridScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= minScore) // 过滤低分文档
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .toList();

            if (sortedScores.size() < hybridScores.size()) {
                log.info("⚠️ 过滤了 {} 个低分文档（评分 < {}）",
                        hybridScores.size() - sortedScores.size(), minScore);
            }

            log.info("🎲 混合评分 Top-{}:", Math.min(topK, sortedScores.size()));
            for (int i = 0; i < Math.min(sortedScores.size(), 10); i++) {
                var entry = sortedScores.get(i);
                Document doc = rag.getDocument(entry.getKey());
                if (doc != null) {
                    log.info("   {}. {} (混合分数: {:.3f})",
                        i + 1, doc.getTitle(), entry.getValue());
                }
            }

            // 5. 从 RAG 获取完整文档
            List<Document> finalDocs = new ArrayList<>();
            for (var entry : sortedScores) {
                Document doc = rag.getDocument(entry.getKey());
                if (doc != null) {
                    finalDocs.add(doc);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 混合检索完成: 返回 {} 个文档，耗时 {}ms", finalDocs.size(), elapsed);

            return finalDocs;

        } catch (Exception e) {
            log.error("混合检索失败，回退到纯关键词检索", e);
            return fallbackToKeywordSearch(question, rag);
        }
    }

    /**
     * 纯关键词检索（回退模式）
     */
    public List<Document> keywordSearch(String question, LocalFileRAG rag) {
        String keywords = extractKeywords(question);
        log.info("🔍 关键词检索: {}", keywords);

        SearchResult result = rag.search(Query.builder()
            .queryText(keywords)
            .limit(properties.getVectorSearch().getTopK())
            .build());

        log.info("📚 找到 {} 个文档", result.getDocuments().size());
        return result.getDocuments();
    }

    /**
     * 回退到关键词检索
     */
    private List<Document> fallbackToKeywordSearch(String question, LocalFileRAG rag) {
        String keywords = extractKeywords(question);
        SearchResult fallbackResult = rag.search(Query.builder()
            .queryText(keywords)
            .limit(properties.getVectorSearch().getTopK())
            .build());
        return fallbackResult.getDocuments();
    }

    /**
     * 提取关键词
     */
    private String extractKeywords(String question) {
        // 简单的停用词列表
        List<String> stopWords = Arrays.asList(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她",
            "什么", "怎么", "如何", "为什么", "吗", "呢", "啊", "那些"
        );

        return Arrays.stream(question.split("\\s+"))
            .filter(word -> !stopWords.contains(word) && word.length() > 1)
            .collect(Collectors.joining(" "));
    }
}

