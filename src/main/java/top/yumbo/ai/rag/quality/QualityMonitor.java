package top.yumbo.ai.rag.quality;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.feedback.Feedback;
import top.yumbo.ai.rag.feedback.FeedbackCollector;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 质量监控器 (Quality Monitor)
 *
 * 监控和评估概念的质量
 * (Monitors and evaluates concept quality)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class QualityMonitor {

    @Autowired(required = false)
    private FeedbackCollector feedbackCollector;

    /**
     * 质量指标存储 (Quality metrics storage)
     */
    private final Map<String, QualityMetrics> metricsStorage = new ConcurrentHashMap<>();

    /**
     * 使用统计 (Usage statistics)
     * Key: conceptId, Value: usage count
     */
    private final Map<String, Long> usageStats = new ConcurrentHashMap<>();

    /**
     * 监控概念质量 (Monitor concept quality)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 质量指标 (Quality metrics)
     */
    public QualityMetrics monitorQuality(String conceptId) {
        log.info(I18N.get("quality.monitor.start", conceptId));
        long startTime = System.currentTimeMillis();

        // 计算各项指标
        double accuracyScore = calculateAccuracyScore(conceptId);
        double freshnessScore = calculateFreshnessScore(conceptId);
        double popularityScore = calculatePopularityScore(conceptId);
        double disputeScore = calculateDisputeScore(conceptId);

        // 计算健康度（综合评分）
        double healthScore = calculateHealthScore(
                accuracyScore, freshnessScore, popularityScore, disputeScore);

        // 获取反馈率
        double[] feedbackRates = calculateFeedbackRates(conceptId);

        // 构建质量指标
        QualityMetrics metrics = QualityMetrics.builder()
                .conceptId(conceptId)
                .healthScore(healthScore)
                .accuracyScore(accuracyScore)
                .freshnessScore(freshnessScore)
                .popularityScore(popularityScore)
                .disputeScore(disputeScore)
                .usageCount(usageStats.getOrDefault(conceptId, 0L))
                .positiveRate(feedbackRates[0])
                .negativeRate(feedbackRates[1])
                .lastUpdated(new Date())
                .build();

        // 判断是否需要审查
        if (healthScore < 50) {
            metrics.setReviewStatus(QualityMetrics.ReviewStatus.NEEDS_REVIEW);
        }

        // 存储指标
        metricsStorage.put(conceptId, metrics);

        long duration = System.currentTimeMillis() - startTime;
        log.info(I18N.get("quality.monitor.complete",
                conceptId, healthScore, duration));

        return metrics;
    }

    /**
     * 记录使用 (Record usage)
     *
     * @param conceptId 概念ID (Concept ID)
     */
    public void recordUsage(String conceptId) {
        usageStats.merge(conceptId, 1L, Long::sum);
        log.debug(I18N.get("quality.usage.recorded",
                conceptId, usageStats.get(conceptId)));
    }

    /**
     * 获取质量指标 (Get quality metrics)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 质量指标 (Quality metrics)
     */
    public QualityMetrics getMetrics(String conceptId) {
        return metricsStorage.get(conceptId);
    }

    /**
     * 获取所有指标 (Get all metrics)
     *
     * @return 指标列表 (Metrics list)
     */
    public List<QualityMetrics> getAllMetrics() {
        return new ArrayList<>(metricsStorage.values());
    }

    /**
     * 获取问题概念 (Get problematic concepts)
     *
     * @return 问题概念列表 (Problematic concept list)
     */
    public List<QualityMetrics> getProblematicConcepts() {
        return metricsStorage.values().stream()
                .filter(m -> !m.isHealthy())
                .sorted((a, b) -> Double.compare(a.getHealthScore(), b.getHealthScore()))
                .toList();
    }

    /**
     * 获取需要审查的概念 (Get concepts needing review)
     *
     * @return 需要审查的概念列表 (Concepts needing review)
     */
    public List<QualityMetrics> getConceptsNeedingReview() {
        return metricsStorage.values().stream()
                .filter(QualityMetrics::needsReview)
                .toList();
    }

    /**
     * 获取仪表盘数据 (Get dashboard data)
     *
     * @return 仪表盘数据 (Dashboard data)
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        int totalConcepts = metricsStorage.size();
        long healthyConcepts = metricsStorage.values().stream()
                .filter(QualityMetrics::isHealthy)
                .count();
        long problematicConcepts = totalConcepts - healthyConcepts;
        long needsReview = metricsStorage.values().stream()
                .filter(QualityMetrics::needsReview)
                .count();

        double averageHealth = metricsStorage.values().stream()
                .mapToDouble(QualityMetrics::getHealthScore)
                .average()
                .orElse(0.0);

        dashboard.put("totalConcepts", totalConcepts);
        dashboard.put("healthyConcepts", healthyConcepts);
        dashboard.put("problematicConcepts", problematicConcepts);
        dashboard.put("needsReview", needsReview);
        dashboard.put("averageHealth", averageHealth);

        log.info(I18N.get("quality.dashboard.generated", totalConcepts, averageHealth));

        return dashboard;
    }

    /**
     * 计算准确度分数 (Calculate accuracy score)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 准确度分数 (Accuracy score)
     */
    private double calculateAccuracyScore(String conceptId) {
        // 基于正面反馈率
        double[] rates = calculateFeedbackRates(conceptId);
        return rates[0] * 100; // 正面率转为0-100分
    }

    /**
     * 计算新鲜度分数 (Calculate freshness score)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 新鲜度分数 (Freshness score)
     */
    private double calculateFreshnessScore(String conceptId) {
        QualityMetrics metrics = metricsStorage.get(conceptId);
        if (metrics == null || metrics.getLastUpdated() == null) {
            return 50.0; // 默认中等
        }

        // 基于最后更新时间
        long daysSinceUpdate = (new Date().getTime() - metrics.getLastUpdated().getTime())
                / (1000 * 60 * 60 * 24);

        if (daysSinceUpdate < 7) return 100.0;
        if (daysSinceUpdate < 30) return 80.0;
        if (daysSinceUpdate < 90) return 60.0;
        if (daysSinceUpdate < 180) return 40.0;
        return 20.0;
    }

    /**
     * 计算流行度分数 (Calculate popularity score)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 流行度分数 (Popularity score)
     */
    private double calculatePopularityScore(String conceptId) {
        long usage = usageStats.getOrDefault(conceptId, 0L);

        // 使用对数缩放
        if (usage == 0) return 0.0;
        if (usage < 10) return 30.0;
        if (usage < 50) return 50.0;
        if (usage < 100) return 70.0;
        if (usage < 500) return 85.0;
        return 100.0;
    }

    /**
     * 计算争议度分数 (Calculate dispute score)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 争议度分数 (Dispute score, 0-1)
     */
    private double calculateDisputeScore(String conceptId) {
        double[] rates = calculateFeedbackRates(conceptId);
        double negativeRate = rates[1];

        // 负面反馈率即为争议度
        return negativeRate;
    }

    /**
     * 计算健康度分数 (Calculate health score)
     *
     * @param accuracy 准确度 (Accuracy)
     * @param freshness 新鲜度 (Freshness)
     * @param popularity 流行度 (Popularity)
     * @param dispute 争议度 (Dispute)
     * @return 健康度分数 (Health score)
     */
    private double calculateHealthScore(double accuracy, double freshness,
                                       double popularity, double dispute) {
        // 权重：准确度30%，新鲜度25%，流行度20%，争议度25%（负向）
        double health = accuracy * 0.3
                      + freshness * 0.25
                      + popularity * 0.2
                      + (100 - dispute * 100) * 0.25;

        return Math.max(0, Math.min(100, health));
    }

    /**
     * 计算反馈率 (Calculate feedback rates)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return [正面率, 负面率] ([Positive rate, Negative rate])
     */
    private double[] calculateFeedbackRates(String conceptId) {
        if (feedbackCollector == null) {
            return new double[]{0.8, 0.2}; // 默认值
        }

        // 这里简化处理，实际应该从反馈收集器获取数据
        // 返回 [正面率, 负面率]
        return new double[]{0.75, 0.15};
    }

    /**
     * 清空所有数据 (Clear all data)
     * 仅用于测试 (For testing only)
     */
    public void clearAll() {
        metricsStorage.clear();
        usageStats.clear();
        log.info(I18N.get("quality.cleared"));
    }
}

