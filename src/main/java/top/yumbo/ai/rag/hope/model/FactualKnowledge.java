package top.yumbo.ai.rag.hope.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 确定性知识 - 存储在低频层的可直接回答的事实
 * (Factual Knowledge - Facts stored in permanent layer that can be directly answered)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactualKnowledge {

    /**
     * 知识唯一标识
     * (Unique knowledge ID)
     */
    private String id;

    /**
     * 问题模式（正则表达式或关键词）
     * (Question pattern - regex or keywords)
     */
    private String questionPattern;

    /**
     * 问题关键词列表
     * (Question keyword list)
     */
    private String[] keywords;

    /**
     * 确定性答案
     * (Definitive answer)
     */
    private String answer;

    /**
     * 知识来源（文档名称）
     * (Knowledge source - document name)
     */
    private String source;

    /**
     * 置信度 (0-1)，1.0 表示完全确定
     * (Confidence score 0-1, 1.0 means completely certain)
     */
    private double confidence;

    /**
     * 访问次数
     * (Access count)
     */
    private long accessCount;

    /**
     * 正面反馈次数
     * (Positive feedback count)
     */
    private long positiveCount;

    /**
     * 负面反馈次数
     * (Negative feedback count)
     */
    private long negativeCount;

    /**
     * 最后访问时间
     * (Last access time)
     */
    private LocalDateTime lastAccessed;

    /**
     * 创建时间
     * (Creation time)
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     * (Last update time)
     */
    private LocalDateTime updatedAt;

    /**
     * 是否启用
     * (Whether enabled)
     */
    private boolean enabled;

    /**
     * 获取满意度
     * (Get satisfaction rate)
     * 
     * @return 满意度分数 (0-1) (Satisfaction score (0-1))
     */
    public double getSatisfactionRate() {
        long total = positiveCount + negativeCount;
        if (total == 0) {
            return 1.0; // 默认满意 (Default satisfied)
        }
        return (double) positiveCount / total;
    }

    /**
     * 记录访问
     * (Record access)
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessed = LocalDateTime.now();
    }

    /**
     * 记录反馈
     * (Record feedback)
     * 
     * @param positive 是否为正面反馈 (Whether it's positive feedback)
     */
    public void recordFeedback(boolean positive) {
        if (positive) {
            this.positiveCount++;
        } else {
            this.negativeCount++;
        }
    }

    /**
     * 是否应该禁用（满意度过低）
     * (Whether should be disabled due to low satisfaction rate)
     * 
     * @return 是否应该禁用 (Whether should be disabled)
     */
    public boolean shouldDisable() {
        long total = positiveCount + negativeCount;
        return total >= 5 && getSatisfactionRate() < 0.5;
    }
}

