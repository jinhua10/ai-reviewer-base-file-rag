package top.yumbo.ai.rag.spring.boot.strategy;

import java.util.List;
import java.util.Map;

/**
 * 多文档分析策略接口
 * (Multi-Document Analysis Strategy Interface)
 *
 * 所有分析策略必须实现此接口
 * (All analysis strategies must implement this interface)
 */
public interface MultiDocAnalysisStrategy {

    /**
     * 获取策略唯一标识
     * (Get strategy unique identifier)
     */
    String getId();

    /**
     * 获取策略名称
     * (Get strategy name)
     */
    String getName();

    /**
     * 获取策略描述
     * (Get strategy description)
     */
    String getDescription();

    /**
     * 获取策略能力声明
     * (Get strategy capabilities)
     */
    StrategyCapabilities getCapabilities();

    /**
     * 评估策略对当前任务的适用性
     * (Evaluate strategy suitability for current task)
     *
     * @param context 分析上下文
     * @return 适用性评分 0-100
     */
    int evaluateSuitability(AnalysisContext context);

    /**
     * 预估资源消耗
     * (Estimate resource consumption)
     */
    ResourceEstimate estimateResources(AnalysisContext context);

    /**
     * 执行分析
     * (Execute analysis)
     *
     * @param context 分析上下文
     * @param progressCallback 进度回调（可选）
     * @return 分析结果
     */
    AnalysisResult analyze(AnalysisContext context, ProgressCallback progressCallback);

    /**
     * 是否支持与其他策略组合
     * (Whether supports combination with other strategies)
     */
    default boolean canCombineWith(MultiDocAnalysisStrategy other) {
        return true;
    }

    /**
     * 获取推荐的组合策略
     * (Get recommended combination strategies)
     */
    default List<String> getRecommendedCombinations() {
        return List.of();
    }
}

