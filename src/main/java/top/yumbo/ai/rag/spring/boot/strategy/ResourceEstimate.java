package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

/**
 * 资源估算
 * (Resource Estimate)
 */
@Data
@Builder
public class ResourceEstimate {

    /**
     * 预估Token消耗
     * (Estimated token consumption)
     */
    private long estimatedTokens;

    /**
     * 预估执行时间（毫秒）
     * (Estimated execution time in milliseconds)
     */
    private long estimatedTimeMs;

    /**
     * 预估内存消耗（MB）
     * (Estimated memory consumption in MB)
     */
    private long estimatedMemoryMb;

    /**
     * 预估成本（美元）
     * (Estimated cost in USD)
     */
    private double estimatedCostUsd;

    /**
     * 估算置信度 0-1
     * (Estimation confidence 0-1)
     */
    @Builder.Default
    private double confidenceLevel = 0.8;

    /**
     * 创建简单估算
     * (Create simple estimate)
     */
    public static ResourceEstimate simple(long tokens, long timeMs) {
        return ResourceEstimate.builder()
                .estimatedTokens(tokens)
                .estimatedTimeMs(timeMs)
                .build();
    }
}

