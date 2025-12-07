package top.yumbo.ai.rag.spring.boot.strategy.search.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关键词检索策略（Keyword Search Strategy）
 *
 * <p>仅使用 Lucene 关键词检索，适合精确匹配场景</p>
 * <p>Uses only Lucene keyword search, suitable for exact match scenarios</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class KeywordSearchStrategy implements SearchStrategy {

    @Override
    public String getId() {
        return "keyword";
    }

    @Override
    public String getName() {
        return I18N.get("strategy.search.keyword.name");
    }

    @Override
    public String getDescription() {
        return I18N.get("strategy.search.keyword.description");
    }

    @Override
    public int evaluateSuitability(SearchContext context) {
        int score = 50;

        // 如果查询包含特殊符号或引号，可能是精确搜索 (If query contains special symbols or quotes, might be exact search)
        String question = context.getQuestion();
        if (question != null) {
            if (question.contains("\"") || question.contains("'")) {
                score += 30; // 引号表示精确匹配 (Quotes indicate exact match)
            }
            if (question.matches(".*[A-Z]{2,}.*")) {
                score += 10; // 包含缩写词 (Contains abbreviations)
            }
            if (question.length() < 10) {
                score += 15; // 短查询更适合关键词 (Short queries better for keywords)
            }
        }

        // 如果没有向量引擎，关键词是唯一选择 (If no vector engine, keyword is the only option)
        if (context.getEmbeddingEngine() == null || context.getVectorIndexEngine() == null) {
            score += 40;
        }

        return Math.min(score, 100);
    }

    @Override
    public List<Document> search(SearchContext context) {
        SearchContext.SearchParameters params = context.getParameters();

        SearchResult result = context.getRag().search(Query.builder()
            .queryText(context.getKeywords())
            .limit(params.getHybridTopK())
            .build());

        List<Document> docs = result.getDocuments().stream()
            .map(ScoredDocument::getDocument)
            .collect(Collectors.toList());

        // 设置评分 (Set scores)
        for (int i = 0; i < docs.size(); i++) {
            docs.get(i).setScore(1.0 - (i * 1.0 / Math.max(docs.size(), 1)));
        }

        log.debug(I18N.get("log.strategy.keyword.completed", docs.size()));
        return docs;
    }

    @Override
    public int getPriority() {
        return 20; // 中等优先级 (Medium priority)
    }
}

