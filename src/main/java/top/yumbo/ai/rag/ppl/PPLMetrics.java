package top.yumbo.ai.rag.ppl;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * PPL 服务性能指标
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
public class PPLMetrics {

    // 调用统计
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong successCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);

    // 延迟统计
    private final DoubleAdder totalLatencyMs = new DoubleAdder();
    private volatile double avgLatencyMs = 0.0;
    private volatile double p50LatencyMs = 0.0;
    private volatile double p95LatencyMs = 0.0;
    private volatile double p99LatencyMs = 0.0;

    // 缓存统计（仅 ONNX）
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // 成本统计（仅 OpenAI）
    private final DoubleAdder totalCost = new DoubleAdder();
    private final AtomicLong totalTokens = new AtomicLong(0);

    /**
     * 记录一次成功调用
     */
    public void recordSuccess(long latencyMs) {
        totalCalls.incrementAndGet();
        successCalls.incrementAndGet();
        totalLatencyMs.add(latencyMs);
        updateAvgLatency();
    }

    /**
     * 记录一次失败调用
     */
    public void recordFailure(long latencyMs) {
        totalCalls.incrementAndGet();
        failedCalls.incrementAndGet();
        totalLatencyMs.add(latencyMs);
        updateAvgLatency();
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * 记录 API 成本
     */
    public void recordCost(double cost, long tokens) {
        totalCost.add(cost);
        totalTokens.addAndGet(tokens);
    }

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = totalCalls.get();
        return total > 0 ? (double) successCalls.get() / total : 0.0;
    }

    /**
     * 更新平均延迟
     */
    private void updateAvgLatency() {
        long calls = totalCalls.get();
        if (calls > 0) {
            avgLatencyMs = totalLatencyMs.sum() / calls;
        }
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalCalls.set(0);
        successCalls.set(0);
        failedCalls.set(0);
        totalLatencyMs.reset();
        avgLatencyMs = 0.0;
        p50LatencyMs = 0.0;
        p95LatencyMs = 0.0;
        p99LatencyMs = 0.0;
        cacheHits.set(0);
        cacheMisses.set(0);
        totalCost.reset();
        totalTokens.set(0);
    }

    /**
     * 获取总调用次数
     */
    public long getTotalCalls() {
        return totalCalls.get();
    }

    /**
     * 获取成功调用次数
     */
    public long getSuccessCalls() {
        return successCalls.get();
    }

    /**
     * 获取失败调用次数
     */
    public long getFailedCalls() {
        return failedCalls.get();
    }

    /**
     * 获取平均延迟
     */
    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    /**
     * 获取缓存命中次数
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * 获取缓存未命中次数
     */
    public long getCacheMisses() {
        return cacheMisses.get();
    }

    /**
     * 获取总成本
     */
    public double getTotalCost() {
        return totalCost.sum();
    }

    /**
     * 获取总 Token 数
     */
    public long getTotalTokens() {
        return totalTokens.get();
    }
}

