package top.yumbo.ai.rag.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存统计信息（Cache statistics）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatistics {

    /**
     * 文档缓存大小 (Document cache size)
     * 当前文档缓存中的条目数量
     * (Number of entries in the current document cache)
     */
    private long documentCacheSize;
    
    /**
     * 查询缓存大小 (Query cache size)
     * 当前查询缓存中的条目数量
     * (Number of entries in the current query cache)
     */
    private long queryCacheSize;
    
    /**
     * 文档缓存命中次数 (Document cache hit count)
     * 文档缓存的命中次数
     * (Number of document cache hits)
     */
    private long documentCacheHits;
    
    /**
     * 文档缓存未命中次数 (Document cache miss count)
     * 文档缓存的未命中次数
     * (Number of document cache misses)
     */
    private long documentCacheMisses;
    
    /**
     * 查询缓存命中次数 (Query cache hit count)
     * 查询缓存的命中次数
     * (Number of query cache hits)
     */
    private long queryCacheHits;
    
    /**
     * 查询缓存未命中次数 (Query cache miss count)
     * 查询缓存的未命中次数
     * (Number of query cache misses)
     */
    private long queryCacheMisses;
    
    /**
     * 总命中次数 (Total hit count)
     * 所有类型的缓存命中总数
     * (Total number of all types of cache hits)
     */
    private long totalHits;
    
    /**
     * 总未命中次数 (Total miss count)
     * 所有类型的缓存未命中总数
     * (Total number of all types of cache misses)
     */
    private long totalMisses;
    
    /**
     * 整体命中率 (Overall hit rate)
     * 所有缓存类型的综合命中率
     * (Combined hit rate for all cache types)
     */
    private double overallHitRate;

    /**
     * 获取文档缓存命中率 (Get document cache hit rate)
     * 计算文档缓存的命中率
     * (Calculates the hit rate of document cache)
     * 
     * @return 文档缓存命中率 (Document cache hit rate)
     */
    public double getDocumentCacheHitRate() {
        long total = documentCacheHits + documentCacheMisses;
        return total == 0 ? 0.0 : (double) documentCacheHits / total;
    }

    /**
     * 获取查询缓存命中率 (Get query cache hit rate)
     * 计算查询缓存的命中率
     * (Calculates the hit rate of query cache)
     * 
     * @return 查询缓存命中率 (Query cache hit rate)
     */
    public double getQueryCacheHitRate() {
        long total = queryCacheHits + queryCacheMisses;
        return total == 0 ? 0.0 : (double) queryCacheHits / total;
    }

    @Override
    public String toString() {
        return String.format(
            "CacheStatistics{docCache=%d, queryCache=%d, hitRate=%.2f%%, " +
            "docHits=%d, docMisses=%d, queryHits=%d, queryMisses=%d}",
            documentCacheSize, queryCacheSize, overallHitRate * 100,
            documentCacheHits, documentCacheMisses,
            queryCacheHits, queryCacheMisses
        );
    }
}
