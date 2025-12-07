package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.feedback.DocumentWeightService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务（Hybrid search service）
 * 结合 Lucene 关键词检索和向量语义检索（Combines Lucene keyword search and vector semantic search）
 *
 * 📈 优化（2025-12-05）：集成查询扩展服务，提升召回率
 * 📈 优化（2025-12-07）：集成文档权重服务，反馈影响检索排序
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class HybridSearchService {

    private final KnowledgeQAProperties properties;
    private final SearchConfigService configService;
    private final QueryExpansionService queryExpansionService;
    private final DocumentWeightService documentWeightService;

    @Autowired
    public HybridSearchService(KnowledgeQAProperties properties,
                               SearchConfigService configService,
                               @Autowired(required = false) QueryExpansionService queryExpansionService,
                               @Autowired(required = false) DocumentWeightService documentWeightService) {
        this.properties = properties;
        this.configService = configService;
        this.queryExpansionService = queryExpansionService;
        this.documentWeightService = documentWeightService;
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

            // 0. 查询扩展（优化：提升召回率）
            String expandedQuestion = expandQueryIfEnabled(question);

            // 1. Lucene 关键词检索（快速粗筛）
            String keywords = extractKeywords(expandedQuestion);
            log.info(I18N.get("log.hybrid.extract_keywords", keywords));

            int luceneLimit = configService.getLuceneTopK();
            SearchResult luceneResult = rag.search(Query.builder()
                .queryText(keywords)
                .limit(luceneLimit)
                .build());

            log.info(I18N.get("log.hybrid.lucene_found", luceneResult.getDocuments().size(), luceneResult.getTotalHits(), luceneLimit));

            // 显示 Lucene Top-10（带评分）
            if (!luceneResult.getDocuments().isEmpty()) {
                log.info(I18N.get("log.hybrid.lucene_top_header"));
                List<Document> luceneDocs = luceneResult.getDocuments().stream()
                    .map(ScoredDocument::getDocument)
                    .toList();
                // 从配置获取日志显示数量 (Get log display limit from config)
                int logLimit = properties.getVectorSearch().getLogDisplayLimit();
                for (int i = 0; i < Math.min(logLimit, luceneDocs.size()); i++) {
                    Document doc = luceneDocs.get(i);
                    double normalizedScore = 1.0 - (i * 1.0 / luceneDocs.size());
                    log.info(I18N.get("log.hybrid.lucene_top_item", i + 1, doc.getTitle(), doc.getContent().length(), normalizedScore));
                }
            }

            // 2. 向量检索（语义精排）(Step 2: Vector search for semantic refinement)
            float[] queryVector = embeddingEngine.embed(question);
            float threshold = properties.getVectorSearch().getSimilarityThreshold();
            int vectorLimit = configService.getVectorTopK();

            List<SimpleVectorIndexEngine.VectorSearchResult> vectorResults =
                vectorIndexEngine.search(queryVector, vectorLimit, threshold);

            log.info(I18N.get("log.hybrid.vector_found", vectorResults.size(), vectorLimit));

            if (!vectorResults.isEmpty()) {
                log.info(I18N.get("log.hybrid.vector_top_header"));
                int logLimit = properties.getVectorSearch().getLogDisplayLimit();
                vectorResults.stream().limit(logLimit).forEach(result -> {
                    Document doc = rag.getDocument(result.getDocId());
                    if (doc != null) {
                        log.info(I18N.get("log.hybrid.vector_top_item", doc.getTitle(), result.getSimilarity()));
                    }
                });
            }

            // 3. 混合评分：融合两种检索结果
            Map<String, Double> hybridScores = new HashMap<>();

            // 从配置获取权重（Lucene and vector weights from configuration）
            double luceneWeight = properties.getVectorSearch().getLuceneWeight();
            double vectorWeight = properties.getVectorSearch().getVectorWeight();

            // Lucene 结果（使用配置的权重）
            List<Document> luceneDocs = luceneResult.getDocuments().stream()
                .map(ScoredDocument::getDocument)
                .toList();
            for (int i = 0; i < luceneDocs.size(); i++) {
                String docId = luceneDocs.get(i).getId();
                double normalizedScore = 1.0 - (i * 1.0 / luceneDocs.size());
                hybridScores.put(docId, luceneWeight * normalizedScore);
            }

            // 向量结果（使用配置的权重）
            for (SimpleVectorIndexEngine.VectorSearchResult result : vectorResults) {
                String docId = result.getDocId();
                double currentScore = hybridScores.getOrDefault(docId, 0.0);
                hybridScores.put(docId, currentScore + vectorWeight * result.getSimilarity());
            }

            // 3.5 应用文档反馈权重（如果启用）
            // (Apply document feedback weights if enabled)
            if (documentWeightService != null) {
                int adjustedCount = 0;
                for (Map.Entry<String, Double> entry : hybridScores.entrySet()) {
                    Document doc = rag.getDocument(entry.getKey());
                    if (doc != null) {
                        double feedbackWeight = documentWeightService.getDocumentWeight(doc.getTitle());
                        if (feedbackWeight != 1.0) {
                            double originalScore = entry.getValue();
                            double adjustedScore = originalScore * feedbackWeight;
                            hybridScores.put(entry.getKey(), adjustedScore);
                            adjustedCount++;
                            log.debug(I18N.get("log.hybrid.feedback_weight_detail",
                                doc.getTitle(),
                                String.format("%.3f", originalScore),
                                String.format("%.2f", feedbackWeight),
                                String.format("%.3f", adjustedScore)));
                        }
                    }
                }
                if (adjustedCount > 0) {
                    log.info(I18N.get("log.hybrid.feedback_weight_applied", adjustedCount));
                }
            }

            // 4. 按混合分数排序并去重 (Sort by hybrid score and deduplicate)
            int topK = configService.getHybridTopK();
            float minScore = configService.getMinScoreThreshold();

            List<Map.Entry<String, Double>> allSortedScores = hybridScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

            if (!allSortedScores.isEmpty()) {
                log.info(I18N.get("log.hybrid.top5_header", minScore, topK));
                for (int i = 0; i < Math.min(5, allSortedScores.size()); i++) {
                    var entry = allSortedScores.get(i);
                    Document doc = rag.getDocument(entry.getKey());
                    if (doc != null) {
                        String status = entry.getValue() >= minScore ? "✅" : "❌";
                        log.info(I18N.get("log.hybrid.top5_item", status, i + 1, doc.getTitle(), entry.getValue()));
                    }
                }
            }

            List<Map.Entry<String, Double>> sortedScores = allSortedScores.stream()
                .filter(entry -> entry.getValue() >= minScore)
                .limit(topK)
                .toList();

            if (sortedScores.size() < hybridScores.size()) {
                log.warn(I18N.get("log.hybrid.filtered", hybridScores.size() - sortedScores.size(), minScore, sortedScores.size()));
            }

            log.info(I18N.get("log.hybrid.topk_header", sortedScores.size()));
            int displayCount = 0;
            int logLimit = properties.getVectorSearch().getLogDisplayLimit();
            for (int i = 0; i < Math.min(sortedScores.size(), logLimit * 2); i++) {
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

                    log.info(I18N.get("log.hybrid.detail_item", i + 1, doc.getTitle(), String.format("%.3f", entry.getValue()), luceneRank > 0 ? luceneRank : "N/A", String.format("%.3f", vectorScore)));
                    displayCount++;
                } else {
                    log.warn(I18N.get("log.hybrid.could_not_get_doc", i + 1, entry.getKey(), String.format("%.3f", entry.getValue())));
                }
            }

            if (displayCount == 0 && !sortedScores.isEmpty()) {
                log.error(I18N.get("log.hybrid.severe_no_docs", sortedScores.size()));
                log.error(I18N.get("log.hybrid.doc_id_list", sortedScores.stream().limit(5).map(Map.Entry::getKey).collect(Collectors.joining(", "))));
            }

            // 5. 从 RAG 获取完整文档，并保存检索分数
            // (Get full documents from RAG and save retrieval scores)
            List<Document> finalDocs = new ArrayList<>();
            int nullCount = 0;
            for (var entry : sortedScores) {
                Document doc = rag.getDocument(entry.getKey());
                if (doc != null) {
                    // 保存检索分数到文档，供后续 PPL Rerank 使用
                    // (Save retrieval score to document for PPL Rerank)
                    doc.setScore(entry.getValue());
                    finalDocs.add(doc);
                } else {
                    nullCount++;
                    if (nullCount <= 3) { // 只输出前3个null的详细信息 (Only output first 3 nulls)
                        log.warn(I18N.get("log.hybrid.cannot_get_doc", entry.getKey(), String.format("%.3f", entry.getValue())));
                    }
                }
            }

            if (nullCount > 0) {
                log.warn(I18N.get("log.hybrid.total_nulls", nullCount, sortedScores.size()));
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info(I18N.get("log.hybrid.completed", finalDocs.size(), elapsed));

            return finalDocs;

        } catch (Exception e) {
            log.error(I18N.get("log.hybrid.failed"), e);
            return fallbackToKeywordSearch(question, rag);
        }
    }

    /**
     * 纯关键词检索（回退模式）（Pure keyword search (fallback mode)）
     */
    public List<Document> keywordSearch(String question, LocalFileRAG rag) {
        String keywords = extractKeywords(question);
        log.info(I18N.get("log.hybrid.keyword_search", keywords));

        SearchResult result = rag.search(Query.builder()
            .queryText(keywords)
            .limit(configService.getHybridTopK())
            .build());

        log.info(I18N.get("log.hybrid.found_docs", result.getDocuments().size()));
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
     *
     * 支持中英文停用词过滤，通过 yml 配置可自定义停用词列表
     */
    private String extractKeywords(String question) {
        // 获取配置的停用词
        KnowledgeQAProperties.SearchConfig searchConfig = properties.getSearch();

        // 如果禁用停用词过滤，直接返回原文
        if (!searchConfig.isEnableStopWordsFilter()) {
            return question;
        }

        // 合并中英文停用词
        Set<String> stopWords = new HashSet<>();
        if (searchConfig.getChineseStopWords() != null) {
            stopWords.addAll(searchConfig.getChineseStopWords());
        }
        if (searchConfig.getEnglishStopWords() != null) {
            // 英文停用词转小写
            searchConfig.getEnglishStopWords().forEach(w -> stopWords.add(w.toLowerCase()));
        }

        int minLength = searchConfig.getMinKeywordLength();

        return Arrays.stream(question.split("\\s+"))
            .filter(word -> {
                String lowerWord = word.toLowerCase();
                // 过滤停用词和过短的词
                return !stopWords.contains(word)
                    && !stopWords.contains(lowerWord)
                    && word.length() >= minLength;
            })
            .collect(Collectors.joining(" "));
    }

    /**
     * 查询扩展（如果启用）
     *
     * 📈 优化（2025-12-05）：通过同义词扩展提升召回率
     */
    private String expandQueryIfEnabled(String question) {
        if (queryExpansionService == null) {
            return question;
        }

        try {
            // 使用简单扩展（不调用 LLM，避免延迟）
            String expanded = queryExpansionService.simpleExpand(question);
            if (!expanded.equals(question)) {
                log.debug("🔍 查询扩展: {} -> {}", question, expanded);
            }
            return expanded;
        } catch (Exception e) {
            log.warn("⚠️ 查询扩展失败，使用原始查询: {}", e.getMessage());
            return question;
        }
    }
}
