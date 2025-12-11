package top.yumbo.ai.rag.loader;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 加载统计信息 (Loading Statistics)
 *
 * 记录索引加载的统计数据，用于监控和优化
 * (Records index loading statistics for monitoring and optimization)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
public class LoadingStats {

    /**
     * 总加载次数 (Total load count)
     */
    private final AtomicLong totalLoadCount = new AtomicLong(0);

    /**
     * 成功加载次数 (Successful load count)
     */
    private final AtomicLong successLoadCount = new AtomicLong(0);

    /**
     * 失败加载次数 (Failed load count)
     */
    private final AtomicLong failedLoadCount = new AtomicLong(0);

    /**
     * 缓存命中次数 (Cache hit count)
     */
    private final AtomicLong cacheHitCount = new AtomicLong(0);

    /**
     * 总加载时间（毫秒） (Total load time in milliseconds)
     */
    private final AtomicLong totalLoadTimeMs = new AtomicLong(0);

    /**
     * 每个角色的加载记录 (Load records per role)
     */
    private final Map<String, RoleLoadRecord> roleRecords = new ConcurrentHashMap<>();

    /**
     * 统计开始时间 (Statistics start time)
     */
    private final Instant startTime = Instant.now();

    /**
     * 记录加载成功 (Record successful load)
     *
     * @param roleId 角色ID (Role ID)
     * @param loadTimeMs 加载时间（毫秒） (Load time in milliseconds)
     */
    public void recordLoad(String roleId, long loadTimeMs) {
        totalLoadCount.incrementAndGet();
        successLoadCount.incrementAndGet();
        totalLoadTimeMs.addAndGet(loadTimeMs);

        roleRecords.computeIfAbsent(roleId, k -> new RoleLoadRecord(roleId))
                .recordLoad(loadTimeMs);

        log.debug(I18N.get("loader.stats.load_recorded", roleId, loadTimeMs));
    }

    /**
     * 记录加载失败 (Record failed load)
     *
     * @param roleId 角色ID (Role ID)
     * @param error 错误信息 (Error message)
     */
    public void recordLoadFailure(String roleId, String error) {
        totalLoadCount.incrementAndGet();
        failedLoadCount.incrementAndGet();

        roleRecords.computeIfAbsent(roleId, k -> new RoleLoadRecord(roleId))
                .recordFailure();

        log.warn(I18N.get("loader.stats.load_failed", roleId, error));
    }

    /**
     * 记录缓存命中 (Record cache hit)
     *
     * @param roleId 角色ID (Role ID)
     */
    public void recordCacheHit(String roleId) {
        cacheHitCount.incrementAndGet();

        roleRecords.computeIfAbsent(roleId, k -> new RoleLoadRecord(roleId))
                .recordCacheHit();

        log.debug(I18N.get("loader.stats.cache_hit", roleId, getCacheHitRate()));
    }

    /**
     * 获取平均加载时间 (Get average load time)
     *
     * @return 平均加载时间（毫秒） (Average load time in milliseconds)
     */
    public double getAverageLoadTimeMs() {
        long count = successLoadCount.get();
        return count == 0 ? 0.0 : (double) totalLoadTimeMs.get() / count;
    }

    /**
     * 获取缓存命中率 (Get cache hit rate)
     *
     * @return 命中率 (Hit rate)
     */
    public double getCacheHitRate() {
        long total = totalLoadCount.get();
        return total == 0 ? 0.0 : (double) cacheHitCount.get() / total;
    }

    /**
     * 获取成功率 (Get success rate)
     *
     * @return 成功率 (Success rate)
     */
    public double getSuccessRate() {
        long total = totalLoadCount.get();
        return total == 0 ? 0.0 : (double) successLoadCount.get() / total;
    }

    /**
     * 获取角色加载记录 (Get role load record)
     *
     * @param roleId 角色ID (Role ID)
     * @return 加载记录 (Load record)
     */
    public RoleLoadRecord getRoleRecord(String roleId) {
        return roleRecords.get(roleId);
    }

