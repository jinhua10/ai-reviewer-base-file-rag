package top.yumbo.ai.rag.api.controller;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.api.model.ApiResponse;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.query.QueryRequest;
import top.yumbo.ai.rag.query.impl.AdvancedQueryProcessor;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

@Slf4j
public class SearchController {
    
    private final LocalFileRAG rag;
    private final AdvancedQueryProcessor queryProcessor;
    
    public SearchController(LocalFileRAG rag) {
        this.rag = rag;
        this.queryProcessor = new AdvancedQueryProcessor(
            rag.getIndexEngine(), 
            rag.getCacheEngine()
        );
    }
    
    /**
     * 基本搜索 (Basic search)
     */
    public ApiResponse<SearchResult> search(String requestBody) {
        try {
            Query query = JSON.parseObject(requestBody, Query.class);
            SearchResult result = rag.search(query);
            
            log.info(LogMessageProvider.getMessage("log.search.completed", query.getQueryText(), result.getTotalHits()));
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.search.failed"), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.search.failed_detail", e.getMessage()));
        }
    }
    
    /**
     * 高级搜索 (Advanced search)
     */
    public ApiResponse<SearchResult> advancedSearch(String requestBody) {
        try {
            QueryRequest request = JSON.parseObject(requestBody, QueryRequest.class);
            SearchResult result = queryProcessor.process(request);
            
            log.info(LogMessageProvider.getMessage("log.search.advanced_completed", request.getQueryText(), result.getTotalHits()));
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.search.advanced_failed"), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.search.advanced_failed_detail", e.getMessage()));
        }
    }
}