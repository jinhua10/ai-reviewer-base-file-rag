package top.yumbo.ai.rag.api.controller;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.api.model.ApiResponse;
import top.yumbo.ai.rag.query.impl.AdvancedQueryProcessor;

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
     * 健康检查
     */
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(health);
    }
    
    /**
     * 统计信息
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
            log.error("Failed to get stats", e);
            return ApiResponse.error("Failed to get stats: " + e.getMessage());
        }
    }
    
    /**
     * 优化索引
     */
    public ApiResponse<String> optimizeIndex() {
        try {
            rag.optimizeIndex();
            log.info("Index optimized");
            return ApiResponse.success("Index optimized successfully", "OK");
            
        } catch (Exception e) {
            log.error("Failed to optimize index", e);
            return ApiResponse.error("Failed to optimize index: " + e.getMessage());
        }
    }
    
    /**
     * 清除缓存
     */
    public ApiResponse<String> clearCache() {
        try {
            queryProcessor.clearCache();
            log.info("Cache cleared");
            return ApiResponse.success("Cache cleared successfully", "OK");
            
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return ApiResponse.error("Failed to clear cache: " + e.getMessage());
        }
    }
}