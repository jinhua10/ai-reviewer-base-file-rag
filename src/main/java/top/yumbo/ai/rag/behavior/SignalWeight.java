package top.yumbo.ai.rag.behavior;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 信号权重配置 (Signal Weight Configuration)
 * 存储和管理不同信号的权重配置
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
@Getter
public class SignalWeight {

    /**
     * 信号类型权重映射 (Signal Type Weight Mapping)
     */
    private Map<SignalType, Double> weights;

    /**
     * 上下文权重调整因子 (Context Weight Adjustments)
     */
    private Map<String, Double> contextAdjustments;

    // ========== 构造函数 (Constructors) ==========

    public SignalWeight() {
        this.weights = new HashMap<>();
        this.contextAdjustments = new HashMap<>();
        initializeDefaultWeights();
    }

    /**
     * 初始化默认权重 (Initialize Default Weights)
     */
    private void initializeDefaultWeights() {
        // 使用枚举中定义的基础权重 (Use base weights from enum)
        for (SignalType type : SignalType.values()) {
            weights.put(type, type.getBaseWeight());
        }
    }

    // ========== Getter/Setter 方法 (Getter/Setter Methods) ==========

    public void setWeights(Map<SignalType, Double> weights) {
        this.weights = weights;
    }

    public void setContextAdjustments(Map<String, Double> contextAdjustments) {
        this.contextAdjustments = contextAdjustments;
    }

    // ========== 业务方法 (Business Methods) ==========

    /**
     * 获取信号权重 (Get Signal Weight)
     */
    public double getWeight(SignalType type) {
        return weights.getOrDefault(type, type.getBaseWeight());
    }

    /**
     * 设置信号权重 (Set Signal Weight)
     */
    public void setWeight(SignalType type, double weight) {
        weights.put(type, weight);
    }

    /**
     * 添加上下文调整因子 (Add Context Adjustment)
     *
     * @param contextKey 上下文键（如 "user_role:expert"）
     * @param adjustment 调整因子（乘数，如 1.5 表示权重增加50%）
     */
    public void addContextAdjustment(String contextKey, double adjustment) {
        contextAdjustments.put(contextKey, adjustment);
    }

    /**
     * 获取上下文调整因子 (Get Context Adjustment)
     */
    public double getContextAdjustment(String contextKey) {
        return contextAdjustments.getOrDefault(contextKey, 1.0);
    }

    /**
     * 计算调整后的权重 (Calculate Adjusted Weight)
     *
     * @param type 信号类型
     * @param contextKeys 适用的上下文键列表
     * @return 调整后的权重
     */
    public double calculateAdjustedWeight(SignalType type, String... contextKeys) {
        double baseWeight = getWeight(type);
        double adjustmentFactor = 1.0;

        // 应用所有上下文调整因子 (Apply all context adjustments)
        for (String key : contextKeys) {
            adjustmentFactor *= getContextAdjustment(key);
        }

        return baseWeight * adjustmentFactor;
    }

    /**
     * 重置为默认权重 (Reset to Default Weights)
     */
    public void resetToDefaults() {
        weights.clear();
        contextAdjustments.clear();
        initializeDefaultWeights();
    }
}

