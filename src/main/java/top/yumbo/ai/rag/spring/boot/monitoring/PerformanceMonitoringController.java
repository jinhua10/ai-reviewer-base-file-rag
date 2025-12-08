package top.yumbo.ai.rag.spring.boot.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能监控控制器
 * (Performance Monitoring Controller)
 *
 * 提供性能仪表盘 API
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class PerformanceMonitoringController {

    private final PerformanceMonitoringService monitoringService;

    @Autowired
    public PerformanceMonitoringController(PerformanceMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * 获取完整的性能仪表盘
     *
     * GET /api/monitoring/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            PerformanceMonitoringService.PerformanceDashboard dashboard =
                monitoringService.getDashboard();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dashboard", dashboard);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取性能仪表盘失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取 HOPE 性能指标
     *
     * GET /api/monitoring/hope
     */
    @GetMapping("/hope")
    public ResponseEntity<?> getHopeMetrics() {
        try {
            PerformanceMonitoringService.HopeMetrics metrics =
                monitoringService.getHopeMetrics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("metrics", metrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取 HOPE 指标失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取 LLM 性能指标
     *
     * GET /api/monitoring/llm
     */
    @GetMapping("/llm")
    public ResponseEntity<?> getLlmMetrics() {
        try {
            PerformanceMonitoringService.LlmMetrics metrics =
                monitoringService.getLlmMetrics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("metrics", metrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取 LLM 指标失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取缓存统计
     *
     * GET /api/monitoring/cache
     */
    @GetMapping("/cache")
    public ResponseEntity<?> getCacheStats() {
        try {
            Map<String, PerformanceMonitoringService.CacheStats> stats =
                monitoringService.getCacheStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cacheStats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取会话统计
     *
     * GET /api/monitoring/session
     */
    @GetMapping("/session")
    public ResponseEntity<?> getSessionMetrics() {
        try {
            PerformanceMonitoringService.SessionMetrics metrics =
                monitoringService.getSessionMetrics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("metrics", metrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取会话统计失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取最近的性能快照
     *
     * GET /api/monitoring/snapshots?limit=10
     */
    @GetMapping("/snapshots")
    public ResponseEntity<?> getRecentSnapshots(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PerformanceMonitoringService.PerformanceSnapshot> snapshots =
                monitoringService.getRecentSnapshots(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", snapshots.size());
            response.put("snapshots", snapshots);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取性能快照失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 重置统计数据
     *
     * POST /api/monitoring/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetStatistics() {
        try {
            monitoringService.reset();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "统计数据已重置");

            log.info("性能统计已通过 API 重置");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("重置统计数据失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 健康检查
     *
     * GET /api/monitoring/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "PerformanceMonitoring");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}