    /**
     * 获取所有角色记录 (Get all role records)
     *
     * @return 所有记录 (All records)
     */
    public Map<String, RoleLoadRecord> getAllRoleRecords() {
        return new ConcurrentHashMap<>(roleRecords);
    }

    /**
     * 重置统计信息 (Reset statistics)
     */
    public void reset() {
        totalLoadCount.set(0);
        successLoadCount.set(0);
        failedLoadCount.set(0);
        cacheHitCount.set(0);
        totalLoadTimeMs.set(0);
        roleRecords.clear();

        log.info(I18N.get("loader.stats.reset"));
    }

    /**
     * 生成统计报告 (Generate statistics report)
     *
     * @return 统计报告 (Statistics report)
     */
    public StatsReport generateReport() {
        return StatsReport.builder()
                .startTime(startTime)
                .totalLoadCount(totalLoadCount.get())
                .successLoadCount(successLoadCount.get())
                .failedLoadCount(failedLoadCount.get())
                .cacheHitCount(cacheHitCount.get())
                .averageLoadTimeMs(getAverageLoadTimeMs())
                .cacheHitRate(getCacheHitRate())
                .successRate(getSuccessRate())
                .roleRecordCount(roleRecords.size())
                .build();
    }

    /**
     * 角色加载记录 (Role Load Record)
     */
    @Data
    public static class RoleLoadRecord {
        /**
         * 角色ID (Role ID)
         */
        private final String roleId;

        /**
         * 加载次数 (Load count)
         */
        private final AtomicLong loadCount = new AtomicLong(0);

        /**
         * 失败次数 (Failure count)
         */
        private final AtomicLong failureCount = new AtomicLong(0);

        /**
         * 缓存命中次数 (Cache hit count)
         */
        private final AtomicLong cacheHitCount = new AtomicLong(0);

        /**
         * 总加载时间 (Total load time)
         */
        private final AtomicLong totalLoadTimeMs = new AtomicLong(0);

        /**
         * 最后加载时间 (Last load time)
         */
        private volatile Instant lastLoadTime;

        /**
         * 记录加载 (Record load)
         *
         * @param loadTimeMs 加载时间 (Load time)
         */
        public void recordLoad(long loadTimeMs) {
            loadCount.incrementAndGet();
            totalLoadTimeMs.addAndGet(loadTimeMs);
            lastLoadTime = Instant.now();
        }

        /**
         * 记录失败 (Record failure)
         */
        public void recordFailure() {
            failureCount.incrementAndGet();
        }

        /**
         * 记录缓存命中 (Record cache hit)
         */
        public void recordCacheHit() {
            cacheHitCount.incrementAndGet();
        }

        /**
         * 获取平均加载时间 (Get average load time)
         *
         * @return 平均时间 (Average time)
         */
        public double getAverageLoadTimeMs() {
            long count = loadCount.get();
            return count == 0 ? 0.0 : (double) totalLoadTimeMs.get() / count;
        }
    }

    /**
     * 统计报告 (Statistics Report)
     */
    @Data
    @lombok.Builder
    public static class StatsReport {
        /**
         * 统计开始时间 (Statistics start time)
         */
        private Instant startTime;

        /**
         * 总加载次数 (Total load count)
         */
        private long totalLoadCount;

        /**
         * 成功次数 (Success count)
         */
        private long successLoadCount;

        /**
         * 失败次数 (Failed count)
         */
        private long failedLoadCount;

        /**
         * 缓存命中次数 (Cache hit count)
         */
        private long cacheHitCount;

        /**
         * 平均加载时间 (Average load time)
         */
        private double averageLoadTimeMs;

        /**
         * 缓存命中率 (Cache hit rate)
         */
        private double cacheHitRate;

        /**
         * 成功率 (Success rate)
         */
        private double successRate;

        /**
         * 角色记录数 (Role record count)
         */
        private int roleRecordCount;
    }
}

