package top.yumbo.ai.rag.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 态度推断引擎 (Attitude Inference Engine)
 * 基于行为信号推断用户对答案的态度
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class AttitudeInferenceEngine {

    private static final Logger logger = LoggerFactory.getLogger(AttitudeInferenceEngine.class);

    /**
     * 信号加权器 (Signal Weighter)
     */
    private final SignalWeighter weighter;

    /**
     * 最小信号数量阈值 (Minimum Signal Count Threshold)
     * 少于此数量的信号，置信度会降低
     */
    private static final int MIN_SIGNAL_COUNT = 3;

    /**
     * 强信号权重加成 (Strong Signal Weight Bonus)
     */
    private static final double STRONG_SIGNAL_BONUS = 0.2;

    // ========== 构造函数 (Constructors) ==========

    public AttitudeInferenceEngine() {
        this.weighter = new SignalWeighter();
    }

    public AttitudeInferenceEngine(SignalWeighter weighter) {
        this.weighter = weighter;
    }

    // ========== 核心推断方法 (Core Inference Methods) ==========

    /**
     * 推断用户态度 (Infer User Attitude)
     *
     * @param signals 行为信号列表
     * @param userContext 用户上下文信息
     * @return 态度评分
     */
    public AttitudeScore inferAttitude(List<BehaviorSignalEvent> signals, Map<String, Object> userContext) {
        logger.debug(I18N.get("behavior.infer.start"), signals.size());

        // 1. 验证输入 (Validate input)
        if (signals == null || signals.isEmpty()) {
            logger.warn(I18N.get("behavior.infer.no_signals"));
            return createDefaultScore(signals);
        }

        // 2. 计算加权总分 (Calculate weighted score)
        double rawScore = calculateWeightedScore(signals, userContext);

        // 3. 计算置信度 (Calculate confidence)
        double confidence = calculateConfidence(signals, rawScore);

        // 4. 创建态度评分 (Create attitude score)
        AttitudeScore score = new AttitudeScore(
                extractUserId(signals),
                extractAnswerId(signals),
                rawScore,
                confidence
        );

        // 5. 设置支撑信号 (Set supporting signals)
        score.setSupportingSignals(signals);

        // 6. 生成解释 (Generate explanation)
        String explanation = generateExplanation(signals, rawScore, confidence);
        score.setExplanation(explanation);

        logger.info(I18N.get("behavior.infer.complete"),
                score.getLevel(), rawScore, confidence);

        return score;
    }

    /**
     * 计算加权总分 (Calculate Weighted Score)
     *
     * @param signals 信号列表
     * @param userContext 用户上下文
     * @return 原始评分（-1.0 到 1.0）
     */
    private double calculateWeightedScore(List<BehaviorSignalEvent> signals, Map<String, Object> userContext) {
        double totalWeightedValue = 0.0;
        double totalAbsWeight = 0.0;

        for (BehaviorSignalEvent signal : signals) {
            // 计算该信号的权重 (Calculate weight for this signal)
            double weight = weighter.calculateWeight(signal, userContext);

            // 累加加权值和权重 (Accumulate weighted value and weight)
            totalWeightedValue += weight;
            totalAbsWeight += Math.abs(weight);

            logger.trace(I18N.get("behavior.infer.signal_weight"),
                    signal.getSignalType(), weight);
        }

        // 避免除以零 (Avoid division by zero)
        if (totalAbsWeight == 0) {
            return 0.0;
        }

        // 归一化到 -1 到 1 范围 (Normalize to -1 to 1)
        double rawScore = totalWeightedValue / totalAbsWeight;

        // 限制范围 (Clamp range)
        return Math.max(-1.0, Math.min(1.0, rawScore));
    }

    /**
     * 计算置信度 (Calculate Confidence)
     *
     * @param signals 信号列表
     * @param rawScore 原始评分
     * @return 置信度（0.0 到 1.0）
     */
    public double calculateConfidence(List<BehaviorSignalEvent> signals, double rawScore) {
        // 1. 基础置信度：基于信号数量 (Base confidence: based on signal count)
        double countConfidence = calculateCountConfidence(signals.size());

        // 2. 一致性置信度：信号方向是否一致 (Consistency confidence)
        double consistencyConfidence = calculateConsistencyConfidence(signals, rawScore);

        // 3. 强信号加成 (Strong signal bonus)
        double strongSignalBonus = calculateStrongSignalBonus(signals);

        // 4. 综合置信度 (Combined confidence)
        double confidence = (countConfidence * 0.4 + consistencyConfidence * 0.4 + strongSignalBonus * 0.2);

        // 限制范围 (Clamp range)
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * 计算基于数量的置信度 (Calculate Count-based Confidence)
     * 信号数量越多，置信度越高
     */
    private double calculateCountConfidence(int signalCount) {
        if (signalCount <= 0) {
            return 0.0;
        } else if (signalCount < MIN_SIGNAL_COUNT) {
            // 少于最小数量，线性增长 (Less than minimum, linear growth)
            return (double) signalCount / MIN_SIGNAL_COUNT * 0.5;
        } else if (signalCount < 10) {
            // 3-10个信号，从0.5增长到0.9 (3-10 signals, grow from 0.5 to 0.9)
            return 0.5 + (signalCount - MIN_SIGNAL_COUNT) / (10.0 - MIN_SIGNAL_COUNT) * 0.4;
        } else {
            // 10个以上，趋近于1.0 (More than 10, approach 1.0)
            return Math.min(1.0, 0.9 + (signalCount - 10) * 0.01);
        }
    }

    /**
     * 计算一致性置信度 (Calculate Consistency Confidence)
     * 信号方向越一致，置信度越高
     */
    private double calculateConsistencyConfidence(List<BehaviorSignalEvent> signals, double rawScore) {
        if (signals.isEmpty()) {
            return 0.0;
        }

        // 计算与最终评分一致的信号比例 (Calculate proportion of consistent signals)
        int consistentCount = 0;
        boolean scoreIsPositive = rawScore > 0;

        for (BehaviorSignalEvent signal : signals) {
            boolean signalIsPositive = signal.getSignalType().isPositive();
            if (scoreIsPositive == signalIsPositive) {
                consistentCount++;
            }
        }

        double consistencyRatio = (double) consistentCount / signals.size();

        // 一致性越高，置信度越高 (Higher consistency = higher confidence)
        // 0.5 一致性 → 0.0 置信度
        // 1.0 一致性 → 1.0 置信度
        return Math.max(0.0, (consistencyRatio - 0.5) * 2.0);
    }

    /**
     * 计算强信号加成 (Calculate Strong Signal Bonus)
     * 强信号的存在会提升置信度
     */
    private double calculateStrongSignalBonus(List<BehaviorSignalEvent> signals) {
        long strongSignalCount = signals.stream()
                .filter(s -> s.getSignalType().isStrong())
                .count();

        // 每个强信号贡献0.2，最多1.0 (Each strong signal contributes 0.2, max 1.0)
        return Math.min(1.0, strongSignalCount * STRONG_SIGNAL_BONUS);
    }

    /**
     * 分类态度 (Classify Attitude)
     *
     * @param rawScore 原始评分
     * @return 态度等级
     */
    public AttitudeLevel classifyAttitude(double rawScore) {
        return AttitudeLevel.fromScore(rawScore);
    }

    /**
     * 生成解释 (Generate Explanation)
     *
     * @param signals 信号列表
     * @param rawScore 原始评分
     * @param confidence 置信度
     * @return 解释文本
     */
    public String generateExplanation(List<BehaviorSignalEvent> signals,
                                       double rawScore, double confidence) {
        StringBuilder explanation = new StringBuilder();

        // 1. 总结 (Summary)
        AttitudeLevel level = AttitudeLevel.fromScore(rawScore);
        explanation.append(I18N.get("behavior.infer.explanation.summary",
                level, rawScore, confidence));
        explanation.append("\n\n");

        // 2. 关键信号 (Key signals)
        explanation.append(I18N.get("behavior.infer.explanation.key_signals"));
        explanation.append("\n");

        // 按权重排序信号 (Sort signals by weight)
        List<BehaviorSignalEvent> sortedSignals = signals.stream()
                .sorted((a, b) -> Double.compare(
                        Math.abs(b.getWeightedValue()),
                        Math.abs(a.getWeightedValue())
                ))
                .limit(5) // 只显示前5个 (Show top 5)
                .collect(Collectors.toList());

        for (BehaviorSignalEvent signal : sortedSignals) {
            String direction = signal.getSignalType().isPositive() ? "+" : "-";
            explanation.append(String.format("  %s %s (%.2f)\n",
                    direction,
                    I18N.get("signal." + signal.getSignalType().getIdentifier()),
                    signal.getWeightedValue()));
        }

        // 3. 置信度说明 (Confidence explanation)
        explanation.append("\n");
        explanation.append(I18N.get("behavior.infer.explanation.confidence",
                signals.size(), confidence));

        return explanation.toString();
    }

    // ========== 辅助方法 (Helper Methods) ==========

    /**
     * 创建默认评分 (Create Default Score)
     * 当没有信号时返回中性评分
     */
    private AttitudeScore createDefaultScore(List<BehaviorSignalEvent> signals) {
        AttitudeScore score = new AttitudeScore();
        score.setRawScore(0.0);
        score.setConfidence(0.0);
        score.setExplanation(I18N.get("behavior.infer.no_signals_explanation"));

        if (signals != null && !signals.isEmpty()) {
            score.setUserId(extractUserId(signals));
            score.setAnswerId(extractAnswerId(signals));
        }

        return score;
    }

    /**
     * 提取用户ID (Extract User ID)
     */
    private String extractUserId(List<BehaviorSignalEvent> signals) {
        return signals.isEmpty() ? null : signals.get(0).getUserId();
    }

    /**
     * 提取答案ID (Extract Answer ID)
     */
    private String extractAnswerId(List<BehaviorSignalEvent> signals) {
        return signals.isEmpty() ? null : signals.get(0).getAnswerId();
    }

    /**
     * 批量推断态度 (Batch Infer Attitudes)
     *
     * @param signalsByAnswer Key: answerId, Value: 信号列表
     * @param userContext 用户上下文
     * @return Key: answerId, Value: 态度评分
     */
    public Map<String, AttitudeScore> batchInfer(Map<String, List<BehaviorSignalEvent>> signalsByAnswer,
                                                   Map<String, Object> userContext) {
        logger.info(I18N.get("behavior.infer.batch_start"), signalsByAnswer.size());

        Map<String, AttitudeScore> results = new HashMap<>();

        signalsByAnswer.forEach((answerId, signals) -> {
            AttitudeScore score = inferAttitude(signals, userContext);
            results.put(answerId, score);
        });

        logger.info(I18N.get("behavior.infer.batch_complete"), results.size());
        return results;
    }
}

