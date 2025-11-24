package top.yumbo.ai.rag.impl.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.core.CacheEngine;
import top.yumbo.ai.rag.model.Document;

import java.time.Duration;

/**
 * Caffeine缓存引擎实现
 * 使用Caffeine提供高性能的多级缓存
 * 
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class CaffeineCacheEngine implements CacheEngine {
    
    private final Cache<String, Document> documentCache;
    private final Cache<String, Object> queryCache;
    
    public CaffeineCacheEngine(RAGConfiguration.CacheConfig config) {
        // 文档缓存
        this.documentCache = Caffeine.newBuilder()
                .maximumSize(config.getDocumentCacheSize())
                .expireAfterAccess(Duration.ofSeconds(config.getDocumentCacheTtlSeconds()))
                .recordStats()
                .build();
        
        // 查询结果缓存
        this.queryCache = Caffeine.newBuilder()
                .maximumSize(config.getQueryCacheSize())
                .expireAfterWrite(Duration.ofSeconds(config.getQueryCacheTtlSeconds()))
                .recordStats()
                .build();
        
        log.info("CaffeineCacheEngine initialized: docCache={}, queryCache={}",
                config.getDocumentCacheSize(), config.getQueryCacheSize());
    }
    
    @Override
    public Document getDocument(String docId) {
        Document doc = documentCache.getIfPresent(docId);
        if (doc != null) {
            log.debug("Document cache hit: {}", docId);
        }
        return doc;
    }
    
    @Override
    public void putDocument(String docId, Document document) {
        documentCache.put(docId, document);
        log.debug("Document cached: {}", docId);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getQueryResult(String queryKey) {
        Object result = queryCache.getIfPresent(queryKey);
        if (result != null) {
            log.debug("Query cache hit: {}", queryKey);
        }
        return (T) result;
    }
    
    @Override
    public <T> void putQueryResult(String queryKey, T result) {
        queryCache.put(queryKey, result);
        log.debug("Query result cached: {}", queryKey);
    }
    
    @Override
    public void invalidateDocument(String docId) {
        documentCache.invalidate(docId);
        log.debug("Document cache invalidated: {}", docId);
    }
    
    @Override
    public void clear() {
        documentCache.invalidateAll();
        queryCache.invalidateAll();
        log.info("All caches cleared");
    }
    
    @Override
    public CacheEngine.CacheStats getStats() {
        // 使用完整类名避免冲突
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineDocStats = documentCache.stats();
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineQueryStats = queryCache.stats();
        
        return new CacheStatsImpl(
                caffeineDocStats.hitCount() + caffeineQueryStats.hitCount(),
                caffeineDocStats.missCount() + caffeineQueryStats.missCount(),
                caffeineDocStats.evictionCount() + caffeineQueryStats.evictionCount(),
                documentCache.estimatedSize() + queryCache.estimatedSize()
        );
    }
    
    /**
     * 缓存统计实现
     */
    private static class CacheStatsImpl implements CacheEngine.CacheStats {
        private final long hitCount;
        private final long missCount;
        private final long evictionCount;
        private final long size;
        
        public CacheStatsImpl(long hitCount, long missCount, long evictionCount, long size) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.evictionCount = evictionCount;
            this.size = size;
        }
        
        @Override
        public long getHitCount() {
            return hitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
        
        @Override
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0.0 : (double) hitCount / total;
        }
        
        @Override
        public long getEvictionCount() {
            return evictionCount;
        }
        
        @Override
        public long getSize() {
            return size;
        }
    }
}

