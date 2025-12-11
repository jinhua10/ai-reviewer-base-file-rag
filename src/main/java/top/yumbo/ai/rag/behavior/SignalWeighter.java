package top.yumbo.ai.rag.behavior;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 信号加权器 (Signal Weighter)
 * 根据上下文动态调整信号权重
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
@Getter
public class SignalWeighter {

    private static final Logger logger = LoggerFactory.getLogger(SignalWeighter.class);

    /**
     * 信号权重配置 (Signal Weight Configuration)
     * -- GETTER --
     *  获取信号权重配置 (Get Signal Weight Configuration)

     */
    private final SignalWeight signalWeight;

    // ========== 构造函数 (Constructors) ==========

    public SignalWeighter() {
        this.signalWeight = new SignalWeight();
        initializeContextAdjustments();
    }

    /**
     * 初始化上下文调整因子 (Initialize Context Adjustments)
     */
    private void initializeContextAdjustments() {
        // 用户角色调整 (User role adjustments)
        signalWeight.addContextAdjustment("role:expert", 1.5);      // 专家权重高50%
        signalWeight.addContextAdjustment("role:power_user", 1.3);  // 活跃用户高30%
        signalWeight.addContextAdjustment("role:regular", 1.0);     // 普通用户标准权重
        signalWeight.addContextAdjustment("role:new_user", 0.8);    // 新用户低20%

        // 用户历史准确率调整 (Historical accuracy adjustments)
        signalWeight.addContextAdjustment("accuracy:high", 1.4);    // 高准确率 (>85%)
        signalWeight.addContextAdjustment("accuracy:medium", 1.0);  // 中等准确率 (60-85%)
        signalWeight.addContextAdjustment("accuracy:low", 0.7);     // 低准确率 (<60%)

        // 用户活跃度调整 (User activity adjustments)
        signalWeight.addContextAdjustment("activity:high", 1.2);    // 高活跃度
        signalWeight.addContextAdjustment("activity:medium", 1.0);  // 中等活跃度
        signalWeight.addContextAdjustment("activity:low", 0.9);     // 低活跃度

        logger.info(I18N.get("behavior.weight.initialized"));
    }

    // ========== 核心加权方法 (Core Weighting Methods) ==========

    /**
     * 计算信号的最终权重 (Calculate Final Weight)
     *
     * @param event 信号事件
     * @param userContext 用户上下文信息
     * @return 最终权重
     */
    public double calculateWeight(BehaviorSignalEvent event, Map<String, Object> userContext) {
        // 1. 获取基础权重 (Get base weight)
        double baseWeight = signalWeight.getWeight(event.getSignalType());

        // 2. 应用信号强度 (Apply signal strength)
        double weightedValue = baseWeight * event.getStrength();

        // 3. 根据用户上下文调整 (Adjust by user context)
        double contextAdjustment = calculateContextAdjustment(userContext);

        // 4. 应用时间衰减 (Apply time decay)
        double timeDecay = calculateTimeDecay(event.getTimestamp());

        // 5. 计算最终权重 (Calculate final weight)
        double finalWeight = weightedValue * contextAdjustment * timeDecay;

        logger.debug(I18N.get("behavior.weight.calculated"),
                event.getSignalType(), baseWeight, finalWeight);

        return finalWeight;
    }

    /**
     * 根据上下文计算调整因子 (Calculate Context Adjustment)
     *
     * @param userContext 用户上下文
     * @return 调整因子（乘数）
     */
    private double calculateContextAdjustment(Map<String, Object> userContext) {
        if (userContext == null || userContext.isEmpty()) {
            return 1.0;
        }

        double adjustment = 1.0;

        // 用户角色调整 (User role adjustment)
        String role = (String) userContext.get("role");
        if (role != null) {
            adjustment *= signalWeight.getContextAdjustment("role:" + role);
        }

        // 历史准确率调整 (Historical accuracy adjustment)
        Double accuracy = (Double) userContext.get("accuracy");
        if (accuracy != null) {
            String accuracyLevel = getAccuracyLevel(accuracy);
            adjustment *= signalWeight.getContextAdjustment("accuracy:" + accuracyLevel);
        }

        // 活跃度调整 (Activity adjustment)
        String activity = (String) userContext.get("activity");
        if (activity != null) {
            adjustment *= signalWeight.getContextAdjustment("activity:" + activity);
        }

        return adjustment;
    }

