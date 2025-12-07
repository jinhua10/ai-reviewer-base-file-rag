package top.yumbo.ai.rag.spring.boot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 检索结果缓存服务
 * (Search result cache service)
 *
 * 通过缓存相似查询的检索结果来提升性能
 * (Improves performance by caching search results for similar queries)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class SearchCacheService {

    private final KnowledgeQAProperties properties;

    // 检索结果缓存 (Search result cache)
    private Cache<String, CachedSearchResult> searchCache;

    // 查询扩展缓存 (Query expansion cache)
    private Cache<String, String> queryExpansionCache;

    @Autowired
    public SearchCacheService(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // 初始化检索结果缓存 (Initialize search result cache)
        searchCache = Caffeine.newBuilder()
            .maximumSize(getCacheMaxSize())
            .expireAfterWrite(getCacheTtlMinutes(), TimeUnit.MINUTES)
            .recordStats()
            .build();

        // 初始化查询扩展缓存 (Initialize query expansion cache)
        queryExpansionCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

        log.info(I18N.get("log.cache.init", getCacheMaxSize(), getCacheTtlMinutes()));
    }

    /**
     * 获取缓存的检索结果，如果不存在则执行检索
     * (Get cached search result, or execute search if not cached)
     *
     * @param question 查询问题 (Query question)
     * @param searchFn 检索函数 (Search function)
     * @return 检索结果 (Search result)
     */
    public List<Document> getCachedOrSearch(String question, Supplier<List<Document>> searchFn) {
        if (!isCacheEnabled()) {
            return searchFn.get();
        }

        String cacheKey = generateCacheKey(question);

        CachedSearchResult cached = searchCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug(I18N.get("log.cache.hit", question.length() > 30 ? question.substring(0, 30) + "..." : question));
            cached.incrementHitCount();
            return cached.getDocuments();
        }

        // 执行检索 (Execute search)
        long startTime = System.currentTimeMillis();
        List<Document> documents = searchFn.get();
        long elapsed = System.currentTimeMillis() - startTime;

        // 缓存结果 (Cache result)
        CachedSearchResult result = new CachedSearchResult();
        result.setDocuments(documents);
        result.setSearchTimeMs(elapsed);
        result.setHitCount(0);

        searchCache.put(cacheKey, result);
        log.debug(I18N.get("log.cache.miss", question.length() > 30 ? question.substring(0, 30) + "..." : question, elapsed));

        return documents;
    }

    /**
     * 获取缓存的查询扩展结果
     * (Get cached query expansion result)
     *
     * @param query 原始查询 (Original query)
     * @param expandFn 扩展函数 (Expansion function)
     * @return 扩展后的查询 (Expanded query)
     */
    public String getCachedOrExpand(String query, Supplier<String> expandFn) {
        return queryExpansionCache.get(query, k -> expandFn.get());
    }

    /**
     * 清除所有缓存
     * (Clear all caches)
     */
    public void clearAll() {
        searchCache.invalidateAll();
        queryExpansionCache.invalidateAll();
        log.info(I18N.get("log.cache.cleared"));
    }

    /**
     * 清除检索缓存
     * (Clear search cache)
     */
    public void clearSearchCache() {
        searchCache.invalidateAll();
        log.info(I18N.get("log.cache.search_cleared"));
    }

    /**
     * 获取缓存统计信息
     * (Get cache statistics)
     */
    public CacheStats getStats() {
        var stats = searchCache.stats();
        CacheStats result = new CacheStats();
        result.setHitCount(stats.hitCount());
        result.setMissCount(stats.missCount());
        result.setHitRate(stats.hitRate());
        result.setEvictionCount(stats.evictionCount());
        result.setCurrentSize(searchCache.estimatedSize());
        result.setMaxSize(getCacheMaxSize());
        return result;
    }

    /**
     * 生成缓存键
     * (Generate cache key)
     */
    private String generateCacheKey(String question) {
        // 简单的缓存键生成：规范化问题文本
        // (Simple cache key generation: normalize question text)
        return question.toLowerCase().trim()
            .replaceAll("\\s+", " ")  // 合并空白字符 (Merge whitespace)
            .replaceAll("[?？!！。，,.]+$", "");  // 移除尾部标点 (Remove trailing punctuation)
    }

    private boolean isCacheEnabled() {
        return properties.getKnowledgeBase() != null &&
               properties.getKnowledgeBase().isEnableCache();
    }

    private int getCacheMaxSize() {
        // 默认缓存 500 个查询结果 (Default cache 500 query results)
        return 500;
    }

    private int getCacheTtlMinutes() {
        // 默认缓存 30 分钟 (Default cache for 30 minutes)
        return 30;
    }

    /**
     * 缓存的检索结果
     * (Cached search result)
     */
    @Data
    public static class CachedSearchResult {
        private List<Document> documents;
        private long searchTimeMs;
        private int hitCount;

        public void incrementHitCount() {
            this.hitCount++;
        }
    }

    /**
     * 缓存统计信息
     * (Cache statistics)
     */
    @Data
    public static class CacheStats {
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long evictionCount;
        private long currentSize;
        private int maxSize;
    }
}

