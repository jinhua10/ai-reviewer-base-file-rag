package top.yumbo.ai.rag.hope.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 近期问答记录 - 存储在中频层的高分问答
 * (Recent QA Record - High-rated QAs stored in ordinary layer)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentQA {

    /**
     * 唯一标识
     */
    private String id;

    /**
     * 问题
     */
    private String question;

    /**
     * 答案
     */
    private String answer;

    /**
     * 问题关键词（用于快速匹配）
     */
    private String[] keywords;

    /**
     * 评分（1-5）
     */
    private int rating;

    /**
     * 累计评分总和
     */
    private int totalRating;

    /**
     * 评分次数
     */
    private int ratingCount;

    /**
     * 访问次数
     */
    private long accessCount;

    /**
     * 来源文档
     */
    private String sourceDocuments;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;

    /**
     * 是否已晋升到低频层
     */
    private boolean promoted;

    /**
     * 获取平均评分
     */
    public double getAverageRating() {
        if (ratingCount == 0) {
            return rating;
        }
        return (double) totalRating / ratingCount;
    }

    /**
     * 记录访问
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 记录新评分
     */
    public void recordRating(int newRating) {
        this.totalRating += newRating;
        this.ratingCount++;
        this.rating = (int) Math.round(getAverageRating());
    }

    /**
     * 检查是否符合晋升条件
     */
    public boolean isEligibleForPromotion(int minAccessCount, double minAvgRating) {
        return !promoted
            && accessCount >= minAccessCount
            && getAverageRating() >= minAvgRating;
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired(int retentionDays) {
        if (createdAt == null) {
            return false;
        }
        return createdAt.plusDays(retentionDays).isBefore(LocalDateTime.now());
    }
}