    /**
     * 计算时间衰减因子 (Calculate Time Decay)
     * 越早的信号，权重越高，避免从众效应
     *
     * @param timestamp 信号时间戳
     * @return 衰减因子（0.5 ~ 1.0）
     */
    private double calculateTimeDecay(LocalDateTime timestamp) {
        Duration age = Duration.between(timestamp, LocalDateTime.now());
        long minutes = age.toMinutes();

        // 30分钟内无衰减 (No decay within 30 minutes)
        if (minutes <= 30) {
            return 1.0;
        }

        // 30分钟后线性衰减到0.5 (Linear decay to 0.5 after 30 minutes)
        // 公式：1.0 - (minutes - 30) / 360 (6小时后降至0.5)
        double decay = 1.0 - Math.min(0.5, (minutes - 30) / 360.0);
        return Math.max(0.5, decay);
    }

    /**
     * 归一化权重到指定范围 (Normalize Weights)
     *
     * @param weights 原始权重列表
     * @param targetMin 目标最小值
     * @param targetMax 目标最大值
     * @return 归一化后的权重
     */
    public Map<String, Double> normalizeWeights(Map<String, Double> weights,
                                                  double targetMin, double targetMax) {
        if (weights == null || weights.isEmpty()) {
            return new HashMap<>();
        }

        // 找出最小值和最大值 (Find min and max)
        double min = weights.values().stream().min(Double::compare).orElse(0.0);
        double max = weights.values().stream().max(Double::compare).orElse(1.0);

        // 归一化 (Normalize)
        Map<String, Double> normalized = new HashMap<>();
        double range = max - min;

        if (range == 0) {
            // 所有权重相同，归一化到中间值 (All same, normalize to middle)
            double midValue = (targetMin + targetMax) / 2;
            weights.keySet().forEach(key -> normalized.put(key, midValue));
        } else {
            // Min-Max归一化 (Min-Max normalization)
            weights.forEach((key, value) -> {
                double normalizedValue = ((value - min) / range) * (targetMax - targetMin) + targetMin;
                normalized.put(key, normalizedValue);
            });
        }

        logger.debug(I18N.get("behavior.weight.normalized"), weights.size(), targetMin, targetMax);
        return normalized;
    }

    /**
     * 应用权重到分数 (Apply Weights to Scores)
     *
     * @param scores 原始分数
     * @param weights 权重
     * @return 加权后的分数
     */
    public double applyWeights(Map<SignalType, Double> scores, Map<SignalType, Double> weights) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<SignalType, Double> entry : scores.entrySet()) {
            SignalType type = entry.getKey();
            double score = entry.getValue();
            double weight = weights.getOrDefault(type, 1.0);

            weightedSum += score * weight;
            totalWeight += Math.abs(weight);
        }

        // 避免除以零 (Avoid division by zero)
        if (totalWeight == 0) {
            return 0.0;
        }

        return weightedSum / totalWeight;
    }

    // ========== 辅助方法 (Helper Methods) ==========

    /**
     * 获取准确率等级 (Get Accuracy Level)
     */
    private String getAccuracyLevel(double accuracy) {
        if (accuracy >= 0.85) {
            return "high";
        } else if (accuracy >= 0.60) {
            return "medium";
        } else {
            return "low";
        }
    }

    /**
     * 定义自定义权重 (Define Custom Weight)
     */
    public void defineWeight(SignalType type, double weight) {
        signalWeight.setWeight(type, weight);
        logger.info(I18N.get("behavior.weight.custom_defined"), type, weight);
    }

    /**
     * 定义自定义上下文调整 (Define Custom Context Adjustment)
     */
    public void defineContextAdjustment(String contextKey, double adjustment) {
        signalWeight.addContextAdjustment(contextKey, adjustment);
        logger.info(I18N.get("behavior.weight.context_defined"), contextKey, adjustment);
    }
}

