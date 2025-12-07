package top.yumbo.ai.rag.spring.boot.strategy.search.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.spring.boot.strategy.search.ScoreContributor;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lucene 评分贡献者（Lucene Score Contributor）
 *
 * <p>基于 Lucene 关键词匹配提供评分</p>
 * <p>Provides scores based on Lucene keyword matching</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class LuceneScoreContributor implements ScoreContributor {

    private double weight = 0.3;

    @Override
    public String getName() {
        return "lucene";
    }

    @Override
    public String getDescription() {
        return I18N.get("score_contributor.lucene.description");
    }

    @Override
    public Map<String, Double> contribute(SearchContext context) {
        Map<String, Double> scores = new HashMap<>();

        if (context.getRag() == null || context.getKeywords() == null) {
            return scores;
        }

        SearchResult result = context.getRag().search(Query.builder()
            .queryText(context.getKeywords())
            .limit(context.getParameters().getLuceneTopK())
            .build());

        List<ScoredDocument> docs = result.getDocuments();
        for (int i = 0; i < docs.size(); i++) {
            String docId = docs.get(i).getDocument().getId();
            // 基于排名的归一化评分 (Rank-based normalized scoring)
            double normalizedScore = 1.0 - (i * 1.0 / Math.max(docs.size(), 1));
            scores.put(docId, normalizedScore);
        }

        log.debug(I18N.get("log.score_contributor.lucene.done", scores.size()));
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
        return 10;
    }
}

