package top.yumbo.ai.rag.query.impl;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.core.CacheEngine;
import top.yumbo.ai.rag.core.IndexEngine;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.query.CacheStatistics;
import top.yumbo.ai.rag.query.PagedResult;
import top.yumbo.ai.rag.query.QueryProcessor;
import top.yumbo.ai.rag.query.QueryRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 高级查询处理器实现（Advanced query processor implementation）
 * 支持缓存、过滤、排序、分页（Supports caching, filtering, sorting, and pagination）
 * 
 * 提供高级查询处理功能，包括缓存机制、分数阈值过滤、自定义排序和分页
 * (Provides advanced query processing capabilities, including caching mechanism, 
 * score threshold filtering, custom sorting, and pagination)
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class AdvancedQueryProcessor implements QueryProcessor {

    /**
     * 索引引擎 (Index engine)
     * 用于执行实际搜索的索引引擎
     * (Index engine used for performing actual searches)
     */
    private final IndexEngine indexEngine;
    
    /**
     * 缓存引擎 (Cache engine)
     * 用于缓存查询结果以提高性能
     * (Cache engine used to cache query results for performance improvement)
     */
    private final CacheEngine cacheEngine;

    /**
     * 构造函数 (Constructor)
     * 
     * @param indexEngine 索引引擎 (Index engine)
     * @param cacheEngine 缓存引擎 (Cache engine)
     */
    public AdvancedQueryProcessor(IndexEngine indexEngine, CacheEngine cacheEngine) {
        this.indexEngine = indexEngine;
        this.cacheEngine = cacheEngine;
    }

    /**
     * 处理查询请求 (Process query request)
     * 
     * 算法说明 (Algorithm description):
     * 1. 尝试从缓存获取结果 (Try to get result from cache)
     * 2. 构建内部查询对象 (Build internal query object)
     * 3. 执行索引搜索 (Execute index search)
     * 4. 应用分数阈值过滤 (Apply score threshold filtering)
     * 5. 应用自定义排序 (Apply custom sorting)
     * 6. 应用分页 (Apply pagination)
     * 7. 缓存结果 (Cache result)
     * 
     * @param request 查询请求 (Query request)
     * @return 查询结果 (Query result)
     */
    @Override
    public SearchResult process(QueryRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 尝试从缓存获取
        String cacheKey = request.getCacheKey();
        SearchResult cached = cacheEngine.getQueryResult(cacheKey);
        if (cached != null) {
            log.debug(I18N.get("log.query.cache_hit", request.getQueryText()));
            return cached;
        }

        // 2. 构建查询对象
        Query query = Query.builder()
                .queryText(request.getQueryText())
                .fields(request.getFields())
                .limit(request.getLimit() + request.getOffset())
                .offset(0)
                .filters(request.getFilters())
                .build();

        // 3. 执行索引搜索
        SearchResult result = indexEngine.search(query);

        // 4. 应用分数阈值过滤
        if (request.getMinScore() > 0) {
            result = applyScoreFilter(result, request.getMinScore());
        }

        // 5. 应用自定义排序
        if (request.getSortField() != null) {
            result = applyCustomSort(result, request.getSortField(), request.getSortOrder());
        }

        // 6. 应用分页
        result = applyPagination(result, request.getOffset(), request.getLimit());

        long queryTime = System.currentTimeMillis() - startTime;
        result.setQueryTimeMs(queryTime);

        // 7. 缓存结果
        cacheEngine.putQueryResult(cacheKey, result);

        log.info(I18N.get("log.query.processed", request.getQueryText(), result.getTotalHits(), queryTime));

        return result;
    }

    /**
     * 处理分页查询请求 (Process paged query request)
     * 
     * @param request 查询请求 (Query request)
     * @return 分页查询结果 (Paged query result)
     */
    @Override
    public PagedResult processPaged(QueryRequest request) {
        SearchResult result = process(request);
        int currentPage = request.getOffset() / request.getLimit();
        return new PagedResult(result, currentPage, request.getLimit());
    }

    /**
     * 清除缓存 (Clear cache)
     * 
     * 清除所有查询相关的缓存数据
     * (Clears all query-related cached data)
     */
    @Override
    public void clearCache() {
        // 清除查询缓存 (Clear query cache)
        cacheEngine.clear();
        log.info(I18N.get("log.query.cache_cleared"));
    }

    /**
     * 获取缓存统计信息 (Get cache statistics)
     * 
     * @return 缓存统计信息 (Cache statistics information)
     */
    @Override
    public CacheStatistics getCacheStatistics() {
        CacheEngine.CacheStats stats = cacheEngine.getStats();

        return CacheStatistics.builder()
                .documentCacheSize(stats.getSize())
                .queryCacheSize(stats.getSize())
                .documentCacheHits(stats.getHitCount())
                .documentCacheMisses(stats.getMissCount())
                .queryCacheHits(stats.getHitCount())
                .queryCacheMisses(stats.getMissCount())
                .totalHits(stats.getHitCount())
                .totalMisses(stats.getMissCount())
                .overallHitRate(stats.getHitRate())
                .build();
    }

    /**
     * 应用分数过滤 (Apply score filter)
     * 
     * 过滤掉分数低于指定阈值的文档
     * (Filters out documents with scores below the specified threshold)
     * 
     * @param result 原始搜索结果 (Original search result)
     * @param minScore 最小分数阈值 (Minimum score threshold)
     * @return 过滤后的搜索结果 (Filtered search result)
     */
    private SearchResult applyScoreFilter(SearchResult result, float minScore) {
        // getScoredDocuments()返回不可变列表，需要通过stream创建新列表
        // (getScoredDocuments() returns an immutable list, need to create a new list through stream)
        List<ScoredDocument> filtered = result.getScoredDocuments().stream()
                .filter(scored -> scored.getScore() >= minScore)
                .collect(Collectors.toList());

        return SearchResult.builder()
                .query(result.getQuery())
                .documents(filtered)
                .totalHits(filtered.size())
                .queryTimeMs(result.getQueryTimeMs())
                .build();
    }

    /**
     * 应用自定义排序 (Apply custom sorting)
     * 
     * 根据指定字段和排序方向对搜索结果进行排序
     * (Sorts search results according to the specified field and sort order)
     * 
     * @param result 原始搜索结果 (Original search result)
     * @param sortField 排序字段 (Sort field)
     * @param sortOrder 排序方向 (Sort order)
     * @return 排序后的搜索结果 (Sorted search result)
     */
    private SearchResult applyCustomSort(SearchResult result, String sortField,
                                        QueryRequest.SortOrder sortOrder) {
        // getScoredDocuments()返回不可变列表，创建可变副本用于排序
        // (getScoredDocuments() returns an immutable list, create a mutable copy for sorting)
        List<ScoredDocument> sorted = new ArrayList<>(result.getScoredDocuments());

        Comparator<ScoredDocument> comparator = null;

        switch (sortField) {
            case "score":
                comparator = Comparator.comparing(ScoredDocument::getScore);
                break;
            case "title":
                comparator = Comparator.comparing(doc -> doc.getDocument().getTitle());
                break;
            case "createdAt":
                comparator = Comparator.comparing(doc -> doc.getDocument().getCreatedAt());
                break;
            default:
                log.warn(I18N.get("log.query.unknown_sort", sortField));
                return result;
        }

        if (sortOrder == QueryRequest.SortOrder.DESC) {
            comparator = comparator.reversed();
        }

        sorted.sort(comparator);

        return SearchResult.builder()
                .query(result.getQuery())
                .documents(sorted)
                .totalHits(result.getTotalHits())
                .queryTimeMs(result.getQueryTimeMs())
                .build();
    }

    /**
     * 应用分页 (Apply pagination)
     * 
     * 根据偏移量和限制从搜索结果中提取指定页
     * (Extracts a specific page from search results based on offset and limit)
     * 
     * @param result 原始搜索结果 (Original search result)
     * @param offset 偏移量 (Offset)
     * @param limit 限制数量 (Limit)
     * @return 分页后的搜索结果 (Paginated search result)
     */
    private SearchResult applyPagination(SearchResult result, int offset, int limit) {
        // getScoredDocuments()返回不可变列表，subList创建视图（不复制数据）
        // (getScoredDocuments() returns an immutable list, subList creates a view without copying data)
        List<ScoredDocument> docs = result.getScoredDocuments();

        int fromIndex = Math.min(offset, docs.size());
        int toIndex = Math.min(offset + limit, docs.size());

        List<ScoredDocument> page = docs.subList(fromIndex, toIndex);

        return SearchResult.builder()
                .query(result.getQuery())
                .documents(page)
                .totalHits(result.getTotalHits())
                .queryTimeMs(result.getQueryTimeMs())
                .hasMore(toIndex < docs.size())
                .page(offset / limit)
                .pageSize(limit)
                .build();
    }
}
