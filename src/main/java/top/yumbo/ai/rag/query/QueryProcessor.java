package top.yumbo.ai.rag.query;

import top.yumbo.ai.rag.model.SearchResult;

/**
 * 查询处理器接口（Query processor interface）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
public interface QueryProcessor {

    /**
     * 执行查询 (Execute query)
     * 
     * @param request 查询请求对象 (Query request object)
     * @return 查询结果 (Search result)
     */
    SearchResult process(QueryRequest request);

    /**
     * 执行分页查询 (Execute paged query)
     * 
     * @param request 查询请求对象 (Query request object)
     * @return 分页查询结果 (Paged query result)
     */
    PagedResult processPaged(QueryRequest request);

    /**
     * 清除查询缓存 (Clear query cache)
     * 清空所有查询相关的缓存数据
     * (Clears all query-related cached data)
     */
    void clearCache();

    /**
     * 获取缓存统计 (Get cache statistics)
     * 
     * @return 缓存统计信息 (Cache statistics information)
     */
    CacheStatistics getCacheStatistics();
}
