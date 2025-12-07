package top.yumbo.ai.rag.spring.boot.strategy.search.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchStrategy;
import top.yumbo.ai.rag.feedback.DocumentWeightService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索策略（Hybrid Search Strategy）
 *
 * <p>结合 Lucene 关键词检索和向量语义检索</p>
 * <p>Combines Lucene keyword search and vector semantic search</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class HybridSearchStrategy implements SearchStrategy {

    private final DocumentWeightService documentWeightService;

    @Autowired
    public HybridSearchStrategy(@Autowired(required = false) DocumentWeightService documentWeightService) {
        this.documentWeightService = documentWeightService;
    }

    @Override
    public String getId() {
        return "hybrid";
    }

    @Override
    public String getName() {
        return I18N.get("strategy.search.hybrid.name");
    }

    @Override
    public String getDescription() {
        return I18N.get("strategy.search.hybrid.description");
    }

    @Override
    public int evaluateSuitability(SearchContext context) {
        // 混合检索适用于大多数场景 (Hybrid search is suitable for most scenarios)
        int score = 80;

        // 如果有向量引擎，提高适用性 (If vector engine available, increase suitability)
        if (context.getEmbeddingEngine() != null && context.getVectorIndexEngine() != null) {
            score += 10;
        }

        // 如果查询较长，向量检索更有效 (If query is long, vector search is more effective)
        if (context.getQuestion() != null && context.getQuestion().length() > 20) {
            score += 5;
        }

        return Math.min(score, 100);
    }

    @Override
    public List<Document> search(SearchContext context) {
        Map<String, Double> hybridScores = new HashMap<>();
        SearchContext.SearchParameters params = context.getParameters();

        // 1. Lucene 关键词检索 (Lucene keyword search)
        SearchResult luceneResult = context.getRag().search(Query.builder()
            .queryText(context.getKeywords())
            .limit(params.getLuceneTopK())
            .build());

        List<Document> luceneDocs = luceneResult.getDocuments().stream()
            .map(ScoredDocument::getDocument)
            .toList();

        // Lucene 归一化评分 (Lucene normalized scoring)
        for (int i = 0; i < luceneDocs.size(); i++) {
            String docId = luceneDocs.get(i).getId();
            double normalizedScore = 1.0 - (i * 1.0 / Math.max(luceneDocs.size(), 1));
            hybridScores.put(docId, params.getLuceneWeight() * normalizedScore);
        }

        // 2. 向量检索 (Vector search)
        if (context.getEmbeddingEngine() != null && context.getVectorIndexEngine() != null) {
            float[] queryVector = context.getEmbeddingEngine().embed(context.getQuestion());
            List<SimpleVectorIndexEngine.VectorSearchResult> vectorResults =
                context.getVectorIndexEngine().search(queryVector, params.getVectorTopK(), params.getSimilarityThreshold());

            for (SimpleVectorIndexEngine.VectorSearchResult result : vectorResults) {
                String docId = result.getDocId();
                double currentScore = hybridScores.getOrDefault(docId, 0.0);
                hybridScores.put(docId, currentScore + params.getVectorWeight() * result.getSimilarity());
            }
        }

        // 3. 应用反馈权重 (Apply feedback weights)
        if (documentWeightService != null) {
            for (Map.Entry<String, Double> entry : hybridScores.entrySet()) {
                Document doc = context.getRag().getDocument(entry.getKey());
                if (doc != null) {
                    double feedbackWeight = documentWeightService.getDocumentWeight(doc.getTitle());
                    if (feedbackWeight != 1.0) {
                        hybridScores.put(entry.getKey(), entry.getValue() * feedbackWeight);
                    }
                }
            }
        }

        // 4. 排序并过滤 (Sort and filter)
        List<Document> results = hybridScores.entrySet().stream()
            .filter(e -> e.getValue() >= params.getMinScoreThreshold())
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(params.getHybridTopK())
            .map(e -> {
                Document doc = context.getRag().getDocument(e.getKey());
                if (doc != null) {
                    doc.setScore(e.getValue());
                }
                return doc;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.debug(I18N.get("log.strategy.hybrid.completed", results.size()));
        return results;
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级 (High priority)
    }
}

