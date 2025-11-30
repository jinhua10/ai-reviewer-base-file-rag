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

    private long documentCacheSize;
    private long queryCacheSize;
    private long documentCacheHits;
    private long documentCacheMisses;
    private long queryCacheHits;
    private long queryCacheMisses;
    private long totalHits;
    private long totalMisses;
    private double overallHitRate;

    /**
     * 获取文档缓存命中率
     */
    public double getDocumentCacheHitRate() {
        long total = documentCacheHits + documentCacheMisses;
        return total == 0 ? 0.0 : (double) documentCacheHits / total;
    }

    /**
     * 获取查询缓存命中率
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
