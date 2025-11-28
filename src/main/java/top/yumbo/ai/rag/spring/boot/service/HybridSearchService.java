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

            // 显示 Lucene Top-10（带评分）
            if (!luceneResult.getDocuments().isEmpty()) {
                log.info("   Lucene Top-10 文档（按相关性排序）:");
                List<Document> luceneDocs = luceneResult.getDocuments();
                for (int i = 0; i < Math.min(10, luceneDocs.size()); i++) {
                    Document doc = luceneDocs.get(i);
                    // 计算归一化评分（第1名=1.0，逐步降低）
                    double normalizedScore = 1.0 - (i * 1.0 / luceneDocs.size());
                    log.info("      {}. {} - {} 字符 (Lucene 排名分: {:.3f})",
                            i + 1, doc.getTitle(), doc.getContent().length(), normalizedScore);
                }
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

            // 4. 按混合分数排序
            int topK = properties.getVectorSearch().getTopK();
            float minScore = properties.getVectorSearch().getMinScoreThreshold();

            // 先排序，看看未过滤前的 Top 文档
            List<Map.Entry<String, Double>> allSortedScores = hybridScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

            // 显示未过滤前的 Top 5
            if (!allSortedScores.isEmpty()) {
                log.info("📊 混合评分 Top-5 (过滤前，阈值={}):", minScore);
                for (int i = 0; i < Math.min(5, allSortedScores.size()); i++) {
                    var entry = allSortedScores.get(i);
                    Document doc = rag.getDocument(entry.getKey());
                    if (doc != null) {
                        String status = entry.getValue() >= minScore ? "✅" : "❌";
                        log.info("      {} {}. {} (评分: {:.3f})",
                            status, i + 1, doc.getTitle(), entry.getValue());
                    }
                }
            }

            // 过滤低分文档
            List<Map.Entry<String, Double>> sortedScores = allSortedScores.stream()
                .filter(entry -> entry.getValue() >= minScore)
                .limit(topK)
                .toList();

            if (sortedScores.size() < hybridScores.size()) {
                log.warn("⚠️ 过滤了 {} 个低分文档（评分 < {}），保留 {} 个文档",
                        hybridScores.size() - sortedScores.size(), minScore, sortedScores.size());
            }

            log.info("🎲 混合评分 Top-{} (Lucene权重:0.3 + 向量权重:0.7):", sortedScores.size());
            int displayCount = 0;
            for (int i = 0; i < Math.min(sortedScores.size(), 20); i++) {
                var entry = sortedScores.get(i);
                Document doc = rag.getDocument(entry.getKey());
                if (doc != null) {
                    // 计算详细评分信息
                    int luceneRank = -1;
                    for (int j = 0; j < luceneDocs.size(); j++) {
                        if (luceneDocs.get(j).getId().equals(entry.getKey())) {
                            luceneRank = j + 1;
                            break;
                        }
                    }

                    double vectorScore = 0.0;
                    for (SimpleVectorIndexEngine.VectorSearchResult result : vectorResults) {
                        if (result.getDocId().equals(entry.getKey())) {
                            vectorScore = result.getSimilarity();
                            break;
                        }
                    }

                    log.info("   {}. {} (混合分: {} = Lucene排名#{} + 向量:{})",
                        i + 1, doc.getTitle(), String.format("%.3f", entry.getValue()),
                        luceneRank > 0 ? luceneRank : "N/A", String.format("%.3f", vectorScore));
                    displayCount++;
                } else {
                    log.warn("   ⚠️ {}. 文档ID={} 无法获取文档对象 (评分: {})",
                        i + 1, entry.getKey(), String.format("%.3f", entry.getValue()));
                }
            }

            if (displayCount == 0 && !sortedScores.isEmpty()) {
                log.error("❌ 严重问题：有 {} 个评分文档，但都无法获取文档对象！", sortedScores.size());
                log.error("   文档ID列表: {}", sortedScores.stream()
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", ")));
            }

            // 5. 从 RAG 获取完整文档
            List<Document> finalDocs = new ArrayList<>();
            int nullCount = 0;
            for (var entry : sortedScores) {
                Document doc = rag.getDocument(entry.getKey());
                if (doc != null) {
                    finalDocs.add(doc);
                } else {
                    nullCount++;
                    if (nullCount <= 3) { // 只输出前3个null的详细信息
                        log.warn("⚠️ 无法获取文档: ID={}, 评分={}", entry.getKey(), String.format("%.3f", entry.getValue()));
                    }
                }
            }

            if (nullCount > 0) {
                log.warn("⚠️ 总计 {} 个文档无法获取（共 {} 个评分文档）", nullCount, sortedScores.size());
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

