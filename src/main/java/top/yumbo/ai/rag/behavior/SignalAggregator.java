package top.yumbo.ai.rag.behavior;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 信号聚合器 (Signal Aggregator)
 * 按不同维度聚合行为信号，生成统计报告
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class SignalAggregator {

    private static final Logger logger = LoggerFactory.getLogger(SignalAggregator.class);

    /**
     * 态度推断引擎 (Attitude Inference Engine)
     */
    private final AttitudeInferenceEngine inferenceEngine;

    // ========== 构造函数 (Constructors) ==========

    public SignalAggregator() {
        this.inferenceEngine = new AttitudeInferenceEngine();
    }

    public SignalAggregator(AttitudeInferenceEngine inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
    }

    // ========== 按用户聚合 (Aggregate by User) ==========

    /**
     * 按用户聚合信号 (Aggregate Signals by User)
     *
     * @param signals 所有信号列表
     * @return Key: userId, Value: 用户聚合结果
     */
    public Map<String, UserAggregation> aggregateByUser(List<BehaviorSignalEvent> signals) {
        logger.info(I18N.get("behavior.aggregate.user.start"), signals.size());

        // 按用户分组 (Group by user)
        Map<String, List<BehaviorSignalEvent>> signalsByUser = signals.stream()
                .collect(Collectors.groupingBy(BehaviorSignalEvent::getUserId));

        Map<String, UserAggregation> results = new HashMap<>();

        signalsByUser.forEach((userId, userSignals) -> {
            UserAggregation aggregation = new UserAggregation(userId);

            // 计算平均态度 (Calculate average attitude)
            Map<String, Object> userContext = new HashMap<>();
            AttitudeScore avgScore = inferenceEngine.inferAttitude(userSignals, userContext);
            aggregation.setAverageAttitude(avgScore);

            // 统计信号类型分布 (Count signal type distribution)
            Map<SignalType, Long> typeDistribution = userSignals.stream()
                    .collect(Collectors.groupingBy(
                            BehaviorSignalEvent::getSignalType,
                            Collectors.counting()
                    ));
            aggregation.setSignalTypeDistribution(typeDistribution);

            // 识别用户偏好 (Identify user preferences)
            identifyUserPreferences(aggregation, userSignals);

            // 追踪态度变化 (Track attitude changes)
            trackAttitudeChanges(aggregation, userSignals);

            results.put(userId, aggregation);
        });

        logger.info(I18N.get("behavior.aggregate.user.complete"), results.size());
        return results;
    }

    /**
     * 识别用户偏好 (Identify User Preferences)
     */
    private void identifyUserPreferences(UserAggregation aggregation, List<BehaviorSignalEvent> signals) {
        // 统计正面和负面信号 (Count positive and negative signals)
        long positiveCount = signals.stream()
                .filter(s -> s.getSignalType().isPositive())
                .count();
        long negativeCount = signals.stream()
                .filter(s -> s.getSignalType().isNegative())
                .count();

        aggregation.setPositiveSignalCount(positiveCount);
        aggregation.setNegativeSignalCount(negativeCount);

        // 判断用户倾向 (Determine user tendency)
        if (positiveCount > negativeCount * 2) {
            aggregation.setTendency("positive");
        } else if (negativeCount > positiveCount * 2) {
            aggregation.setTendency("negative");
        } else {
            aggregation.setTendency("neutral");
        }
    }

    /**
     * 追踪态度变化 (Track Attitude Changes)
     */
    private void trackAttitudeChanges(UserAggregation aggregation, List<BehaviorSignalEvent> signals) {
        if (signals.size() < 2) {
            aggregation.setAttitudeTrend("stable");
            return;
        }

        // 按时间排序 (Sort by time)
        List<BehaviorSignalEvent> sorted = signals.stream()
                .sorted(Comparator.comparing(BehaviorSignalEvent::getTimestamp))
                .collect(Collectors.toList());

        // 计算早期和晚期的平均态度 (Calculate early and late average attitude)
        int midpoint = sorted.size() / 2;
        List<BehaviorSignalEvent> earlySignals = sorted.subList(0, midpoint);
        List<BehaviorSignalEvent> lateSignals = sorted.subList(midpoint, sorted.size());

        double earlyScore = calculateAverageScore(earlySignals);
        double lateScore = calculateAverageScore(lateSignals);

        // 判断趋势 (Determine trend)
        double change = lateScore - earlyScore;
        if (Math.abs(change) < 0.1) {
            aggregation.setAttitudeTrend("stable");
        } else if (change > 0) {
            aggregation.setAttitudeTrend("improving");
        } else {
            aggregation.setAttitudeTrend("declining");
        }
    }

    // ========== 按概念聚合 (Aggregate by Concept) ==========

    /**
     * 按概念（答案）聚合信号 (Aggregate Signals by Concept/Answer)
     *
     * @param signals 所有信号列表
     * @return Key: answerId, Value: 概念聚合结果
     */
    public Map<String, ConceptAggregation> aggregateByConcept(List<BehaviorSignalEvent> signals) {
        logger.info(I18N.get("behavior.aggregate.concept.start"), signals.size());

        // 按答案分组 (Group by answer)
        Map<String, List<BehaviorSignalEvent>> signalsByAnswer = signals.stream()
                .collect(Collectors.groupingBy(BehaviorSignalEvent::getAnswerId));

        Map<String, ConceptAggregation> results = new HashMap<>();

        signalsByAnswer.forEach((answerId, answerSignals) -> {
            ConceptAggregation aggregation = new ConceptAggregation(answerId);

            // 计算平均评分 (Calculate average score)
            double avgScore = calculateAverageScore(answerSignals);
            aggregation.setAverageScore(avgScore);

            // 统计正负反馈比 (Calculate positive/negative ratio)
            long positiveCount = answerSignals.stream()
                    .filter(s -> s.getSignalType().isPositive())
                    .count();
            long negativeCount = answerSignals.stream()
                    .filter(s -> s.getSignalType().isNegative())
                    .count();

            aggregation.setPositiveFeedbackCount(positiveCount);
            aggregation.setNegativeFeedbackCount(negativeCount);
            aggregation.setFeedbackRatio(calculateRatio(positiveCount, negativeCount));

            // 识别问题概念 (Identify problematic concepts)
            boolean isProblematic = identifyProblematicConcept(answerSignals, avgScore);
            aggregation.setProblematic(isProblematic);

            // 计算信心指数 (Calculate confidence index)
            double confidence = inferenceEngine.calculateConfidence(answerSignals, avgScore);
            aggregation.setConfidenceIndex(confidence);

            results.put(answerId, aggregation);
        });

        logger.info(I18N.get("behavior.aggregate.concept.complete"), results.size());
        return results;
    }

    /**
     * 识别问题概念 (Identify Problematic Concept)
     * 负面信号过多或平均评分过低
     */
    private boolean identifyProblematicConcept(List<BehaviorSignalEvent> signals, double avgScore) {
        // 1. 平均评分过低 (Low average score)
        if (avgScore < -0.3) {
            return true;
        }

        // 2. 负面信号比例过高 (High negative signal ratio)
        long negativeCount = signals.stream()
                .filter(s -> s.getSignalType().isNegative())
                .count();
        double negativeRatio = (double) negativeCount / signals.size();
        if (negativeRatio > 0.6) {
            return true;
        }

        // 3. 强负面信号存在 (Strong negative signals present)
        boolean hasStrongNegative = signals.stream()
                .anyMatch(s -> s.getSignalType().isStrong() && s.getSignalType().isNegative());
        if (hasStrongNegative) {
            return true;
        }

        return false;
    }

    // ========== 按角色聚合 (Aggregate by Role) ==========

    /**
     * 按角色聚合信号 (Aggregate Signals by Role)
     *
     * @param signals 所有信号列表
     * @param userRoles Key: userId, Value: 用户角色
     * @return Key: role, Value: 角色聚合结果
     */
    public Map<String, RoleAggregation> aggregateByRole(List<BehaviorSignalEvent> signals,
                                                         Map<String, String> userRoles) {
        logger.info(I18N.get("behavior.aggregate.role.start"), signals.size());

        // 按角色分组信号 (Group signals by role)
        Map<String, List<BehaviorSignalEvent>> signalsByRole = new HashMap<>();

        for (BehaviorSignalEvent signal : signals) {
            String userId = signal.getUserId();
            String role = userRoles.getOrDefault(userId, "regular");
            signalsByRole.computeIfAbsent(role, k -> new ArrayList<>()).add(signal);
        }

        Map<String, RoleAggregation> results = new HashMap<>();

        signalsByRole.forEach((role, roleSignals) -> {
            RoleAggregation aggregation = new RoleAggregation(role);

            // 计算角色的态度分布 (Calculate attitude distribution for role)
            Map<AttitudeLevel, Long> attitudeDistribution = calculateAttitudeDistribution(roleSignals);
            aggregation.setAttitudeDistribution(attitudeDistribution);

            // 计算平均评分 (Calculate average score)
            double avgScore = calculateAverageScore(roleSignals);
            aggregation.setAverageScore(avgScore);

            // 分析角色差异 (Analyze role differences)
            analyzeRoleDifferences(aggregation, roleSignals);

            results.put(role, aggregation);
        });

        logger.info(I18N.get("behavior.aggregate.role.complete"), results.size());
        return results;
    }

    /**
     * 计算态度分布 (Calculate Attitude Distribution)
     */
    private Map<AttitudeLevel, Long> calculateAttitudeDistribution(List<BehaviorSignalEvent> signals) {
        Map<AttitudeLevel, Long> distribution = new EnumMap<>(AttitudeLevel.class);

        // 按答案分组推断态度 (Infer attitude for each answer)
        Map<String, List<BehaviorSignalEvent>> signalsByAnswer = signals.stream()
                .collect(Collectors.groupingBy(BehaviorSignalEvent::getAnswerId));

        signalsByAnswer.values().forEach(answerSignals -> {
            AttitudeScore score = inferenceEngine.inferAttitude(answerSignals, new HashMap<>());
            AttitudeLevel level = score.getLevel();
            distribution.merge(level, 1L, Long::sum);
        });

        return distribution;
    }

    /**
     * 分析角色差异 (Analyze Role Differences)
     */
    private void analyzeRoleDifferences(RoleAggregation aggregation, List<BehaviorSignalEvent> signals) {
        // 计算信号多样性 (Calculate signal diversity)
        Set<SignalType> uniqueSignalTypes = signals.stream()
                .map(BehaviorSignalEvent::getSignalType)
                .collect(Collectors.toSet());
        aggregation.setSignalDiversity(uniqueSignalTypes.size());

        // 计算活跃度 (Calculate activity level)
        aggregation.setTotalSignalCount(signals.size());
        aggregation.setAverageSignalsPerUser(calculateAverageSignalsPerUser(signals));
    }

    // ========== 生成报告 (Generate Report) ==========

    /**
     * 生成综合报告 (Generate Comprehensive Report)
     *
     * @param signals 所有信号列表
     * @return 报告对象
     */
    public AggregationReport generateReport(List<BehaviorSignalEvent> signals) {
        logger.info(I18N.get("behavior.aggregate.report.start"), signals.size());

        AggregationReport report = new AggregationReport();

        // 1. 整体满意度 (Overall satisfaction)
        double overallSatisfaction = calculateAverageScore(signals);
        report.setOverallSatisfaction(overallSatisfaction);

        // 2. 关键发现 (Key findings)
        List<String> keyFindings = identifyKeyFindings(signals);
        report.setKeyFindings(keyFindings);

        // 3. 改进建议 (Improvement suggestions)
        List<String> suggestions = generateSuggestions(signals, overallSatisfaction);
        report.setSuggestions(suggestions);

        // 4. 统计摘要 (Statistical summary)
        report.setTotalSignalCount(signals.size());
        report.setPositiveSignalRatio(calculatePositiveRatio(signals));
        report.setNegativeSignalRatio(calculateNegativeRatio(signals));

        logger.info(I18N.get("behavior.aggregate.report.complete"));
        return report;
    }

    /**
     * 识别关键发现 (Identify Key Findings)
     */
    private List<String> identifyKeyFindings(List<BehaviorSignalEvent> signals) {
        List<String> findings = new ArrayList<>();

        // 1. 最常见的信号类型 (Most common signal types)
        Map<SignalType, Long> typeCounts = signals.stream()
                .collect(Collectors.groupingBy(
                        BehaviorSignalEvent::getSignalType,
                        Collectors.counting()
                ));

        SignalType mostCommon = Collections.max(typeCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
        findings.add(I18N.get("behavior.aggregate.finding.most_common", mostCommon));

        // 2. 正负比例 (Positive/negative ratio)
        double positiveRatio = calculatePositiveRatio(signals);
        if (positiveRatio > 0.7) {
            findings.add(I18N.get("behavior.aggregate.finding.highly_positive"));
        } else if (positiveRatio < 0.3) {
            findings.add(I18N.get("behavior.aggregate.finding.highly_negative"));
        }

        return findings;
    }

    /**
     * 生成改进建议 (Generate Improvement Suggestions)
     */
    private List<String> generateSuggestions(List<BehaviorSignalEvent> signals, double overallSatisfaction) {
        List<String> suggestions = new ArrayList<>();

        if (overallSatisfaction < 0.0) {
            suggestions.add(I18N.get("behavior.aggregate.suggestion.low_satisfaction"));
        }

        // 检查是否有大量快速关闭信号 (Check for many close signals)
        long closeCount = signals.stream()
                .filter(s -> s.getSignalType() == SignalType.CLOSE_IMMEDIATELY)
                .count();
        if (closeCount > signals.size() * 0.2) {
            suggestions.add(I18N.get("behavior.aggregate.suggestion.too_many_closes"));
        }

        return suggestions;
    }

    // ========== 辅助方法 (Helper Methods) ==========

    /**
     * 计算平均评分 (Calculate Average Score)
     */
    private double calculateAverageScore(List<BehaviorSignalEvent> signals) {
        if (signals.isEmpty()) {
            return 0.0;
        }

        double sum = signals.stream()
                .mapToDouble(BehaviorSignalEvent::getWeightedValue)
                .sum();

        return sum / signals.size();
    }

    /**
     * 计算正面信号比例 (Calculate Positive Ratio)
     */
    private double calculatePositiveRatio(List<BehaviorSignalEvent> signals) {
        if (signals.isEmpty()) {
            return 0.0;
        }

        long positiveCount = signals.stream()
                .filter(s -> s.getSignalType().isPositive())
                .count();

        return (double) positiveCount / signals.size();
    }

    /**
     * 计算负面信号比例 (Calculate Negative Ratio)
     */
    private double calculateNegativeRatio(List<BehaviorSignalEvent> signals) {
        if (signals.isEmpty()) {
            return 0.0;
        }

        long negativeCount = signals.stream()
                .filter(s -> s.getSignalType().isNegative())
                .count();

        return (double) negativeCount / signals.size();
    }

    /**
     * 计算比率 (Calculate Ratio)
     */
    private double calculateRatio(long positive, long negative) {
        if (negative == 0) {
            return positive > 0 ? Double.POSITIVE_INFINITY : 0.0;
        }
        return (double) positive / negative;
    }

    /**
     * 计算平均每用户信号数 (Calculate Average Signals Per User)
     */
    private double calculateAverageSignalsPerUser(List<BehaviorSignalEvent> signals) {
        Set<String> uniqueUsers = signals.stream()
                .map(BehaviorSignalEvent::getUserId)
                .collect(Collectors.toSet());

        return uniqueUsers.isEmpty() ? 0.0 : (double) signals.size() / uniqueUsers.size();
    }

    // ========== 内部类：聚合结果 (Inner Classes: Aggregation Results) ==========

    @Data
    public static class UserAggregation {
        // Getters and Setters (省略 for brevity)
        private String userId;
        private AttitudeScore averageAttitude;
        private Map<SignalType, Long> signalTypeDistribution;
        private long positiveSignalCount;
        private long negativeSignalCount;
        private String tendency;
        private String attitudeTrend;

        public UserAggregation(String userId) {
            this.userId = userId;
        }

    }

    @Data
    public static class ConceptAggregation {
        private String answerId;
        private double averageScore;
        private long positiveFeedbackCount;
        private long negativeFeedbackCount;
        private double feedbackRatio;
        private boolean problematic;
        private double confidenceIndex;

        public ConceptAggregation(String answerId) {
            this.answerId = answerId;
        }


    }

    @Data
    public static class RoleAggregation {
        private String role;
        private Map<AttitudeLevel, Long> attitudeDistribution;
        private double averageScore;
        private int signalDiversity;
        private int totalSignalCount;
        private double averageSignalsPerUser;

        public RoleAggregation(String role) {
            this.role = role;
        }

    }

    @Data
    public static class AggregationReport {
        private double overallSatisfaction;
        private List<String> keyFindings;
        private List<String> suggestions;
        private int totalSignalCount;
        private double positiveSignalRatio;
        private double negativeSignalRatio;

    }
}

