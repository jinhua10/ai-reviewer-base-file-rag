package top.yumbo.ai.rag.spring.boot.monitoring;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务
 * (Performance Monitoring Service)
 *
 * 收集和统计系统性能指标：
 * - HOPE 查询耗时
 * - LLM 流式性能
 * - 缓存命中率
 * - 会话完成率
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Service
public class PerformanceMonitoringService {

    // HOPE 查询统计
    private final AtomicLong hopeQueryCount = new AtomicLong(0);
    private final AtomicLong hopeQueryTotalTime = new AtomicLong(0);
    private final AtomicLong hopeHitCount = new AtomicLong(0);
    private final List<Long> hopeQueryTimes = Collections.synchronizedList(new ArrayList<>());

    // LLM 流式统计
    private final AtomicLong llmStreamCount = new AtomicLong(0);
    private final AtomicLong llmStreamTotalTime = new AtomicLong(0);
    private final AtomicLong llmStreamSuccessCount = new AtomicLong(0);
    private final List<Long> llmStreamTimes = Collections.synchronizedList(new ArrayList<>());

    // 缓存统计
    private final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    // 会话统计
    private final AtomicLong sessionTotalCount = new AtomicLong(0);
    private final AtomicLong sessionCompletedCount = new AtomicLong(0);
    private final AtomicLong sessionInterruptedCount = new AtomicLong(0);
    private final AtomicLong sessionTimeoutCount = new AtomicLong(0);

    // 最近的性能快照（保留最近100条）
    private final List<PerformanceSnapshot> recentSnapshots =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * 记录 HOPE 查询
     * (Record HOPE query)
     *
     * @param durationMs 查询耗时（毫秒）
     * @param hit 是否命中
     */
    public void recordHopeQuery(long durationMs, boolean hit) {
        hopeQueryCount.incrementAndGet();
        hopeQueryTotalTime.addAndGet(durationMs);
        if (hit) {
            hopeHitCount.incrementAndGet();
        }

        synchronized (hopeQueryTimes) {
            hopeQueryTimes.add(durationMs);
            // 保留最近1000条记录
            if (hopeQueryTimes.size() > 1000) {
                hopeQueryTimes.remove(0);
            }
        }

        log.debug("HOPE 查询记录: {}ms, hit={}", durationMs, hit);
    }

    /**
     * 记录 LLM 流式生成
     * (Record LLM streaming)
     *
     * @param durationMs 生成耗时（毫秒）
     * @param success 是否成功
     */
    public void recordLlmStream(long durationMs, boolean success) {
        llmStreamCount.incrementAndGet();
        llmStreamTotalTime.addAndGet(durationMs);
        if (success) {
            llmStreamSuccessCount.incrementAndGet();
        }

        synchronized (llmStreamTimes) {
            llmStreamTimes.add(durationMs);
            if (llmStreamTimes.size() > 1000) {
                llmStreamTimes.remove(0);
            }
        }

        log.debug("LLM 流式记录: {}ms, success={}", durationMs, success);
    }

    /**
     * 记录缓存访问
     * (Record cache access)
     *
     * @param cacheName 缓存名称
     * @param hit 是否命中
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(
            cacheName, k -> new CacheStats(cacheName));

        stats.recordAccess(hit);
    }

    /**
     * 记录会话状态
     * (Record session status)
     *
     * @param status COMPLETED/INTERRUPTED/TIMEOUT
     */
    public void recordSessionStatus(String status) {
        sessionTotalCount.incrementAndGet();

        switch (status.toUpperCase()) {
            case "COMPLETED":
                sessionCompletedCount.incrementAndGet();
                break;
            case "INTERRUPTED":
                sessionInterruptedCount.incrementAndGet();
                break;
            case "TIMEOUT":
                sessionTimeoutCount.incrementAndGet();
                break;
        }

        log.debug("会话状态记录: {}", status);
    }

    /**
     * 获取 HOPE 性能指标
     * (Get HOPE performance metrics)
     */
    public HopeMetrics getHopeMetrics() {
        long count = hopeQueryCount.get();
        long totalTime = hopeQueryTotalTime.get();
        long hits = hopeHitCount.get();

        HopeMetrics metrics = new HopeMetrics();
        metrics.setQueryCount(count);
        metrics.setHitCount(hits);
        metrics.setHitRate(count > 0 ? (double) hits / count : 0.0);
        metrics.setAverageTimeMs(count > 0 ? (double) totalTime / count : 0.0);

        // 计算 P95 和 P99
        if (!hopeQueryTimes.isEmpty()) {
            List<Long> sorted = new ArrayList<>(hopeQueryTimes);
            Collections.sort(sorted);
            int size = sorted.size();
            metrics.setP95TimeMs(sorted.get((int) (size * 0.95)));
            metrics.setP99TimeMs(sorted.get((int) (size * 0.99)));
        }

        return metrics;
    }

    /**
     * 获取 LLM 性能指标
     * (Get LLM performance metrics)
     */
    public LlmMetrics getLlmMetrics() {
        long count = llmStreamCount.get();
        long totalTime = llmStreamTotalTime.get();
        long successCount = llmStreamSuccessCount.get();

        LlmMetrics metrics = new LlmMetrics();
        metrics.setStreamCount(count);
        metrics.setSuccessCount(successCount);
        metrics.setSuccessRate(count > 0 ? (double) successCount / count : 0.0);
        metrics.setAverageTimeMs(count > 0 ? (double) totalTime / count : 0.0);

        // 计算 P95 和 P99
        if (!llmStreamTimes.isEmpty()) {
            List<Long> sorted = new ArrayList<>(llmStreamTimes);
            Collections.sort(sorted);
            int size = sorted.size();
            metrics.setP95TimeMs(sorted.get((int) (size * 0.95)));
            metrics.setP99TimeMs(sorted.get((int) (size * 0.99)));
        }

        return metrics;
    }

