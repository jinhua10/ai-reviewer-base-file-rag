package top.yumbo.ai.rag.hope.monitor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HOPE 性能指标收集器
 * (HOPE Performance Metrics Collector)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Data
public class HOPEMetrics {

    // 查询统计
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong directAnswers = new AtomicLong(0);
    private final AtomicLong templateAnswers = new AtomicLong(0);
    private final AtomicLong fullRAGAnswers = new AtomicLong(0);

    // 层级命中统计
    private final AtomicLong permanentHits = new AtomicLong(0);
    private final AtomicLong ordinaryHits = new AtomicLong(0);
    private final AtomicLong highFreqHits = new AtomicLong(0);

    // 响应时间统计（毫秒）
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong directAnswerTime = new AtomicLong(0);
    private final AtomicLong templateAnswerTime = new AtomicLong(0);
    private final AtomicLong fullRAGTime = new AtomicLong(0);

    // 学习统计
    private final AtomicLong learnEvents = new AtomicLong(0);
    private final AtomicLong promotions = new AtomicLong(0);

    // 错误统计
    private final AtomicLong errors = new AtomicLong(0);

    // 按小时统计
    private final Map<String, HourlyStats> hourlyStats = new ConcurrentHashMap<>();

    // 统计起始时间
    private LocalDateTime startTime = LocalDateTime.now();

    /**
     * 记录查询
     */
    public void recordQuery(String strategyType, String hitLayer, long responseTimeMs) {
        totalQueries.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);

        // 按策略类型统计
        switch (strategyType) {
            case "DIRECT_ANSWER" -> {
                directAnswers.incrementAndGet();
                directAnswerTime.addAndGet(responseTimeMs);
            }
            case "TEMPLATE_ANSWER" -> {
                templateAnswers.incrementAndGet();
                templateAnswerTime.addAndGet(responseTimeMs);
            }
            case "FULL_RAG" -> {
                fullRAGAnswers.incrementAndGet();
                fullRAGTime.addAndGet(responseTimeMs);
            }
        }

        // 按命中层级统计
        if (hitLayer != null) {
            switch (hitLayer) {
                case "permanent" -> permanentHits.incrementAndGet();
                case "ordinary" -> ordinaryHits.incrementAndGet();
                case "high_frequency" -> highFreqHits.incrementAndGet();
            }
        }

        // 更新小时统计
        updateHourlyStats(strategyType, responseTimeMs);
    }

    /**
     * 记录学习事件
     */
    public void recordLearn() {
        learnEvents.incrementAndGet();
    }

    /**
     * 记录晋升事件
     */
    public void recordPromotion() {
        promotions.incrementAndGet();
    }

    /**
     * 记录错误
     */
    public void recordError() {
        errors.incrementAndGet();
    }

    /**
     * 更新小时统计
     */
    private void updateHourlyStats(String strategyType, long responseTimeMs) {
        String hourKey = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
        hourlyStats.computeIfAbsent(hourKey, k -> new HourlyStats()).update(strategyType, responseTimeMs);
    }

    /**
     * 获取 LLM 节省率
     */
    public double getLLMSavingsRate() {
        long total = totalQueries.get();
        if (total == 0) return 0.0;
        return (double) (directAnswers.get() + templateAnswers.get()) / total;
    }

    /**
     * 获取平均响应时间
     */
    public double getAverageResponseTime() {
        long total = totalQueries.get();
        if (total == 0) return 0.0;
        return (double) totalResponseTime.get() / total;
    }

    /**
     * 获取直接回答平均响应时间
     */
    public double getDirectAnswerAvgTime() {
        long count = directAnswers.get();
        if (count == 0) return 0.0;
        return (double) directAnswerTime.get() / count;
    }

    /**
     * 获取完整 RAG 平均响应时间
     */
    public double getFullRAGAvgTime() {
        long count = fullRAGAnswers.get();
        if (count == 0) return 0.0;
        return (double) fullRAGTime.get() / count;
    }

    /**
     * 获取摘要报告
     */
    public MetricsSummary getSummary() {
        return MetricsSummary.builder()
            .totalQueries(totalQueries.get())
            .directAnswers(directAnswers.get())
            .templateAnswers(templateAnswers.get())
            .fullRAGAnswers(fullRAGAnswers.get())
            .permanentHits(permanentHits.get())
            .ordinaryHits(ordinaryHits.get())
            .highFreqHits(highFreqHits.get())
            .llmSavingsRate(getLLMSavingsRate())
            .avgResponseTimeMs(getAverageResponseTime())
            .directAnswerAvgTimeMs(getDirectAnswerAvgTime())
            .fullRAGAvgTimeMs(getFullRAGAvgTime())
            .learnEvents(learnEvents.get())
            .promotions(promotions.get())
            .errors(errors.get())
            .uptimeMinutes(java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes())
            .build();
    }

    /**
     * 重置统计
     */
    public void reset() {
        totalQueries.set(0);
        directAnswers.set(0);
        templateAnswers.set(0);
        fullRAGAnswers.set(0);
        permanentHits.set(0);
        ordinaryHits.set(0);
        highFreqHits.set(0);
        totalResponseTime.set(0);
        directAnswerTime.set(0);
        templateAnswerTime.set(0);
        fullRAGTime.set(0);
        learnEvents.set(0);
        promotions.set(0);
        errors.set(0);
        hourlyStats.clear();
        startTime = LocalDateTime.now();
    }

    /**
     * 小时统计
     */
    @Data
    public static class HourlyStats {
        private final AtomicLong queries = new AtomicLong(0);
        private final AtomicLong directAnswers = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);

        public void update(String strategyType, long responseTimeMs) {
            queries.incrementAndGet();
            totalTime.addAndGet(responseTimeMs);
            if ("DIRECT_ANSWER".equals(strategyType)) {
                directAnswers.incrementAndGet();
            }
        }
    }

    /**
     * 摘要报告
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private long totalQueries;
        private long directAnswers;
        private long templateAnswers;
        private long fullRAGAnswers;
        private long permanentHits;
        private long ordinaryHits;
        private long highFreqHits;
        private double llmSavingsRate;
        private double avgResponseTimeMs;
        private double directAnswerAvgTimeMs;
        private double fullRAGAvgTimeMs;
        private long learnEvents;
        private long promotions;
        private long errors;
        private long uptimeMinutes;
    }
}

