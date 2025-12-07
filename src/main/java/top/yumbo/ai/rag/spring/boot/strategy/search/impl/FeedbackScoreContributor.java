package top.yumbo.ai.rag.spring.boot.strategy.search.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.feedback.DocumentWeightService;
import top.yumbo.ai.rag.spring.boot.strategy.search.ScoreContributor;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 反馈评分贡献者（Feedback Score Contributor）
 *
 * <p>基于用户反馈权重提供评分调整</p>
 * <p>Provides score adjustments based on user feedback weights</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class FeedbackScoreContributor implements ScoreContributor {

    private final DocumentWeightService documentWeightService;
    private double weight = 0.2;

    @Autowired
    public FeedbackScoreContributor(@Autowired(required = false) DocumentWeightService documentWeightService) {
        this.documentWeightService = documentWeightService;
    }

    @Override
    public String getName() {
        return "feedback";
    }

    @Override
    public String getDescription() {
        return I18N.get("score_contributor.feedback.description");
    }

    @Override
    public Map<String, Double> contribute(SearchContext context) {
        Map<String, Double> scores = new HashMap<>();

        if (documentWeightService == null || context.getRag() == null) {
            log.debug(I18N.get("log.score_contributor.feedback.disabled"));
            return scores;
        }

        // 获取所有文档的反馈权重 (Get feedback weights for all documents)
        // 这里我们需要结合其他来源的文档ID，所以暂时返回空
        // 实际使用时，会在 ScoreFusionService 中与其他贡献者的结果结合
        // (Actual usage will combine with results from other contributors in ScoreFusionService)

        log.debug(I18N.get("log.score_contributor.feedback.done", scores.size()));
        return scores;
    }

    /**
     * 为指定文档ID列表提供反馈评分调整
     * (Provide feedback score adjustments for specified document IDs)
     *
     * @param context 检索上下文 (Search context)
     * @param docIds 文档ID列表 (Document ID list)
     * @return 调整后的评分 (Adjusted scores)
     */
    public Map<String, Double> contributeForDocs(SearchContext context, Iterable<String> docIds) {
        Map<String, Double> scores = new HashMap<>();

        if (documentWeightService == null || context.getRag() == null) {
            return scores;
        }

        for (String docId : docIds) {
            Document doc = context.getRag().getDocument(docId);
            if (doc != null) {
                double feedbackWeight = documentWeightService.getDocumentWeight(doc.getTitle());
                // 将权重转换为评分贡献（偏离1.0的程度）
                // (Convert weight to score contribution - deviation from 1.0)
                scores.put(docId, feedbackWeight);
            }
        }

        log.debug(I18N.get("log.score_contributor.feedback.done", scores.size()));
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
    public boolean isEnabled() {
        return documentWeightService != null;
    }

    @Override
    public int getPriority() {
        return 30;
    }
}