    /**
     * 获取缓存统计
     * (Get cache statistics)
     */
    public Map<String, CacheStats> getCacheStats() {
        return new HashMap<>(cacheStatsMap);
    }

    /**
     * 获取会话统计
     * (Get session statistics)
     */
    public SessionMetrics getSessionMetrics() {
        long total = sessionTotalCount.get();
        long completed = sessionCompletedCount.get();
        long interrupted = sessionInterruptedCount.get();
        long timeout = sessionTimeoutCount.get();

        SessionMetrics metrics = new SessionMetrics();
        metrics.setTotalCount(total);
        metrics.setCompletedCount(completed);
        metrics.setInterruptedCount(interrupted);
        metrics.setTimeoutCount(timeout);
        metrics.setCompletionRate(total > 0 ? (double) completed / total : 0.0);

        return metrics;
    }

    /**
     * 获取完整的性能仪表盘
     * (Get complete performance dashboard)
     */
    public PerformanceDashboard getDashboard() {
        PerformanceDashboard dashboard = new PerformanceDashboard();
        dashboard.setTimestamp(LocalDateTime.now());
        dashboard.setHopeMetrics(getHopeMetrics());
        dashboard.setLlmMetrics(getLlmMetrics());
        dashboard.setCacheStats(getCacheStats());
        dashboard.setSessionMetrics(getSessionMetrics());
        return dashboard;
    }

    /**
     * 定期创建性能快照（每分钟）
     * (Create performance snapshot periodically)
     */
    @Scheduled(fixedRate = 60000)
    public void createSnapshot() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        snapshot.setTimestamp(LocalDateTime.now());
        snapshot.setHopeMetrics(getHopeMetrics());
        snapshot.setLlmMetrics(getLlmMetrics());
        snapshot.setSessionMetrics(getSessionMetrics());

        synchronized (recentSnapshots) {
            recentSnapshots.add(snapshot);
            // 保留最近100个快照（约100分钟）
            if (recentSnapshots.size() > 100) {
                recentSnapshots.remove(0);
            }
        }

        log.debug("性能快照已创建: HOPE查询={}, LLM流式={}, 会话完成率={}%",
            snapshot.getHopeMetrics().getQueryCount(),
            snapshot.getLlmMetrics().getStreamCount(),
            (int) (snapshot.getSessionMetrics().getCompletionRate() * 100));
    }

    /**
     * 获取最近的性能快照
     * (Get recent performance snapshots)
     */
    public List<PerformanceSnapshot> getRecentSnapshots(int limit) {
        synchronized (recentSnapshots) {
            int size = recentSnapshots.size();
            int fromIndex = Math.max(0, size - limit);
            return new ArrayList<>(recentSnapshots.subList(fromIndex, size));
        }
    }

    /**
     * 重置统计数据
     * (Reset statistics)
     */
    public void reset() {
        hopeQueryCount.set(0);
        hopeQueryTotalTime.set(0);
        hopeHitCount.set(0);
        hopeQueryTimes.clear();

        llmStreamCount.set(0);
        llmStreamTotalTime.set(0);
        llmStreamSuccessCount.set(0);
        llmStreamTimes.clear();

        cacheStatsMap.clear();

        sessionTotalCount.set(0);
        sessionCompletedCount.set(0);
        sessionInterruptedCount.set(0);
        sessionTimeoutCount.set(0);

        recentSnapshots.clear();

        log.info("性能统计已重置");
    }

    // ==================== 内部数据类 ====================

    @Data
    public static class HopeMetrics {
        private long queryCount;
        private long hitCount;
        private double hitRate;
        private double averageTimeMs;
        private long p95TimeMs;
        private long p99TimeMs;
    }

    @Data
    public static class LlmMetrics {
        private long streamCount;
        private long successCount;
        private double successRate;
        private double averageTimeMs;
        private long p95TimeMs;
        private long p99TimeMs;
    }

    @Data
    public static class SessionMetrics {
        private long totalCount;
        private long completedCount;
        private long interruptedCount;
        private long timeoutCount;
        private double completionRate;
    }

    @Data
    public static class CacheStats {
        private String cacheName;
        private AtomicLong hitCount = new AtomicLong(0);
        private AtomicLong missCount = new AtomicLong(0);

        public CacheStats(String cacheName) {
            this.cacheName = cacheName;
        }

        public void recordAccess(boolean hit) {
            if (hit) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }

        public long getTotalCount() {
            return hitCount.get() + missCount.get();
        }

        public double getHitRate() {
            long total = getTotalCount();
            return total > 0 ? (double) hitCount.get() / total : 0.0;
        }
    }

    @Data
    public static class PerformanceDashboard {
        private LocalDateTime timestamp;
        private HopeMetrics hopeMetrics;
        private LlmMetrics llmMetrics;
        private Map<String, CacheStats> cacheStats;
        private SessionMetrics sessionMetrics;
    }

    @Data
    public static class PerformanceSnapshot {
        private LocalDateTime timestamp;
        private HopeMetrics hopeMetrics;
        private LlmMetrics llmMetrics;
        private SessionMetrics sessionMetrics;
    }
}

