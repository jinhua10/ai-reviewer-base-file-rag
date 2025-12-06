package top.yumbo.ai.rag.spring.boot.strategy;

import java.util.List;

/**
 * 多文档分析策略接口（Multi-Document Analysis Strategy Interface）
 *
 * <p>所有分析策略必须实现此接口，用于多文档联合分析</p>
 * <p>All analysis strategies must implement this interface for multi-document joint analysis</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
public interface MultiDocAnalysisStrategy {

    /**
     * 获取策略唯一标识（Get strategy unique identifier）
     *
     * @return 策略ID（Strategy ID）
     */
    String getId();

    /**
     * 获取策略名称（Get strategy name）
     *
     * @return 策略名称（Strategy name）
     */
    String getName();

    /**
     * 获取策略描述（Get strategy description）
     *
     * @return 策略描述（Strategy description）
     */
    String getDescription();

    /**
     * 获取策略能力声明（Get strategy capabilities）
     *
     * @return 能力声明对象（Capabilities object）
     */
    StrategyCapabilities getCapabilities();

    /**
     * 评估策略对当前任务的适用性（Evaluate strategy suitability for current task）
     *
     * <p>返回 0-100 的分数，分数越高表示越适合当前任务</p>
     * <p>Returns a score from 0-100, higher score means more suitable for current task</p>
     *
     * @param context 分析上下文（Analysis context）
     * @return 适用性评分 0-100（Suitability score 0-100）
     */
    int evaluateSuitability(AnalysisContext context);

    /**
     * 预估资源消耗（Estimate resource consumption）
     *
     * @param context 分析上下文（Analysis context）
     * @return 资源估算（Resource estimate）
     */
    ResourceEstimate estimateResources(AnalysisContext context);

    /**
     * 执行分析（Execute analysis）
     *
     * @param context 分析上下文（Analysis context）
     * @param progressCallback 进度回调，可为空（Progress callback, can be null）
     * @return 分析结果（Analysis result）
     */
    AnalysisResult analyze(AnalysisContext context, ProgressCallback progressCallback);

    /**
     * 是否支持与其他策略组合（Whether supports combination with other strategies）
     *
     * @param other 其他策略（Other strategy）
     * @return 是否可组合（Whether can combine）
     */
    default boolean canCombineWith(MultiDocAnalysisStrategy other) {
        return true;
    }

    /**
     * 获取推荐的组合策略（Get recommended combination strategies）
     *
     * @return 推荐策略ID列表（List of recommended strategy IDs）
     */
    default List<String> getRecommendedCombinations() {
        return List.of();
    }
}

