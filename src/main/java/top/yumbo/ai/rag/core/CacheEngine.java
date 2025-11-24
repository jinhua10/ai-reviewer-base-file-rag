package top.yumbo.ai.rag.core;

import top.yumbo.ai.rag.model.Document;

/**
 * 缓存引擎接口
 * 提供多级缓存支持
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface CacheEngine {

    /**
     * 获取缓存的文档
     *
     * @param docId 文档ID
     * @return 文档对象，如果缓存未命中则返回null
     */
    Document getDocument(String docId);

    /**
     * 缓存文档
     *
     * @param docId 文档ID
     * @param document 文档对象
     */
    void putDocument(String docId, Document document);

    /**
     * 获取缓存的查询结果
     *
     * @param queryKey 查询键
     * @return 查询结果，如果缓存未命中则返回null
     */
    <T> T getQueryResult(String queryKey);

    /**
     * 缓存查询结果
     *
     * @param queryKey 查询键
     * @param result 查询结果
     */
    <T> void putQueryResult(String queryKey, T result);

    /**
     * 使文档缓存失效
     *
     * @param docId 文档ID
     */
    void invalidateDocument(String docId);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计对象
     */
    CacheStats getStats();

    /**
     * 缓存统计信息
     */
    interface CacheStats {
        long getHitCount();
        long getMissCount();
        double getHitRate();
        long getEvictionCount();
        long getSize();
    }
}

