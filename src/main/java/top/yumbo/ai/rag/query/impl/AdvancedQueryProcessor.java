package top.yumbo.ai.rag.query.impl;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.core.CacheEngine;
import top.yumbo.ai.rag.core.IndexEngine;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
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
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class AdvancedQueryProcessor implements QueryProcessor {

    private final IndexEngine indexEngine;
    private final CacheEngine cacheEngine;

    public AdvancedQueryProcessor(IndexEngine indexEngine, CacheEngine cacheEngine) {
        this.indexEngine = indexEngine;
        this.cacheEngine = cacheEngine;
    }

    @Override
    public SearchResult process(QueryRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 尝试从缓存获取
        String cacheKey = request.getCacheKey();
        SearchResult cached = cacheEngine.getQueryResult(cacheKey);
        if (cached != null) {
            log.debug(LogMessageProvider.getMessage("log.query.cache_hit", request.getQueryText()));
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

        log.info(LogMessageProvider.getMessage("log.query.processed", request.getQueryText(), result.getTotalHits(), queryTime));

        return result;
    }

    @Override
    public PagedResult processPaged(QueryRequest request) {
        SearchResult result = process(request);
        int currentPage = request.getOffset() / request.getLimit();
        return new PagedResult(result, currentPage, request.getLimit());
    }

    @Override
    public void clearCache() {
        // 清除查询缓存
        cacheEngine.clear();
        log.info(LogMessageProvider.getMessage("log.query.cache_cleared"));
    }

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
     * 应用分数过滤
     */
    private SearchResult applyScoreFilter(SearchResult result, float minScore) {
        // getScoredDocuments()返回不可变列表，需要通过stream创建新列表
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
     * 应用自定义排序
     */
    private SearchResult applyCustomSort(SearchResult result, String sortField,
                                        QueryRequest.SortOrder sortOrder) {
        // getScoredDocuments()返回不可变列表，创建可变副本用于排序
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
                log.warn(LogMessageProvider.getMessage("log.query.unknown_sort", sortField));
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
     * 应用分页
     */
    private SearchResult applyPagination(SearchResult result, int offset, int limit) {
        // getScoredDocuments()返回不可变列表，subList创建视图（不复制数据）
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
