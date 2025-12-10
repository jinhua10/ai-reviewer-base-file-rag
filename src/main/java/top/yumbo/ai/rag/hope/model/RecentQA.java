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
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentQA {

    /**
     * 唯一标识
     * (Unique ID)
     */
    private String id;

    /**
     * 问题
     * (Question)
     */
    private String question;

    /**
     * 答案
     * (Answer)
     */
    private String answer;

    /**
     * 问题关键词（用于快速匹配）
     * (Question keywords for quick matching)
     */
    private String[] keywords;

    /**
     * 评分（1-5）
     * (Rating 1-5)
     */
    private int rating;

    /**
     * 累计评分总和
     * (Cumulative rating sum)
     */
    private int totalRating;

    /**
     * 评分次数
     * (Rating count)
     */
    private int ratingCount;

    /**
     * 访问次数
     * (Access count)
     */
    private long accessCount;

    /**
     * 来源文档
     * (Source documents)
     */
    private String sourceDocuments;

    /**
     * 创建时间
     * (Creation time)
     */
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     * (Last access time)
     */
    private LocalDateTime lastAccessedAt;

    /**
     * 是否已晋升到低频层
     * (Whether promoted to permanent layer)
     */
    private boolean promoted;

    /**
     * 会话ID（用于流式响应追踪）
     * (Session ID for streaming response tracking)
     */
    private String sessionId;

    /**
     * 相似度评分（用于查询时）
     * (Similarity score when queried)
     */
    private Double similarityScore;

    /**
     * 完成时间（流式响应）
     * (Completion time for streaming response)
     */
    private LocalDateTime completedAt;

    /**
     * 响应时长（秒）
     * (Response time in seconds)
     */
    private Long responseTimeSeconds;

    /**
     * 获取平均评分
     * (Get average rating)
     * 
     * @return 平均评分 (Average rating)
     */
    public double getAverageRating() {
        if (ratingCount == 0) {
            return rating;
        }
        return (double) totalRating / ratingCount;
    }

    /**
     * 记录访问
     * (Record access)
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 记录新评分
     * (Record new rating)
     * 
     * @param newRating 新评分 (New rating)
     */
    public void recordRating(int newRating) {
        this.totalRating += newRating;
        this.ratingCount++;
        this.rating = (int) Math.round(getAverageRating());
    }

    /**
     * 检查是否符合晋升条件
     * (Check if eligible for promotion)
     * 
     * @param minAccessCount 最小访问次数 (Minimum access count)
     * @param minAvgRating 最小平均评分 (Minimum average rating)
     * @return 是否符合晋升条件 (Whether eligible for promotion)
     */
    public boolean isEligibleForPromotion(int minAccessCount, double minAvgRating) {
        return !promoted
            && accessCount >= minAccessCount
            && getAverageRating() >= minAvgRating;
    }

    /**
     * 检查是否过期
     * (Check if expired)
     * 
     * @param retentionDays 保留天数 (Retention days)
     * @return 是否过期 (Whether expired)
     */
    public boolean isExpired(int retentionDays) {
        if (createdAt == null) {
            return false;
        }
        return createdAt.plusDays(retentionDays).isBefore(LocalDateTime.now());
    }
}

