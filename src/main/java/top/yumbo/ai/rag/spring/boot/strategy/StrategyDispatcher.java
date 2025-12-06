package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能策略调度器
 * (Intelligent Strategy Dispatcher)
 *
 * 负责：
 * 1. 注册和管理所有策略
 * 2. 根据上下文智能选择最佳策略
 * 3. 组合多个策略执行
 * 4. 监控策略执行效果
 */
@Service
@Slf4j
public class StrategyDispatcher {

    @Autowired
    private List<MultiDocAnalysisStrategy> strategies;

    private final Map<String, MultiDocAnalysisStrategy> strategyMap = new ConcurrentHashMap<>();

    // 策略使用统计
    private final Map<String, StrategyStats> strategyStats = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("📦 Initializing Strategy Dispatcher...");

        for (MultiDocAnalysisStrategy strategy : strategies) {
            strategyMap.put(strategy.getId(), strategy);
            strategyStats.put(strategy.getId(), new StrategyStats());
            log.info("  ✅ Registered strategy: {} - {}", strategy.getId(), strategy.getName());
        }

        log.info("✅ Strategy Dispatcher initialized with {} strategies", strategyMap.size());
    }

    /**
     * 执行智能分析
     * (Execute smart analysis)
     *
     * 根据上下文自动选择最佳策略组合
     */
    public AnalysisResult analyze(AnalysisContext context, ProgressCallback callback) {
        long startTime = System.currentTimeMillis();

        // 选择策略
        List<MultiDocAnalysisStrategy> selectedStrategies = selectStrategies(context);

        if (selectedStrategies.isEmpty()) {
            log.warn("No suitable strategy found for context");
            return AnalysisResult.failure("没有找到合适的分析策略");
        }

        log.info("🎯 Selected strategies: {}",
                selectedStrategies.stream().map(MultiDocAnalysisStrategy::getId).toList());

        // 执行策略
        AnalysisResult result;
        if (selectedStrategies.size() == 1) {
            result = executeSingle(selectedStrategies.get(0), context, callback);
        } else {
            result = executeCombined(selectedStrategies, context, callback);
        }

        // 更新统计
        long executionTime = System.currentTimeMillis() - startTime;
        for (MultiDocAnalysisStrategy strategy : selectedStrategies) {
            updateStats(strategy.getId(), result.isSuccess(), executionTime);
        }

        return result;
    }

    /**
     * 执行指定策略
     * (Execute specified strategies)
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
            log.warn("No valid strategies found in: {}", strategyIds);
            return AnalysisResult.failure("未找到有效的策略: " + strategyIds);
        }

        if (selectedStrategies.size() == 1) {
            return executeSingle(selectedStrategies.get(0), context, callback);
        } else {
            return executeCombined(selectedStrategies, context, callback);
        }
    }

    /**
     * 选择最佳策略
     * (Select best strategies)
     */
    private List<MultiDocAnalysisStrategy> selectStrategies(AnalysisContext context) {
        // 如果指定了策略，直接使用
        if (context.getStrategies() != null && !context.getStrategies().isEmpty()) {
            return context.getStrategies().stream()
                    .map(strategyMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 评估所有策略的适用性
        List<StrategyScore> scores = new ArrayList<>();

        for (MultiDocAnalysisStrategy strategy : strategies) {
            try {
                int suitability = strategy.evaluateSuitability(context);
                if (suitability > 0) {
                    // 结合历史统计调整分数
                    double adjustedScore = adjustScoreWithStats(strategy.getId(), suitability);
                    scores.add(new StrategyScore(strategy, adjustedScore));
                }
            } catch (Exception e) {
                log.warn("Error evaluating strategy {}: {}", strategy.getId(), e.getMessage());
            }
        }

        // 按分数排序
        scores.sort((a, b) -> Double.compare(b.score, a.score));

        // 选择最佳策略（可能多个）
        if (scores.isEmpty()) {
            return List.of();
        }

        // 如果最高分策略明显优于其他，单独使用
        if (scores.size() == 1 || scores.get(0).score > scores.get(1).score * 1.5) {
            return List.of(scores.get(0).strategy);
        }

        // 否则组合使用得分相近的策略
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
     * 执行单个策略
     */
    private AnalysisResult executeSingle(
            MultiDocAnalysisStrategy strategy,
            AnalysisContext context,
            ProgressCallback callback) {

        log.info("Executing single strategy: {}", strategy.getId());
        return strategy.analyze(context, callback);
    }

    /**
     * 组合执行多个策略
     */
    private AnalysisResult executeCombined(
            List<MultiDocAnalysisStrategy> strategies,
            AnalysisContext context,
            ProgressCallback callback) {

        log.info("Executing combined strategies: {}",
                strategies.stream().map(MultiDocAnalysisStrategy::getId).toList());

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

        // 合并结果
        callback.onProgress(90, "合并分析结果...");
        return mergeResults(results, strategies);
    }

    /**
     * 合并多个策略的结果
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
                combinedAnswer.append("## ").append(strategy.getName()).append(" 分析结果\n\n");
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

        // 去重关键点
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
     * 根据历史统计调整分数
     */
    private double adjustScoreWithStats(String strategyId, int baseSuitability) {
        StrategyStats stats = strategyStats.get(strategyId);
        if (stats == null || stats.totalExecutions < 5) {
            return baseSuitability;
        }

        // 成功率调整
        double successRate = (double) stats.successCount / stats.totalExecutions;
        double adjustment = successRate * 10 - 5; // -5 到 +5 的调整

        return baseSuitability + adjustment;
    }

    /**
     * 更新策略统计
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
     * 获取可用策略列表
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
     * 获取策略统计
     */
    public Map<String, StrategyStats> getStrategyStats() {
        return new HashMap<>(strategyStats);
    }

    // 内部类
    private static class StrategyScore {
        MultiDocAnalysisStrategy strategy;
        double score;

        StrategyScore(MultiDocAnalysisStrategy strategy, double score) {
            this.strategy = strategy;
            this.score = score;
        }
    }

    public static class StrategyStats {
        public int totalExecutions = 0;
        public int successCount = 0;
        public long totalExecutionTimeMs = 0;

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successCount / totalExecutions : 0;
        }

        public long getAverageExecutionTime() {
            return totalExecutions > 0 ? totalExecutionTimeMs / totalExecutions : 0;
        }
    }
}

