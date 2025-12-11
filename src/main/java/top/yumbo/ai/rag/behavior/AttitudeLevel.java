package top.yumbo.ai.rag.behavior;

/**
 * 态度等级枚举 (Attitude Level Enum)
 * 用户对答案的态度分类
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public enum AttitudeLevel {

    /**
     * 非常满意 (Very Satisfied)
     * 评分范围：0.7 ~ 1.0
     */
    VERY_SATISFIED("very_satisfied", 0.7, 1.0),

    /**
     * 满意 (Satisfied)
     * 评分范围：0.3 ~ 0.7
     */
    SATISFIED("satisfied", 0.3, 0.7),

    /**
     * 中性 (Neutral)
     * 评分范围：-0.3 ~ 0.3
     */
    NEUTRAL("neutral", -0.3, 0.3),

    /**
     * 不满意 (Dissatisfied)
     * 评分范围：-0.7 ~ -0.3
     */
    DISSATISFIED("dissatisfied", -0.7, -0.3),

    /**
     * 非常不满意 (Very Dissatisfied)
     * 评分范围：-1.0 ~ -0.7
     */
    VERY_DISSATISFIED("very_dissatisfied", -1.0, -0.7);

    private final String identifier;
    private final double minScore;
    private final double maxScore;

    AttitudeLevel(String identifier, double minScore, double maxScore) {
        this.identifier = identifier;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    /**
     * 根据评分判断态度等级 (Determine Level by Score)
     *
     * @param score 评分（-1.0 到 1.0）
     * @return 态度等级
     */
    public static AttitudeLevel fromScore(double score) {
        // 限制分数范围 (Clamp score range)
        score = Math.max(-1.0, Math.min(1.0, score));

        for (AttitudeLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }

        // 默认返回中性 (Default to neutral)
        return NEUTRAL;
    }

    /**
     * 判断是否为正面态度 (Is Positive Attitude)
     */
    public boolean isPositive() {
        return this == VERY_SATISFIED || this == SATISFIED;
    }

    /**
     * 判断是否为负面态度 (Is Negative Attitude)
     */
    public boolean isNegative() {
        return this == DISSATISFIED || this == VERY_DISSATISFIED;
    }
}

