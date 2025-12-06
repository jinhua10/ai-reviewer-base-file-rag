package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能策略调度器（Intelligent Strategy Dispatcher）
 *
 * <p>负责策略的注册、管理和智能调度</p>
 * <p>Responsible for strategy registration, management and intelligent dispatching</p>
 *
 * <p>主要功能（Main functions）:</p>
 * <ul>
 *   <li>注册和管理所有策略（Register and manage all strategies）</li>
 *   <li>根据上下文智能选择最佳策略（Intelligently select best strategy based on context）</li>
 *   <li>组合多个策略执行（Combine multiple strategies for execution）</li>
 *   <li>监控策略执行效果（Monitor strategy execution performance）</li>
 * </ul>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Service
@Slf4j
public class StrategyDispatcher {

    @Autowired
    private List<MultiDocAnalysisStrategy> strategies;

    /** 策略映射表（Strategy map） */
    private final Map<String, MultiDocAnalysisStrategy> strategyMap = new ConcurrentHashMap<>();

    /** 策略使用统计（Strategy usage statistics） */
    private final Map<String, StrategyStats> strategyStats = new ConcurrentHashMap<>();

    /**
     * 初始化调度器（Initialize dispatcher）
     */
    @PostConstruct
    public void init() {
        log.info(I18N.get("strategy.dispatcher.log.init_start"));

        for (MultiDocAnalysisStrategy strategy : strategies) {
            strategyMap.put(strategy.getId(), strategy);
            strategyStats.put(strategy.getId(), new StrategyStats());
            log.info(I18N.get("strategy.dispatcher.log.strategy_registered",
                    strategy.getId(), strategy.getName()));
        }

        log.info(I18N.get("strategy.dispatcher.log.init_complete", strategyMap.size()));
    }

    /**
     * 执行智能分析（Execute smart analysis）
     *
     * <p>根据上下文自动选择最佳策略组合</p>
     * <p>Automatically select best strategy combination based on context</p>
     *
     * @param context 分析上下文（Analysis context）
     * @param callback 进度回调（Progress callback）
     * @return 分析结果（Analysis result）
     */
    public AnalysisResult analyze(AnalysisContext context, ProgressCallback callback) {
        long startTime = System.currentTimeMillis();

        // 选择策略（Select strategies）
        List<MultiDocAnalysisStrategy> selectedStrategies = selectStrategies(context);

        if (selectedStrategies.isEmpty()) {
            log.warn(I18N.get("strategy.dispatcher.log.no_suitable_strategy"));
            return AnalysisResult.failure(I18N.get("strategy.dispatcher.log.no_suitable_strategy"));
        }

        log.info(I18N.get("strategy.dispatcher.log.selected_strategies",
                selectedStrategies.stream().map(MultiDocAnalysisStrategy::getId).toList()));

        // 执行策略（Execute strategies）
        AnalysisResult result;
        if (selectedStrategies.size() == 1) {
            result = executeSingle(selectedStrategies.get(0), context, callback);
        } else {
            result = executeCombined(selectedStrategies, context, callback);
        }

        // 更新统计（Update statistics）
        long executionTime = System.currentTimeMillis() - startTime;
        for (MultiDocAnalysisStrategy strategy : selectedStrategies) {
            updateStats(strategy.getId(), result.isSuccess(), executionTime);
        }

        return result;
    }

