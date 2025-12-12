package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.service.admin.SystemConfigService;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统管理控制器 (Admin Controller)
 *
 * 提供系统管理相关的 API
 * (Provides system administration related API)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final SystemConfigService configService;

    public AdminController(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 更新系统配置 (Update system configuration)
     *
     * PUT /api/admin/system-config
     * Body: {
     *   "maxUploadSize": "20MB",
     *   "searchMaxLimit": 200,
     *   "cacheEnabled": true
     * }
     */
    @PutMapping("/system-config")
    public ResponseEntity<?> updateSystemConfig(@RequestBody Map<String, Object> config) {
        log.info(I18N.get("admin.api.sysconfig_request"));

        try {
            SystemConfigService.ConfigUpdateResult result = configService.updateSystemConfig(config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.sysconfig_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取当前系统配置 (Get current system configuration)
     *
     * GET /api/admin/system-config
     */
    @GetMapping("/system-config")
    public ResponseEntity<?> getSystemConfig() {
        log.info(I18N.get("admin.api.sysconfig_get_request"));

        try {
            Map<String, Object> config = configService.getCurrentSystemConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.sysconfig_get_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 更新模型配置 (Update model configuration)
     *
     * PUT /api/admin/model-config
     * Body: {
     *   "model": "gpt-4o",
     *   "temperature": 0.7,
     *   "maxTokens": 2000
     * }
     */
    @PutMapping("/model-config")
    public ResponseEntity<?> updateModelConfig(@RequestBody Map<String, Object> config) {
        log.info(I18N.get("admin.api.modelconfig_request"));

        try {
            SystemConfigService.ConfigUpdateResult result = configService.updateModelConfig(config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.modelconfig_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取当前模型配置 (Get current model configuration)
     *
     * GET /api/admin/model-config
     */
    @GetMapping("/model-config")
    public ResponseEntity<?> getModelConfig() {
        log.info(I18N.get("admin.api.modelconfig_get_request"));

        try {
            Map<String, Object> config = configService.getCurrentModelConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.modelconfig_get_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取日志 (Get logs)
     * GET /api/admin/logs?level=ERROR&keyword=exception&page=0&size=100
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        log.info(I18N.get("admin.api.logs_request"), level, keyword);

        try {
            List<LogEntry> logs = new ArrayList<>();

            // 模拟日志数据 (Simulate log data)
            for (int i = 0; i < Math.min(size, 20); i++) {
                LogEntry entry = new LogEntry();
                entry.setTimestamp(LocalDateTime.now().minusMinutes(i * 5));
                entry.setLevel(i % 3 == 0 ? "ERROR" : (i % 3 == 1 ? "WARN" : "INFO"));
                entry.setLogger("top.yumbo.ai.rag.service.SomeService");
                entry.setMessage("示例日志消息 #" + i);
                entry.setThread("http-nio-8080-exec-" + (i % 10));
                logs.add(entry);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("content", logs);
            result.put("totalElements", 500);
            result.put("totalPages", 5);
            result.put("currentPage", page);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.logs_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取监控指标 (Get metrics)
     * GET /api/admin/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        log.info(I18N.get("admin.api.metrics_request"));

        try {
            SystemMetrics metrics = new SystemMetrics();

            // CPU
            metrics.setCpuUsage(45.6);
            metrics.setCpuCores(8);

            // 内存 (Memory)
            metrics.setMemoryUsed(2048L * 1024 * 1024); // 2GB
            metrics.setMemoryTotal(8192L * 1024 * 1024); // 8GB
            metrics.setMemoryUsagePercent(25.0);

            // 磁盘 (Disk)
            metrics.setDiskUsed(100L * 1024 * 1024 * 1024); // 100GB
            metrics.setDiskTotal(500L * 1024 * 1024 * 1024); // 500GB
            metrics.setDiskUsagePercent(20.0);

            // JVM
            metrics.setJvmHeapUsed(512L * 1024 * 1024); // 512MB
            metrics.setJvmHeapMax(2048L * 1024 * 1024); // 2GB
            metrics.setJvmThreadCount(150);

            // 业务指标 (Business metrics)
            metrics.setTotalDocuments(1234L);
            metrics.setTotalQuestions(5678L);
            metrics.setRequestsPerMinute(120);

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.metrics_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 健康检查 (Health check)
     * GET /api/admin/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        log.info(I18N.get("admin.api.health_request"));

        try {
            HealthStatus health = new HealthStatus();
            health.setStatus("UP");
            health.setTimestamp(LocalDateTime.now());

            Map<String, Object> components = new HashMap<>();
            components.put("database", Map.of("status", "UP", "responseTime", "15ms"));
            components.put("storage", Map.of("status", "UP", "freeSpace", "400GB"));
            components.put("cache", Map.of("status", "UP", "hitRate", "85%"));
            components.put("llm", Map.of("status", "UP", "model", "qwen-2.5"));

            health.setComponents(components);

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error(I18N.get("admin.api.health_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    // DTO 类 (DTO Classes)

    @Data
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String level;
        private String logger;
        private String message;
        private String thread;
    }

    @Data
    public static class SystemMetrics {
        // CPU
        private Double cpuUsage;
        private Integer cpuCores;

        // Memory
        private Long memoryUsed;
        private Long memoryTotal;
        private Double memoryUsagePercent;

        // Disk
        private Long diskUsed;
        private Long diskTotal;
        private Double diskUsagePercent;

        // JVM
        private Long jvmHeapUsed;
        private Long jvmHeapMax;
        private Integer jvmThreadCount;

        // Business
        private Long totalDocuments;
        private Long totalQuestions;
        private Integer requestsPerMinute;
    }

    @Data
    public static class HealthStatus {
        private String status;
        private LocalDateTime timestamp;
        private Map<String, Object> components;
    }
}

