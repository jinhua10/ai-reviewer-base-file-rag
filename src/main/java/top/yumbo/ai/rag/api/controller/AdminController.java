package top.yumbo.ai.rag.api.controller;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.api.model.ApiResponse;
import top.yumbo.ai.rag.query.impl.AdvancedQueryProcessor;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AdminController {
    
    private final LocalFileRAG rag;
    private final AdvancedQueryProcessor queryProcessor;
    
    public AdminController(LocalFileRAG rag) {
        this.rag = rag;
        this.queryProcessor = new AdvancedQueryProcessor(
            rag.getIndexEngine(), 
            rag.getCacheEngine()
        );
    }
    
    /**
     * 健康检查 (Health check)
     */
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(health);
    }
    
    /**
     * 统计信息 (Statistics)
     */
    public ApiResponse<Map<String, Object>> stats() {
        try {
            var statistics = rag.getStatistics();
            var cacheStats = queryProcessor.getCacheStatistics();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("documentCount", statistics.getDocumentCount());
            stats.put("indexedDocumentCount", statistics.getIndexedDocumentCount());
            stats.put("cacheStatistics", cacheStats);
            
            return ApiResponse.success(stats);
            
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.admin.stats_failed"), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.admin.stats_failed_detail", e.getMessage()));
        }
    }
    
    /**
     * 优化索引 (Optimize index)
     */
    public ApiResponse<String> optimizeIndex() {
        try {
            rag.optimizeIndex();
            log.info(LogMessageProvider.getMessage("log.admin.index_optimized"));
            return ApiResponse.success(LogMessageProvider.getMessage("log.admin.index_optimized_success"), "OK");

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.admin.index_optimize_failed"), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.admin.index_optimize_failed_detail", e.getMessage()));
        }
    }
    
    /**
     * 清除缓存 (Clear cache)
     */
    public ApiResponse<String> clearCache() {
        try {
            queryProcessor.clearCache();
            log.info(LogMessageProvider.getMessage("log.admin.cache_cleared"));
            return ApiResponse.success(LogMessageProvider.getMessage("log.admin.cache_cleared_success"), "OK");

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.admin.clear_cache_failed"), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.admin.clear_cache_failed_detail", e.getMessage()));
        }
    }
}