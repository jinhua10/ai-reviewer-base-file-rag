package top.yumbo.ai.rag.spring.boot.strategy.search.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.spring.boot.strategy.search.ScoreContributor;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量评分贡献者（Vector Score Contributor）
 *
 * <p>基于向量相似度提供评分</p>
 * <p>Provides scores based on vector similarity</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class VectorScoreContributor implements ScoreContributor {

    private double weight = 0.7;

    @Override
    public String getName() {
        return "vector";
    }

    @Override
    public String getDescription() {
        return I18N.get("score_contributor.vector.description");
    }

    @Override
    public Map<String, Double> contribute(SearchContext context) {
        Map<String, Double> scores = new HashMap<>();

        if (context.getEmbeddingEngine() == null || context.getVectorIndexEngine() == null) {
            log.debug(I18N.get("log.score_contributor.vector.no_engine"));
            return scores;
        }

        float[] queryVector = context.getEmbeddingEngine().embed(context.getQuestion());
        List<SimpleVectorIndexEngine.VectorSearchResult> results =
            context.getVectorIndexEngine().search(
                queryVector,
                context.getParameters().getVectorTopK(),
                context.getParameters().getSimilarityThreshold()
            );

        for (SimpleVectorIndexEngine.VectorSearchResult result : results) {
            scores.put(result.getDocId(), (double) result.getSimilarity());
        }

        log.debug(I18N.get("log.score_contributor.vector.done", scores.size()));
        return scores;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public int getPriority() {
        return 20;
    }
}