    /**
     * 执行指定策略（Execute specified strategies）
     *
     * @param context 分析上下文（Analysis context）
     * @param strategyIds 策略ID列表（List of strategy IDs）
     * @param callback 进度回调（Progress callback）
     * @return 分析结果（Analysis result）
     */
    public AnalysisResult analyzeWithStrategies(
            AnalysisContext context,
            List<String> strategyIds,
            ProgressCallback callback) {

        List<MultiDocAnalysisStrategy> selectedStrategies = strategyIds.stream()
                .map(strategyMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (selectedStrategies.isEmpty()) {
            log.warn(I18N.get("strategy.dispatcher.log.no_suitable_strategy") + ": {}", strategyIds);
            return AnalysisResult.failure(I18N.get("strategy.dispatcher.log.no_suitable_strategy") + ": " + strategyIds);
        }

        if (selectedStrategies.size() == 1) {
            return executeSingle(selectedStrategies.get(0), context, callback);
        } else {
            return executeCombined(selectedStrategies, context, callback);
        }
    }

    /**
     * 选择最佳策略（Select best strategies）
     */
    private List<MultiDocAnalysisStrategy> selectStrategies(AnalysisContext context) {
        // 如果指定了策略，直接使用（If strategies specified, use them directly）
        if (context.getStrategies() != null && !context.getStrategies().isEmpty()) {
            return context.getStrategies().stream()
                    .map(strategyMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 评估所有策略的适用性（Evaluate suitability of all strategies）
        List<StrategyScore> scores = new ArrayList<>();

        for (MultiDocAnalysisStrategy strategy : strategies) {
            try {
                int suitability = strategy.evaluateSuitability(context);
                if (suitability > 0) {
                    // 结合历史统计调整分数（Adjust score based on historical stats）
                    double adjustedScore = adjustScoreWithStats(strategy.getId(), suitability);
                    scores.add(new StrategyScore(strategy, adjustedScore));
                }
            } catch (Exception e) {
                log.warn(I18N.get("strategy.dispatcher.log.evaluate_failed",
                        strategy.getId(), e.getMessage()));
            }
        }

        // 按分数排序（Sort by score）
        scores.sort((a, b) -> Double.compare(b.score, a.score));

        // 选择最佳策略（Select best strategies）
        if (scores.isEmpty()) {
            return List.of();
        }

        // 如果最高分策略明显优于其他，单独使用（If top strategy significantly better, use alone）
        if (scores.size() == 1 || scores.get(0).score > scores.get(1).score * 1.5) {
            return List.of(scores.get(0).strategy);
        }

        // 否则组合使用得分相近的策略（Otherwise combine strategies with similar scores）
        List<MultiDocAnalysisStrategy> selected = new ArrayList<>();
        double threshold = scores.get(0).score * 0.8;

        for (StrategyScore score : scores) {
            if (score.score >= threshold && selected.size() < 2) {
                selected.add(score.strategy);
            }
        }

        return selected;
    }

    /**
     * 执行单个策略（Execute single strategy）
     */
    private AnalysisResult executeSingle(
            MultiDocAnalysisStrategy strategy,
            AnalysisContext context,
            ProgressCallback callback) {

        log.info(I18N.get("strategy.dispatcher.log.executing_single", strategy.getId()));
        return strategy.analyze(context, callback);
    }

    /**
     * 组合执行多个策略（Execute combined strategies）
     */
    private AnalysisResult executeCombined(
            List<MultiDocAnalysisStrategy> strategies,
            AnalysisContext context,
            ProgressCallback callback) {

        log.info(I18N.get("strategy.dispatcher.log.executing_combined",
                strategies.stream().map(MultiDocAnalysisStrategy::getId).toList()));

        List<AnalysisResult> results = new ArrayList<>();
        int progressPerStrategy = 80 / strategies.size();
        int currentProgress = 10;

        for (int i = 0; i < strategies.size(); i++) {
            MultiDocAnalysisStrategy strategy = strategies.get(i);
            final int strategyProgress = currentProgress;

            ProgressCallback wrappedCallback = (progress, message) -> {
                int overallProgress = strategyProgress + (progress * progressPerStrategy / 100);
                callback.onProgress(overallProgress, strategy.getName() + ": " + message);
            };

            AnalysisResult result = strategy.analyze(context, wrappedCallback);
            results.add(result);

            currentProgress += progressPerStrategy;
        }

        // 合并结果（Merge results）
        callback.onProgress(90, I18N.get("strategy.dispatcher.log.merging_results"));
        return mergeResults(results, strategies);
    }

    /**
     * 合并多个策略的结果（Merge results from multiple strategies）
     */
    private AnalysisResult mergeResults(
            List<AnalysisResult> results,
            List<MultiDocAnalysisStrategy> strategies) {

        StringBuilder combinedAnswer = new StringBuilder();
        List<String> allKeyPoints = new ArrayList<>();
        List<AnalysisResult.DocumentRelation> allRelations = new ArrayList<>();
        List<String> strategiesUsed = new ArrayList<>();
        long totalTime = 0;
        int totalTokens = 0;

        for (int i = 0; i < results.size(); i++) {
            AnalysisResult result = results.get(i);
            MultiDocAnalysisStrategy strategy = strategies.get(i);

            if (result.isSuccess()) {
                combinedAnswer.append("## ").append(strategy.getName()).append(" 分析结果(Analysis Result)\n\n");
                combinedAnswer.append(result.getAnswer()).append("\n\n");

                if (result.getKeyPoints() != null) {
                    allKeyPoints.addAll(result.getKeyPoints());
                }
                if (result.getRelations() != null) {
                    allRelations.addAll(result.getRelations());
                }
            }

            strategiesUsed.add(strategy.getId());
            totalTime += result.getExecutionTimeMs();
            totalTokens += result.getTokensUsed();
        }

        // 去重关键点（Deduplicate key points）
        List<String> uniqueKeyPoints = allKeyPoints.stream()
                .distinct()
                .limit(15)
                .collect(Collectors.toList());

        return AnalysisResult.builder()
                .success(true)
                .answer(combinedAnswer.toString())
                .comprehensiveSummary(combinedAnswer.toString())
                .finalReport(combinedAnswer.toString())
                .keyPoints(uniqueKeyPoints)
                .relations(allRelations)
                .strategiesUsed(strategiesUsed)
                .executionTimeMs(totalTime)
                .tokensUsed(totalTokens)
                .build();
    }

    /**
     * 根据历史统计调整分数（Adjust score based on historical statistics）
     */
    private double adjustScoreWithStats(String strategyId, int baseSuitability) {
        StrategyStats stats = strategyStats.get(strategyId);
        if (stats == null || stats.totalExecutions < 5) {
            return baseSuitability;
        }

        // 成功率调整（Success rate adjustment）
        double successRate = (double) stats.successCount / stats.totalExecutions;
        double adjustment = successRate * 10 - 5; // -5 到 +5 的调整（Adjustment from -5 to +5）

        return baseSuitability + adjustment;
    }

    /**
     * 更新策略统计（Update strategy statistics）
     */
    private void updateStats(String strategyId, boolean success, long executionTime) {
        StrategyStats stats = strategyStats.get(strategyId);
        if (stats != null) {
            stats.totalExecutions++;
            if (success) {
                stats.successCount++;
            }
            stats.totalExecutionTimeMs += executionTime;
        }
    }

    /**
     * 获取可用策略列表（Get available strategies list）
     *
     * @return 策略信息列表（List of strategy info）
     */
    public List<Map<String, Object>> getAvailableStrategies() {
        return strategies.stream().map(s -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", s.getId());
            info.put("name", s.getName());
            info.put("description", s.getDescription());
            info.put("capabilities", s.getCapabilities());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 获取策略统计（Get strategy statistics）
     *
     * @return 策略统计映射（Strategy statistics map）
     */
    public Map<String, StrategyStats> getStrategyStats() {
        return new HashMap<>(strategyStats);
    }

    /**
     * 策略评分（Strategy Score）
     */
    private static class StrategyScore {
        MultiDocAnalysisStrategy strategy;
        double score;

        StrategyScore(MultiDocAnalysisStrategy strategy, double score) {
            this.strategy = strategy;
            this.score = score;
        }
    }

    /**
     * 策略统计（Strategy Statistics）
     */
    public static class StrategyStats {
        /** 总执行次数（Total executions） */
        public int totalExecutions = 0;

        /** 成功次数（Success count） */
        public int successCount = 0;

        /** 总执行时间（毫秒）（Total execution time in ms） */
        public long totalExecutionTimeMs = 0;

        /**
         * 获取成功率（Get success rate）
         *
         * @return 成功率 0-1（Success rate 0-1）
         */
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successCount / totalExecutions : 0;
        }

        /**
         * 获取平均执行时间（Get average execution time）
         *
         * @return 平均执行时间（毫秒）（Average execution time in ms）
         */
        public long getAverageExecutionTime() {
            return totalExecutions > 0 ? totalExecutionTimeMs / totalExecutions : 0;
        }
    }
}

