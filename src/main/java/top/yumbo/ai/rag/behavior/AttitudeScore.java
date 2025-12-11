package top.yumbo.ai.rag.behavior;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 态度评分 (Attitude Score)
 * 基于行为信号推断出的用户态度评分
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class AttitudeScore {

    /**
     * 用户ID (User ID)
     */
    @Setter
    private String userId;

    /**
     * 答案ID (Answer ID)
     */
    @Setter
    private String answerId;

    /**
     * 原始评分 (Raw Score)
     * 范围：-1.0（非常不满意）到 +1.0（非常满意）
     */
    private double rawScore;

    /**
     * 归一化评分 (Normalized Score)
     * 范围：0.0 到 1.0，用于与显式评分对齐
     */
    private double normalizedScore;

    /**
     * 置信度 (Confidence)
     * 范围：0.0 到 1.0，表示推断的可信程度
     */
    private double confidence;

    /**
     * 态度等级 (Attitude Level)
     */
    private AttitudeLevel level;

    /**
     * 支撑信号列表 (Supporting Signals)
     * 用于推断的行为信号
     */
    @Setter
    private List<BehaviorSignalEvent> supportingSignals;

    /**
     * 推断解释 (Inference Explanation)
     * 说明评分的依据
     */
    @Setter
    private String explanation;

    // ========== 构造函数 (Constructors) ==========

    public AttitudeScore() {
        this.supportingSignals = new ArrayList<>();
    }

    public AttitudeScore(String userId, String answerId, double rawScore, double confidence) {
        this();
        this.userId = userId;
        this.answerId = answerId;
        this.rawScore = rawScore;
        this.confidence = confidence;
        this.normalizedScore = normalizeScore(rawScore);
        this.level = AttitudeLevel.fromScore(rawScore);
    }

    // ========== Getter/Setter 方法 (Getter/Setter Methods) ==========

    public String getUserId() {
        return userId;
    }

    public String getAnswerId() {
        return answerId;
    }

    public double getRawScore() {
        return rawScore;
    }

    public void setRawScore(double rawScore) {
        this.rawScore = rawScore;
        this.normalizedScore = normalizeScore(rawScore);
        this.level = AttitudeLevel.fromScore(rawScore);
    }

    public double getNormalizedScore() {
        return normalizedScore;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public AttitudeLevel getLevel() {
        return level;
    }

    public List<BehaviorSignalEvent> getSupportingSignals() {
        return supportingSignals;
    }

    public String getExplanation() {
        return explanation;
    }

    // ========== 便捷方法 (Convenience Methods) ==========

    /**
     * 添加支撑信号 (Add Supporting Signal)
     */
    public void addSupportingSignal(BehaviorSignalEvent signal) {
        this.supportingSignals.add(signal);
    }

    /**
     * 将原始评分归一化到 0-1 范围 (Normalize Score to 0-1)
     * 公式：(score + 1) / 2
     */
    private double normalizeScore(double score) {
        return (score + 1.0) / 2.0;
    }

    /**
     * 判断是否为正面态度 (Is Positive)
     */
    public boolean isPositive() {
        return level != null && level.isPositive();
    }

    /**
     * 判断是否为负面态度 (Is Negative)
     */
    public boolean isNegative() {
        return level != null && level.isNegative();
    }

    /**
     * 判断置信度是否足够高 (Is Confident)
     * 置信度 >= 0.7 认为可信
     */
    public boolean isConfident() {
        return confidence >= 0.7;
    }

    @Override
    public String toString() {
        return String.format("AttitudeScore{userId='%s', answerId='%s', rawScore=%.2f, normalizedScore=%.2f, confidence=%.2f, level=%s}",
                userId, answerId, rawScore, normalizedScore, confidence, level);
    }
}

