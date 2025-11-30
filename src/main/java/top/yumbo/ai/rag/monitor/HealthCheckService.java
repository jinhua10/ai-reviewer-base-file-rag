package top.yumbo.ai.rag.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.service.LocalFileRAG;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查服务（Health check service）
 */
@Data
public class HealthCheckService {
    
    private final LocalFileRAG rag;
    
    /**
     * 健康检查结果（Health check result）
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
     * 执行健康检查（Execute health check）
     */
    public HealthStatus check() {
        Map<String, ComponentHealth> components = new HashMap<>();
        
        // 检查存储（Check storage）
        components.put("storage", checkStorage());
        
        // 检查索引（Check index）
        components.put("index", checkIndex());
        
        // 检查缓存（Check cache）
        components.put("cache", checkCache());
        
        // 确定总体状态（Determine overall status）
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
            // 简单检查：获取统计信息（Simple check: get statistics）
            var stats = rag.getStatistics();
            return ComponentHealth.builder()
                .status("UP")
                .message(LogMessageProvider.getMessage("log.monitor.health.storage_ok", stats.getDocumentCount()))
                .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                .status("DOWN")
                .message(LogMessageProvider.getMessage("log.monitor.health.storage_error", e.getMessage()))
                .build();
        }
    }
    
    private ComponentHealth checkIndex() {
        try {
            // 简单检查：获取统计信息（Simple check: get statistics）
            var stats = rag.getStatistics();
            return ComponentHealth.builder()
                .status("UP")
                .message(LogMessageProvider.getMessage("log.monitor.health.index_ok", stats.getIndexedDocumentCount()))
                .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                .status("DOWN")
                .message(LogMessageProvider.getMessage("log.monitor.health.index_error", e.getMessage()))
                .build();
        }
    }
    
    private ComponentHealth checkCache() {
        try {
            return ComponentHealth.builder()
                .status("UP")
                .message(LogMessageProvider.getMessage("log.monitor.health.cache_ok"))
                .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                .status("DOWN")
                .message(LogMessageProvider.getMessage("log.monitor.health.cache_error", e.getMessage()))
                .build();
        }
    }
}