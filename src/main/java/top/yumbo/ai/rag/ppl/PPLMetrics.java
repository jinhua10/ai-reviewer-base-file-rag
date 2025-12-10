package top.yumbo.ai.rag.ppl;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * PPL 服务性能指标 (PPL Service Performance Metrics)
 * 
 * 用于收集和统计 PPL 服务的各种性能数据，包括调用次数、延迟、缓存命中率等
 * (Used to collect and statistics various performance data of PPL service, 
 * including call count, latency, cache hit rate, etc.)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
public class PPLMetrics {

    // 调用统计 (Call statistics)
    /**
     * 总调用次数 (Total call count)
     */
    private final AtomicLong totalCalls = new AtomicLong(0);
    
    /**
     * 成功调用次数 (Success call count)
     */
    private final AtomicLong successCalls = new AtomicLong(0);
    
    /**
     * 失败调用次数 (Failed call count)
     */
    private final AtomicLong failedCalls = new AtomicLong(0);

    // 延迟统计 (Latency statistics)
    /**
     * 总延迟时间（毫秒）(Total latency time in milliseconds)
     */
    private final DoubleAdder totalLatencyMs = new DoubleAdder();
    
    /**
     * 平均延迟时间（毫秒）(Average latency time in milliseconds)
     */
    private volatile double avgLatencyMs = 0.0;
    
    /**
     * 50分位延迟时间（毫秒）(50th percentile latency time in milliseconds)
     */
    private volatile double p50LatencyMs = 0.0;
    
    /**
     * 95分位延迟时间（毫秒）(95th percentile latency time in milliseconds)
     */
    private volatile double p95LatencyMs = 0.0;
    
    /**
     * 99分位延迟时间（毫秒）(99th percentile latency time in milliseconds)
     */
    private volatile double p99LatencyMs = 0.0;

    // 缓存统计（仅 ONNX）(Cache statistics - only for ONNX)
    /**
     * 缓存命中次数 (Cache hit count)
     */
    private final AtomicLong cacheHits = new AtomicLong(0);
    
    /**
     * 缓存未命中次数 (Cache miss count)
     */
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // 成本统计（仅 OpenAI）(Cost statistics - only for OpenAI)
    /**
     * 总成本（美元）(Total cost in USD)
     */
    private final DoubleAdder totalCost = new DoubleAdder();
    
    /**
     * 总Token数 (Total token count)
     */
    private final AtomicLong totalTokens = new AtomicLong(0);

    /**
     * 记录一次成功调用 (Record a successful call)
     * 
     * @param latencyMs 延迟时间（毫秒）(Latency time in milliseconds)
     */
    public void recordSuccess(long latencyMs) {
        totalCalls.incrementAndGet();
        successCalls.incrementAndGet();
        totalLatencyMs.add(latencyMs);
        updateAvgLatency();
    }

    /**
     * 记录一次失败调用 (Record a failed call)
     * 
     * @param latencyMs 延迟时间（毫秒）(Latency time in milliseconds)
     */
    public void recordFailure(long latencyMs) {
        totalCalls.incrementAndGet();
        failedCalls.incrementAndGet();
        totalLatencyMs.add(latencyMs);
        updateAvgLatency();
    }

    /**
     * 记录缓存命中 (Record a cache hit)
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * 记录缓存未命中 (Record a cache miss)
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * 记录 API 成本 (Record API cost)
     * 
     * @param cost 成本（美元）(Cost in USD)
     * @param tokens Token数量 (Token count)
     */
    public void recordCost(double cost, long tokens) {
        totalCost.add(cost);
        totalTokens.addAndGet(tokens);
    }

    /**
     * 获取缓存命中率 (Get cache hit rate)
     * 
     * @return 缓存命中率，范围 [0.0, 1.0] (Cache hit rate, range [0.0, 1.0])
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * 获取成功率 (Get success rate)
     * 
     * @return 成功率，范围 [0.0, 1.0] (Success rate, range [0.0, 1.0])
     */
    public double getSuccessRate() {
        long total = totalCalls.get();
        return total > 0 ? (double) successCalls.get() / total : 0.0;
    }

    /**
     * 更新平均延迟 (Update average latency)
     * 在每次调用成功或失败后自动调用 (Automatically called after each successful or failed call)
     */
    private void updateAvgLatency() {
        long calls = totalCalls.get();
        if (calls > 0) {
            avgLatencyMs = totalLatencyMs.sum() / calls;
        }
    }

    /**
     * 重置所有指标 (Reset all metrics)
     * 将所有统计数据清零 (Clear all statistical data to zero)
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

