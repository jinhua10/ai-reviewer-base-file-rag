package top.yumbo.ai.rag.spring.boot.strategy.search.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 向量检索策略（Vector Search Strategy）
 *
 * <p>仅使用向量语义检索，适合语义相似场景</p>
 * <p>Uses only vector semantic search, suitable for semantic similarity scenarios</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class VectorSearchStrategy implements SearchStrategy {

    @Override
    public String getId() {
        return "vector";
    }

    @Override
    public String getName() {
        return I18N.get("strategy.search.vector.name");
    }

    @Override
    public String getDescription() {
        return I18N.get("strategy.search.vector.description");
    }

    @Override
    public int evaluateSuitability(SearchContext context) {
        // 必须有向量引擎 (Must have vector engine)
        if (context.getEmbeddingEngine() == null || context.getVectorIndexEngine() == null) {
            return 0;
        }

        int score = 60;

        String question = context.getQuestion();
        if (question != null) {
            // 长查询更适合向量检索 (Long queries better for vector search)
            if (question.length() > 30) {
                score += 20;
            }
            // 自然语言问句 (Natural language questions)
            if (question.contains("？") || question.contains("?") ||
                question.contains("如何") || question.contains("怎么") ||
                question.contains("什么") || question.contains("为什么")) {
                score += 15;
            }
        }

        return Math.min(score, 100);
    }

    @Override
    public List<Document> search(SearchContext context) {
        if (context.getEmbeddingEngine() == null || context.getVectorIndexEngine() == null) {
            log.warn(I18N.get("log.strategy.vector.no_engine"));
            return Collections.emptyList();
        }

        SearchContext.SearchParameters params = context.getParameters();

        float[] queryVector = context.getEmbeddingEngine().embed(context.getQuestion());
        List<SimpleVectorIndexEngine.VectorSearchResult> vectorResults =
            context.getVectorIndexEngine().search(queryVector, params.getHybridTopK(), params.getSimilarityThreshold());

        List<Document> docs = vectorResults.stream()
            .map(result -> {
                Document doc = context.getRag().getDocument(result.getDocId());
                if (doc != null) {
                    doc.setScore((double) result.getSimilarity());
                }
                return doc;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.debug(I18N.get("log.strategy.vector.completed", docs.size()));
        return docs;
    }

    @Override
    public int getPriority() {
        return 30; // 较低优先级 (Lower priority)
    }
}

