package top.yumbo.ai.rag.feedback;

import lombok.Data;

/**
 * 反馈统计 (Feedback Statistics)
 *
 * 记录反馈的统计信息
 * (Records feedback statistics)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
public class FeedbackStats {

    /**
     * 总反馈数 (Total feedback count)
     */
    private long totalCount = 0;

    /**
     * 显式反馈数 (Explicit feedback count)
     */
    private long explicitCount = 0;

    /**
     * 隐式反馈数 (Implicit feedback count)
     */
    private long implicitCount = 0;

    /**
     * 正面反馈数 (Positive feedback count)
     */
    private long positiveCount = 0;

    /**
     * 负面反馈数 (Negative feedback count)
     */
    private long negativeCount = 0;

    /**
     * 已处理反馈数 (Processed feedback count)
     */
    private long processedCount = 0;

    /**
     * 增加总数 (Increment total)
     */
    public void incrementTotal() {
        totalCount++;
    }

    /**
     * 增加显式反馈数 (Increment explicit)
     */
    public void incrementExplicit() {
        explicitCount++;
    }

    /**
     * 增加隐式反馈数 (Increment implicit)
     */
    public void incrementImplicit() {
        implicitCount++;
    }

    /**
     * 增加正面反馈数 (Increment positive)
     */
    public void incrementPositive() {
        positiveCount++;
    }

    /**
     * 增加负面反馈数 (Increment negative)
     */
    public void incrementNegative() {
        negativeCount++;
    }

    /**
     * 增加已处理数 (Increment processed)
     */
    public void incrementProcessed() {
        processedCount++;
    }

    /**
     * 获取显式反馈占比 (Get explicit ratio)
     *
     * @return 显式反馈占比 (Explicit ratio)
     */
    public double getExplicitRatio() {
        return totalCount == 0 ? 0.0 : (double) explicitCount / totalCount;
    }

    /**
     * 获取隐式反馈占比 (Get implicit ratio)
     *
     * @return 隐式反馈占比 (Implicit ratio)
     */
    public double getImplicitRatio() {
        return totalCount == 0 ? 0.0 : (double) implicitCount / totalCount;
    }

    /**
     * 获取正面反馈率 (Get positive rate)
     *
     * @return 正面反馈率 (Positive rate)
     */
    public double getPositiveRate() {
        long total = positiveCount + negativeCount;
        return total == 0 ? 0.0 : (double) positiveCount / total;
    }

    /**
     * 获取负面反馈率 (Get negative rate)
     *
     * @return 负面反馈率 (Negative rate)
     */
    public double getNegativeRate() {
        long total = positiveCount + negativeCount;
        return total == 0 ? 0.0 : (double) negativeCount / total;
    }

    /**
     * 获取处理进度 (Get processing progress)
     *
     * @return 处理进度 (Processing progress)
     */
    public double getProcessingProgress() {
        return totalCount == 0 ? 0.0 : (double) processedCount / totalCount;
    }

    /**
     * 获取待处理数量 (Get pending count)
     *
     * @return 待处理数量 (Pending count)
     */
    public long getPendingCount() {
        return totalCount - processedCount;
    }
}

