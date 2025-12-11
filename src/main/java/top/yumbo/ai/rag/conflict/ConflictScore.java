package top.yumbo.ai.rag.conflict;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 冲突评分 (Conflict Score)
 *
 * 记录冲突的各项评分指标
 * (Records various scoring metrics for conflicts)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictScore {

    /**
     * 相似度分数 (Similarity score, 0-1)
     * 内容相似度越高，越可能冲突
     */
    private double similarityScore;

    /**
     * 差异度分数 (Difference score, 0-1)
     * 关键差异越大，冲突越严重
     */
    private double differenceScore;

    /**
     * 置信度分数 (Confidence score, 0-1)
     * 检测的置信度
     */
    private double confidenceScore;

    /**
     * 综合评分 (Final score, 0-1)
     * 综合各项指标的最终分数
     */
    private double finalScore;

    /**
     * 是否高置信度 (Is high confidence)
     *
     * @return 是否高置信度 (Whether high confidence)
     */
    public boolean isHighConfidence() {
        return confidenceScore >= 0.8;
    }

    /**
     * 是否明显冲突 (Is obvious conflict)
     *
     * @return 是否明显冲突 (Whether obvious conflict)
     */
    public boolean isObviousConflict() {
        return similarityScore >= 0.85 && differenceScore >= 0.6;
    }

    /**
     * 计算综合评分 (Calculate final score)
     *
     * @return 综合评分 (Final score)
     */
    public double calculateFinalScore() {
        // 相似度权重0.4，差异度权重0.4，置信度权重0.2
        finalScore = similarityScore * 0.4 + differenceScore * 0.4 + confidenceScore * 0.2;
        return finalScore;
    }
}

