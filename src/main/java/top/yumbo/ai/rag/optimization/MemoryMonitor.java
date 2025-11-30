package top.yumbo.ai.rag.optimization;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * 内存监控工具
 * 用于监控和记录系统内存使用情况
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class MemoryMonitor {

    private final Runtime runtime;
    private static final long MB = 1024 * 1024;

    public MemoryMonitor() {
        this(Runtime.getRuntime());
    }

    public MemoryMonitor(Runtime runtime) {
        this.runtime = runtime;
    }

    /**
     * 记录当前内存使用情况
     *
     * @param phase 当前处理阶段描述
     */
    public void logMemoryUsage(String phase) {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        long usedMB = usedMemory / MB;
        long totalMB = totalMemory / MB;
        long maxMB = maxMemory / MB;
        long freeMB = freeMemory / MB;

        double usagePercent = (double) usedMemory / maxMemory * 100;

        String message;
        message = LogMessageProvider.getMessage("log.memory.usage", phase, usedMB, maxMB, String.format("%.1f", usagePercent));
        log.info(message);

        // 内存使用超过80%时发出警告
        if (usagePercent > 80) {
            log.warn(LogMessageProvider.getMessage("log.memory.warning", phase, String.format("%.1f", usagePercent)));
        }

        // 内存使用超过90%时发出严重警告
        if (usagePercent > 90) {
            log.error(LogMessageProvider.getMessage("log.memory.critical", phase, String.format("%.1f", usagePercent)));
        }
    }

    /**
     * 获取当前已使用内存（MB）
     */
    public long getUsedMemoryMB() {
        return (runtime.totalMemory() - runtime.freeMemory()) / MB;
    }

    /**
     * 获取最大可用内存（MB）
     */
    public long getMaxMemoryMB() {
        return runtime.maxMemory() / MB;
    }

    /**
     * 获取内存使用百分比
     */
    public double getMemoryUsagePercent() {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        return (double) usedMemory / maxMemory * 100;
    }

    /**
     * 检查是否需要触发GC
     *
     * @param threshold 内存使用阈值（百分比，0-100）
     * @return 如果内存使用超过阈值返回true
     */
    public boolean shouldTriggerGC(double threshold) {
        return getMemoryUsagePercent() > threshold;
    }

    /**
     * 建议执行垃圾回收
     * 注意：这只是建议，实际执行由JVM决定
     */
    public void suggestGC() {
        long beforeUsed = getUsedMemoryMB();
        log.info(LogMessageProvider.getMessage("log.optimization.memory.suggest_gc", beforeUsed));

        System.gc();

        // 等待一小段时间让GC完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long afterUsed = getUsedMemoryMB();
        long freed = beforeUsed - afterUsed;

        if (freed > 0) {
            log.info(LogMessageProvider.getMessage("log.optimization.memory.gc_freed", freed));
        } else {
            log.info(LogMessageProvider.getMessage("log.optimization.memory.gc_no_freed"));
        }
    }

    /**
     * 获取内存使用摘要
     */
    public MemoryStats getMemoryStats() {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        return MemoryStats.builder()
            .usedMB(usedMemory / MB)
            .freeMB(freeMemory / MB)
            .totalMB(totalMemory / MB)
            .maxMB(maxMemory / MB)
            .usagePercent((double) usedMemory / maxMemory * 100)
            .build();
    }

    /**
     * 内存统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class MemoryStats {
        private long usedMB;
        private long freeMB;
        private long totalMB;
        private long maxMB;
        private double usagePercent;

        @Override
        public String toString() {
            return String.format("Memory[used=%dMB, free=%dMB, total=%dMB, max=%dMB, usage=%.1f%%]",
                usedMB, freeMB, totalMB, maxMB, usagePercent);
        }
    }
}
