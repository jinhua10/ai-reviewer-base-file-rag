package top.yumbo.ai.rag.hope.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.hope.HOPEConfig;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.monitor.HOPEMetrics;
import top.yumbo.ai.rag.hope.monitor.HOPEMonitorService;
import top.yumbo.ai.rag.hope.monitor.KnowledgeQualityService;

import java.util.HashMap;
import java.util.Map;

/**
 * HOPE 监控 API 控制器
 * (HOPE Monitor API Controller)
 *
 * 提供 HOPE 系统的监控和管理接口
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@RestController
@RequestMapping("/api/hope")
public class HOPEMonitorController {

    private final HOPEConfig config;
    private final HOPEKnowledgeManager hopeManager;
    private final HOPEMonitorService monitorService;
    private final KnowledgeQualityService qualityService;

    @Autowired
    public HOPEMonitorController(HOPEConfig config,
                                  HOPEKnowledgeManager hopeManager,
                                  HOPEMonitorService monitorService,
                                  KnowledgeQualityService qualityService) {
        this.config = config;
        this.hopeManager = hopeManager;
        this.monitorService = monitorService;
        this.qualityService = qualityService;
    }

    /**
     * 获取 HOPE 系统状态
     * GET /api/hope/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", config.isEnabled());
        status.put("config", Map.of(
            "permanent", Map.of(
                "storagePath", config.getPermanent().getStoragePath(),
                "directAnswerConfidence", config.getPermanent().getDirectAnswerConfidence()
            ),
            "ordinary", Map.of(
                "storagePath", config.getOrdinary().getStoragePath(),
                "retentionDays", config.getOrdinary().getRetentionDays(),
                "similarityThreshold", config.getOrdinary().getSimilarityThreshold()
            ),
            "highFrequency", Map.of(
                "storage", config.getHighFrequency().getStorage(),
                "sessionTimeoutMinutes", config.getHighFrequency().getSessionTimeoutMinutes()
            )
        ));
        return ResponseEntity.ok(status);
    }

    /**
     * 获取完整仪表盘数据
     * GET /api/hope/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(monitorService.getDashboard());
    }

    /**
     * 获取性能指标
     * GET /api/hope/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<HOPEMetrics.MetricsSummary> getMetrics() {
        return ResponseEntity.ok(monitorService.getMetricsSummary());
    }

    /**
     * 获取三层统计信息
     * GET /api/hope/layers
     */
    @GetMapping("/layers")
    public ResponseEntity<Map<String, Object>> getLayerStats() {
        if (!config.isEnabled()) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        return ResponseEntity.ok(hopeManager.getStatistics());
    }

    /**
     * 获取健康状态
     * GET /api/hope/health
     */
    @GetMapping("/health")
    public ResponseEntity<HOPEMonitorService.HealthStatus> getHealth() {
        return ResponseEntity.ok(monitorService.getHealthStatus());
    }

    /**
     * 获取知识质量评估报告
     * GET /api/hope/quality
     */
    @GetMapping("/quality")
    public ResponseEntity<KnowledgeQualityService.QualityReport> getQualityReport() {
        return ResponseEntity.ok(qualityService.assess());
    }

    /**
     * 重置监控指标
     * POST /api/hope/metrics/reset
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        monitorService.resetMetrics();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Metrics reset successfully"
        ));
    }

    /**
     * 手动触发知识晋升检查
     * POST /api/hope/promote
     */
    @PostMapping("/promote")
    public ResponseEntity<Map<String, Object>> triggerPromotion() {
        if (!config.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "HOPE is disabled"
            ));
        }

        // 这里可以调用中频层的晋升检查
        // ordinaryLayer.checkAndPromote();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Promotion check triggered"
        ));
    }

    /**
     * 测试 HOPE 查询
     * POST /api/hope/test
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testQuery(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String sessionId = request.getOrDefault("sessionId", "test-session");

        if (question == null || question.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Question is required"
            ));
        }

        long startTime = System.currentTimeMillis();
        var result = hopeManager.smartQuery(question, sessionId);
        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("needsLLM", result.isNeedsLLM());
        response.put("sourceLayer", result.getSourceLayer());
        response.put("confidence", result.getConfidence());
        response.put("answer", result.getAnswer());
        response.put("hasSkillTemplate", result.hasSkillTemplate());
        response.put("hasSimilarReference", result.hasSimilarReference());
        response.put("processingTimeMs", elapsed);
        response.put("strategy", hopeManager.getStrategy(question, result).name());

        return ResponseEntity.ok(response);
    }

    /**
     * 添加临时定义
     * POST /api/hope/definition
     */
    @PostMapping("/definition")
    public ResponseEntity<Map<String, Object>> addDefinition(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String term = request.get("term");
        String definition = request.get("definition");

        if (sessionId == null || term == null || definition == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "sessionId, term, and definition are required"
            ));
        }

        hopeManager.addTempDefinition(sessionId, term, definition);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Definition added for session: " + sessionId
        ));
    }

    /**
     * 清除会话
     * DELETE /api/hope/session/{sessionId}
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        hopeManager.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Session cleared: " + sessionId
        ));
    }
}

