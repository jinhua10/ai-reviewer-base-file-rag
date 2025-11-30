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
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务（Hybrid search service）
 * 结合 Lucene 关键词检索和向量语义检索（Combines Lucene keyword search and vector semantic search）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class HybridSearchService {

    private final KnowledgeQAProperties properties;
    private final SearchConfigService configService;

    public HybridSearchService(KnowledgeQAProperties properties, SearchConfigService configService) {
        this.properties = properties;
        this.configService = configService;
    }

    /**
     * 混合检索：结合 Lucene 关键词检索和向量语义检索（Hybrid search: combines Lucene keyword search and vector semantic search）
     *
     * @param question 查询问题（Query question）
     * @param rag RAG 实例（RAG instance）
     * @param embeddingEngine 嵌入引擎（Embedding engine）
     * @param vectorIndexEngine 向量索引引擎（Vector index engine）
     * @return 检索到的文档列表（Retrieved document list）
     */
    public List<Document> hybridSearch(String question, LocalFileRAG rag,
                                      LocalEmbeddingEngine embeddingEngine,
                                      SimpleVectorIndexEngine vectorIndexEngine) {
        try {
            long startTime = System.currentTimeMillis();

            // 1. Lucene 关键词检索（快速粗筛）
            String keywords = extractKeywords(question);
            log.info(LogMessageProvider.getMessage("log.hybrid.extract_keywords", keywords));

            int luceneLimit = configService.getLuceneTopK();
            SearchResult luceneResult = rag.search(Query.builder()
                .queryText(keywords)
                .limit(luceneLimit)
                .build());

            log.info(LogMessageProvider.getMessage("log.hybrid.lucene_found", luceneResult.getDocuments().size(), luceneResult.getTotalHits(), luceneLimit));

            // 显示 Lucene Top-10（带评分）
            if (!luceneResult.getDocuments().isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.hybrid.lucene_top_header"));
                List<Document> luceneDocs = luceneResult.getDocuments().stream()
                    .map(ScoredDocument::getDocument)
                    .toList();
                for (int i = 0; i < Math.min(10, luceneDocs.size()); i++) {
                    Document doc = luceneDocs.get(i);
                    double normalizedScore = 1.0 - (i * 1.0 / luceneDocs.size());
                    log.info(LogMessageProvider.getMessage("log.hybrid.lucene_top_item", i + 1, doc.getTitle(), doc.getContent().length(), normalizedScore));
                }
            }

            // 2. 向量检索（语义精排）
            float[] queryVector = embeddingEngine.embed(question);
            float threshold = properties.getVectorSearch().getSimilarityThreshold();
            int vectorLimit = configService.getVectorTopK();

            List<SimpleVectorIndexEngine.VectorSearchResult> vectorResults =
                vectorIndexEngine.search(queryVector, vectorLimit, threshold);

            log.info(LogMessageProvider.getMessage("log.hybrid.vector_found", vectorResults.size(), vectorLimit));

            if (!vectorResults.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.hybrid.vector_top_header"));
                vectorResults.stream().limit(10).forEach(result -> {
                    Document doc = rag.getDocument(result.getDocId());
                    if (doc != null) {
                        log.info(LogMessageProvider.getMessage("log.hybrid.vector_top_item", doc.getTitle(), result.getSimilarity()));
                    }
                });
            }

            // 3. 混合评分：融合两种检索结果
            Map<String, Double> hybridScores = new HashMap<>();

            // Lucene 结果（权重 0.3）
            List<Document> luceneDocs = luceneResult.getDocuments().stream()
                .map(ScoredDocument::getDocument)
                .toList();
            for (int i = 0; i < luceneDocs.size(); i++) {
                String docId = luceneDocs.get(i).getId();
                double normalizedScore = 1.0 - (i * 1.0 / luceneDocs.size());
                hybridScores.put(docId, 0.3 * normalizedScore);
            }

            // 向量结果（权重 0.7）
            for (SimpleVectorIndexEngine.VectorSearchResult result : vectorResults) {
                String docId = result.getDocId();
                double currentScore = hybridScores.getOrDefault(docId, 0.0);
                hybridScores.put(docId, currentScore + 0.7 * result.getSimilarity());
            }

            // 4. 按混合分数排序并去重
            int topK = configService.getHybridTopK();
            float minScore = configService.getMinScoreThreshold();

            List<Map.Entry<String, Double>> allSortedScores = hybridScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

            if (!allSortedScores.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.hybrid.top5_header", minScore, topK));
                for (int i = 0; i < Math.min(5, allSortedScores.size()); i++) {
                    var entry = allSortedScores.get(i);
                    Document doc = rag.getDocument(entry.getKey());
                    if (doc != null) {
                        String status = entry.getValue() >= minScore ? "✅" : "❌";
                        log.info(LogMessageProvider.getMessage("log.hybrid.top5_item", status, i + 1, doc.getTitle(), entry.getValue()));
                    }
                }
            }

            List<Map.Entry<String, Double>> sortedScores = allSortedScores.stream()
                .filter(entry -> entry.getValue() >= minScore)
                .limit(topK)
                .toList();

            if (sortedScores.size() < hybridScores.size()) {
                log.warn(LogMessageProvider.getMessage("log.hybrid.filtered", hybridScores.size() - sortedScores.size(), minScore, sortedScores.size()));
            }

            log.info(LogMessageProvider.getMessage("log.hybrid.topk_header", sortedScores.size()));
            int displayCount = 0;
            for (int i = 0; i < Math.min(sortedScores.size(), 20); i++) {
                var entry = sortedScores.get(i);
                Document doc = rag.getDocument(entry.getKey());
                if (doc != null) {
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

                    log.info(LogMessageProvider.getMessage("log.hybrid.detail_item", i + 1, doc.getTitle(), String.format("%.3f", entry.getValue()), luceneRank > 0 ? luceneRank : "N/A", String.format("%.3f", vectorScore)));
                    displayCount++;
                } else {
                    log.warn(LogMessageProvider.getMessage("log.hybrid.could_not_get_doc", i + 1, entry.getKey(), String.format("%.3f", entry.getValue())));
                }
            }

            if (displayCount == 0 && !sortedScores.isEmpty()) {
                log.error(LogMessageProvider.getMessage("log.hybrid.severe_no_docs", sortedScores.size()));
                log.error(LogMessageProvider.getMessage("log.hybrid.doc_id_list", sortedScores.stream().limit(5).map(Map.Entry::getKey).collect(Collectors.joining(", "))));
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
                        log.warn(LogMessageProvider.getMessage("log.hybrid.cannot_get_doc", entry.getKey(), String.format("%.3f", entry.getValue())));
                    }
                }
            }

            if (nullCount > 0) {
                log.warn(LogMessageProvider.getMessage("log.hybrid.total_nulls", nullCount, sortedScores.size()));
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info(LogMessageProvider.getMessage("log.hybrid.completed", finalDocs.size(), elapsed));

            return finalDocs;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.hybrid.failed"), e);
            return fallbackToKeywordSearch(question, rag);
        }
    }

    /**
     * 纯关键词检索（回退模式）（Pure keyword search (fallback mode)）
     */
    public List<Document> keywordSearch(String question, LocalFileRAG rag) {
        String keywords = extractKeywords(question);
        log.info(LogMessageProvider.getMessage("log.hybrid.keyword_search", keywords));

        SearchResult result = rag.search(Query.builder()
            .queryText(keywords)
            .limit(configService.getHybridTopK())
            .build());

        log.info(LogMessageProvider.getMessage("log.hybrid.found_docs", result.getDocuments().size()));
        return result.getDocuments().stream()
            .map(ScoredDocument::getDocument)
            .collect(Collectors.toList());
    }

    /**
     * 回退到关键词检索（Fallback to keyword search）
     */
    private List<Document> fallbackToKeywordSearch(String question, LocalFileRAG rag) {
        String keywords = extractKeywords(question);
        SearchResult fallbackResult = rag.search(Query.builder()
            .queryText(keywords)
            .limit(configService.getHybridTopK())
            .build());
        return fallbackResult.getDocuments().stream()
            .map(ScoredDocument::getDocument)
            .collect(Collectors.toList());
    }

    /**
     * 提取关键词（Extract keywords）
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
