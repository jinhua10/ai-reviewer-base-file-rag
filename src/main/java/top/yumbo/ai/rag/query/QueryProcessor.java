package top.yumbo.ai.rag.query;

import top.yumbo.ai.rag.model.SearchResult;

/**
 * 查询处理器接口
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
public interface QueryProcessor {

    /**
     * 执行查询
     */
    SearchResult process(QueryRequest request);

    /**
     * 执行分页查询
     */
    PagedResult processPaged(QueryRequest request);

    /**
     * 清除查询缓存
     */
    void clearCache();

    /**
     * 获取缓存统计
     */
    CacheStatistics getCacheStatistics();
}

