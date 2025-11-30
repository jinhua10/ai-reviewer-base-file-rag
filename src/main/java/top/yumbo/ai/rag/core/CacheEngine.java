package top.yumbo.ai.rag.core;

import top.yumbo.ai.rag.model.Document;

/**
 * 缓存引擎接口 (Cache engine interface)
 * 提供多级缓存支持 (Provides multi-level cache support)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface CacheEngine {

    /**
     * 获取缓存的文档 (Get cached document)
     *
     * @param docId 文档ID (document ID)
     * @return 文档对象，如果缓存未命中则返回null (document object, returns null if cache miss)
     */
    Document getDocument(String docId);

    /**
     * 缓存文档 (Cache document)
     *
     * @param docId 文档ID (document ID)
     * @param document 文档对象 (document object)
     */
    void putDocument(String docId, Document document);

    /**
     * 获取缓存的查询结果 (Get cached query result)
     *
     * @param queryKey 查询键 (query key)
     * @return 查询结果，如果缓存未命中则返回null (query result, returns null if cache miss)
     */
    <T> T getQueryResult(String queryKey);

    /**
     * 缓存查询结果 (Cache query result)
     *
     * @param queryKey 查询键 (query key)
     * @param result 查询结果 (query result)
     */
    <T> void putQueryResult(String queryKey, T result);

    /**
     * 使文档缓存失效 (Invalidate document cache)
     *
     * @param docId 文档ID (document ID)
     */
    void invalidateDocument(String docId);

    /**
     * 清空所有缓存 (Clear all cache)
     */
    void clear();

    /**
     * 获取缓存统计信息 (Get cache statistics)
     *
     * @return 缓存统计对象 (cache statistics object)
     */
    CacheStats getStats();

    /**
     * 缓存统计信息 (Cache statistics)
     */
    interface CacheStats {
        long getHitCount();
        long getMissCount();
        double getHitRate();
        long getEvictionCount();
        long getSize();
    }
}
