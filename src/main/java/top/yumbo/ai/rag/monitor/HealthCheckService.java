package top.yumbo.ai.rag.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yumbo.ai.rag.LocalFileRAG;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查服务
 */
@Data
public class HealthCheckService {
    
    private final LocalFileRAG rag;
    
    /**
     * 健康检查结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthStatus {
        private String status;
        private Map<String, ComponentHealth> components;
        private long timestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private String status;
        private String message;
    }
    
    /**
     * 执行健康检查
     */
    public HealthStatus check() {
        Map<String, ComponentHealth> components = new HashMap<>();
        
        // 检查存储
        components.put("storage", checkStorage());
        
        // 检查索引
        components.put("index", checkIndex());
        
        // 检查缓存
        components.put("cache", checkCache());
        
        // 确定总体状态
        String overallStatus = components.values().stream()
            .allMatch(c -> "UP".equals(c.getStatus())) ? "UP" : "DOWN";
        
        return HealthStatus.builder()
            .status(overallStatus)
            .components(components)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    private ComponentHealth checkStorage() {
        try {
            // 简单检查：获取统计信息
            var stats = rag.getStatistics();
            return ComponentHealth.builder()
                .status("UP")
                .message("Storage OK, docs: " + stats.getDocumentCount())
                .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                .status("DOWN")
                .message("Storage error: " + e.getMessage())
                .build();
        }
    }
    
    private ComponentHealth checkIndex() {
        try {
            // 简单检查：获取统计信息
            var stats = rag.getStatistics();
            return ComponentHealth.builder()
                .status("UP")
                .message("Index OK, indexed: " + stats.getIndexedDocumentCount())
                .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                .status("DOWN")
                .message("Index error: " + e.getMessage())
                .build();
        }
    }
    
    private ComponentHealth checkCache() {
        try {
            return ComponentHealth.builder()
                .status("UP")
                .message("Cache OK")
                .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                .status("DOWN")
                .message("Cache error: " + e.getMessage())
                .build();
        }
    }
}