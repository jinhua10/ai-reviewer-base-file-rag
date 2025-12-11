package top.yumbo.ai.rag.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 质量指标 (Quality Metrics)
 *
 * 记录概念的质量评分
 * (Records quality scores for concepts)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityMetrics {

    /**
     * 概念ID (Concept ID)
     */
    private String conceptId;

    /**
     * 健康度分数 (Health score, 0-100)
     */
    private double healthScore;

    /**
     * 准确度分数 (Accuracy score, 0-100)
     */
    private double accuracyScore;

    /**
     * 新鲜度分数 (Freshness score, 0-100)
     */
    private double freshnessScore;

    /**
     * 流行度分数 (Popularity score, 0-100)
     */
    private double popularityScore;

    /**
     * 争议度分数 (Dispute score, 0-1)
     */
    private double disputeScore;

    /**
     * 使用次数 (Usage count)
     */
    private long usageCount;

    /**
     * 正面反馈率 (Positive rate, 0-1)
     */
    private double positiveRate;

    /**
     * 负面反馈率 (Negative rate, 0-1)
     */
    private double negativeRate;

    /**
     * 最后更新时间 (Last updated)
     */
    @Builder.Default
    private Date lastUpdated = new Date();

    /**
     * 审查状态 (Review status)
     */
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.NORMAL;

    /**
     * 是否健康 (Is healthy)
     *
     * @return 是否健康 (Whether healthy)
     */
    public boolean isHealthy() {
        return healthScore > 70;
    }

    /**
     * 是否需要审查 (Needs review)
     *
     * @return 是否需要审查 (Whether needs review)
     */
    public boolean needsReview() {
        return reviewStatus == ReviewStatus.NEEDS_REVIEW || healthScore < 50;
    }

    /**
     * 是否有争议 (Is disputed)
     *
     * @return 是否有争议 (Whether disputed)
     */
    public boolean isDisputed() {
        return disputeScore > 0.5;
    }

    /**
     * 获取综合评分 (Get overall score)
     *
     * @return 综合评分 (Overall score)
     */
    public double getOverallScore() {
        return healthScore;
    }

    /**
     * 审查状态 (Review Status)
     */
    public enum ReviewStatus {
        /**
         * 正常 (Normal)
         */
        NORMAL,

        /**
         * 需要审查 (Needs review)
         */
        NEEDS_REVIEW,

        /**
         * 审查中 (Under review)
         */
        UNDER_REVIEW,

        /**
         * 已审查 (Reviewed)
         */
        REVIEWED
    }
}

