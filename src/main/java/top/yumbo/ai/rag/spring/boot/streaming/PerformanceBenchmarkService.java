package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 性能基准测试服务
 * (Performance Benchmark Service)
 *
 * 提供详细的性能指标测试
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Service
public class PerformanceBenchmarkService {

    private final HOPEFastQueryService hopeFastQueryService;
    private final HybridStreamingService hybridStreamingService;

    @Autowired
    public PerformanceBenchmarkService(
            @Autowired(required = false) HOPEFastQueryService hopeFastQueryService,
            @Autowired(required = false) HybridStreamingService hybridStreamingService) {
        this.hopeFastQueryService = hopeFastQueryService;
        this.hybridStreamingService = hybridStreamingService;
    }

    /**
     * HOPE 查询性能测试
     */
    public BenchmarkResult benchmarkHOPEQuery(String question, int iterations) {
        if (hopeFastQueryService == null) {
            return BenchmarkResult.error("HOPE service not available");
        }

        log.info(I18N.get("log.benchmark.hope_query_start", iterations));
        List<Long> durations = new ArrayList<>();
        int successCount = 0;
        int foundCount = 0;

        for (int i = 0; i < iterations; i++) {
            String sessionId = "bench-hope-" + System.currentTimeMillis() + "-" + i;

            long startTime = System.nanoTime();
            try {
                HOPEAnswer answer = hopeFastQueryService.queryFast(question, sessionId);
                long duration = (System.nanoTime() - startTime) / 1_000_000;
                durations.add(duration);

                successCount++;
                if (answer != null && answer.getAnswer() != null) {
                    foundCount++;
                }

            } catch (Exception e) {
                log.warn(I18N.get("log.benchmark.iteration_failed", i, e.getMessage()));
            }

            // 避免过快
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        LongSummaryStatistics stats = durations.stream()
            .mapToLong(Long::longValue)
            .summaryStatistics();

        BenchmarkResult result = new BenchmarkResult();
        result.setTestName("HOPE Query Performance");
        result.setIterations(iterations);
        result.setSuccessCount(successCount);
        result.setFoundCount(foundCount);
        result.setMinDuration(stats.getMin());
        result.setMaxDuration(stats.getMax());
        result.setAvgDuration((long) stats.getAverage());
        result.setTargetDuration(300L); // HOPE 目标 <300ms

        log.info(I18N.get("log.benchmark.hope_query_complete"));
        log.info(I18N.get("log.benchmark.iterations", iterations));
        log.info(I18N.get("log.benchmark.success_count", successCount));
        log.info(I18N.get("log.benchmark.found_count", foundCount));
        log.info(I18N.get("log.benchmark.min_time", stats.getMin()));
        log.info(I18N.get("log.benchmark.max_time", stats.getMax()));
        log.info(I18N.get("log.benchmark.avg_time", (long) stats.getAverage()));
        log.info(I18N.get("log.benchmark.target_achievement", calculateTargetAchievement(durations, 300L)));

        return result;
    }

    /**
     * LLM 流式初始化性能测试
     */
    public BenchmarkResult benchmarkLLMInitialization(String question, int iterations) {
        if (hybridStreamingService == null) {
            return BenchmarkResult.error("Hybrid streaming service not available");
        }

        log.info(I18N.get("log.benchmark.llm_init_start", iterations));
        List<Long> durations = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < iterations; i++) {
            String userId = "bench-llm-" + System.currentTimeMillis() + "-" + i;

            long startTime = System.nanoTime();
            try {
                StreamingResponse response = hybridStreamingService.ask(question, userId);
                long duration = (System.nanoTime() - startTime) / 1_000_000;
                durations.add(duration);

                successCount++;

            } catch (Exception e) {
                log.warn(I18N.get("log.benchmark.iteration_failed", i, e.getMessage()));
            }

            // 避免过快
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        LongSummaryStatistics stats = durations.stream()
            .mapToLong(Long::longValue)
            .summaryStatistics();

        BenchmarkResult result = new BenchmarkResult();
        result.setTestName("LLM Initialization Performance");
        result.setIterations(iterations);
        result.setSuccessCount(successCount);
        result.setMinDuration(stats.getMin());
        result.setMaxDuration(stats.getMax());
        result.setAvgDuration((long) stats.getAverage());
        result.setTargetDuration(1000L); // LLM TTFB 目标 <1s

        log.info(I18N.get("log.benchmark.llm_init_complete"));
        log.info(I18N.get("log.benchmark.iterations", iterations));
        log.info(I18N.get("log.benchmark.success_count", successCount));
        log.info(I18N.get("log.benchmark.min_time", stats.getMin()));
        log.info(I18N.get("log.benchmark.max_time", stats.getMax()));
        log.info(I18N.get("log.benchmark.avg_time", (long) stats.getAverage()));
        log.info(I18N.get("log.benchmark.target_achievement", calculateTargetAchievement(durations, 1000L)));

        return result;
    }

    /**
     * 端到端性能测试
     */
    public BenchmarkResult benchmarkEndToEnd(String question, int iterations) {
        log.info(I18N.get("log.benchmark.end_to_end_start", iterations));
        List<Long> hopeDurations = new ArrayList<>();
        List<Long> llmDurations = new ArrayList<>();
        List<Long> totalDurations = new ArrayList<>();

        int successCount = 0;

        for (int i = 0; i < iterations; i++) {
            String userId = "bench-e2e-" + System.currentTimeMillis() + "-" + i;

            try {
                long totalStart = System.nanoTime();

                // HOPE 查询
                long hopeStart = System.nanoTime();
                HOPEAnswer hopeAnswer = hopeFastQueryService.queryFast(question, userId);
                long hopeDuration = (System.nanoTime() - hopeStart) / 1_000_000;
                hopeDurations.add(hopeDuration);

                // LLM 流式初始化
                long llmStart = System.nanoTime();
                StreamingResponse response = hybridStreamingService.ask(question, userId);
                long llmDuration = (System.nanoTime() - llmStart) / 1_000_000;
                llmDurations.add(llmDuration);

                long totalDuration = (System.nanoTime() - totalStart) / 1_000_000;
                totalDurations.add(totalDuration);

                successCount++;

            } catch (Exception e) {
                log.warn(I18N.get("log.benchmark.iteration_failed", i, e.getMessage()));
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        BenchmarkResult result = new BenchmarkResult();
        result.setTestName("End-to-End Performance");
        result.setIterations(iterations);
        result.setSuccessCount(successCount);

        if (!totalDurations.isEmpty()) {
            LongSummaryStatistics totalStats = totalDurations.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

            result.setMinDuration(totalStats.getMin());
            result.setMaxDuration(totalStats.getMax());
            result.setAvgDuration((long) totalStats.getAverage());
            result.setTargetDuration(1300L); // HOPE(300ms) + LLM(1000ms)

            log.info(I18N.get("log.benchmark.end_to_end_complete"));
            log.info(I18N.get("log.benchmark.iterations", iterations));
            log.info(I18N.get("log.benchmark.success_count", successCount));
            log.info(I18N.get("log.benchmark.hope_avg", hopeDurations.stream().mapToLong(Long::longValue).average().orElse(0)));
            log.info(I18N.get("log.benchmark.llm_avg", llmDurations.stream().mapToLong(Long::longValue).average().orElse(0)));
            log.info(I18N.get("log.benchmark.total_avg", (long) totalStats.getAverage()));
        }

        return result;
    }

    /**
     * 计算目标达成率
     */
    private double calculateTargetAchievement(List<Long> durations, long targetMs) {
        if (durations.isEmpty()) {
            return 0.0;
        }

        long meetTarget = durations.stream()
            .filter(d -> d <= targetMs)
            .count();

        return (double) meetTarget / durations.size() * 100.0;
    }

    /**
     * 性能基准测试结果
     */
    @Data
    public static class BenchmarkResult {
        private String testName;
        private int iterations;
        private int successCount;
        private int foundCount;
        private long minDuration;
        private long maxDuration;
        private long avgDuration;
        private long targetDuration;
        private String errorMessage;

        public static BenchmarkResult error(String message) {
            BenchmarkResult result = new BenchmarkResult();
            result.setErrorMessage(message);
            return result;
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }

        public double getSuccessRate() {
            return iterations > 0 ? (double) successCount / iterations * 100.0 : 0.0;
        }

        public boolean meetsTarget() {
            return avgDuration <= targetDuration;
        }

        public String getSummary() {
            if (!isSuccess()) {
                return "❌ " + errorMessage;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📊 ").append(testName).append("\n");
            sb.append("  - 迭代: ").append(iterations).append("\n");
            sb.append("  - 成功率: ").append(String.format("%.1f%%", getSuccessRate())).append("\n");
            sb.append("  - 平均耗时: ").append(avgDuration).append("ms");
            sb.append(meetsTarget() ? " ✅" : " ⚠️").append("\n");
            sb.append("  - 目标: <").append(targetDuration).append("ms");

            return sb.toString();
        }
    }
}

