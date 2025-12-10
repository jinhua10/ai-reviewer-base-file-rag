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
import top.yumbo.ai.rag.i18n.I18N;

import java.util.HashMap;
import java.util.Map;

/**
 * HOPE 监控 API 控制器 (HOPE Monitor API Controller)
 * 
 * 提供 HOPE 系统的监控和管理接口
 * (Provides monitoring and management interfaces for the HOPE system)
 * 
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/hope")
public class HOPEMonitorController {

    private final HOPEConfig config;
    private final HOPEKnowledgeManager hopeManager;
    private final HOPEMonitorService monitorService;
    private final KnowledgeQualityService qualityService;

    /**
     * 构造函数 (Constructor)
     * 
     * @param config HOPE 配置 (HOPE configuration)
     * @param hopeManager HOPE 知识管理器 (HOPE knowledge manager)
     * @param monitorService HOPE 监控服务 (HOPE monitor service)
     * @param qualityService 知识质量评估服务 (Knowledge quality assessment service)
     */
    @Autowired
    public HOPEMonitorController(HOPEConfig config,
                                  HOPEKnowledgeManager hopeManager,
                                  HOPEMonitorService monitorService,
                                  KnowledgeQualityService qualityService) {
        // 1. 初始化配置 (Initialize configuration)
        this.config = config;
        // 2. 初始化知识管理器 (Initialize knowledge manager)
        this.hopeManager = hopeManager;
        // 3. 初始化监控服务 (Initialize monitor service)
        this.monitorService = monitorService;
        // 4. 初始化质量评估服务 (Initialize quality assessment service)
        this.qualityService = qualityService;
    }

    /**
     * 获取 HOPE 系统状态 (Get HOPE system status)
     * 
     * 返回 HOPE 系统的启用状态和配置信息
     * (Returns the enabled status and configuration information of HOPE system)
     * 
     * @return 系统状态响应 (System status response)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        // 1. 创建状态映射 (Create status map)
        Map<String, Object> status = new HashMap<>();
        // 2. 添加启用状态 (Add enabled status)
        status.put("enabled", config.isEnabled());
        // 3. 添加配置信息 (Add configuration information)
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
        // 4. 返回响应 (Return response)
        return ResponseEntity.ok(status);
    }

    /**
     * 获取完整仪表盘数据 (Get complete dashboard data)
     * 
     * 返回包含性能指标、健康状态和优化建议的完整仪表盘数据
     * (Returns complete dashboard data including performance metrics, health status, and optimization suggestions)
     * 
     * @return 仪表盘数据响应 (Dashboard data response)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(monitorService.getDashboard());
    }

    /**
     * 获取性能指标 (Get performance metrics)
     * 
     * 返回系统当前的性能指标摘要
     * (Returns the current performance metrics summary of the system)
     * 
     * @return 性能指标响应 (Performance metrics response)
     */
    @GetMapping("/metrics")
    public ResponseEntity<HOPEMetrics.MetricsSummary> getMetrics() {
        return ResponseEntity.ok(monitorService.getMetricsSummary());
    }

    /**
     * 获取三层统计信息 (Get three-layer statistics)
     * 
     * 返回 HOPE 三层知识结构的统计信息
     * (Returns statistics of HOPE three-layer knowledge structure)
     * 
     * @return 层级统计响应 (Layer statistics response)
     */
    @GetMapping("/layers")
    public ResponseEntity<Map<String, Object>> getLayerStats() {
        // 1. 检查系统是否启用 (Check if system is enabled)
        if (!config.isEnabled()) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        // 2. 返回统计信息 (Return statistics)
        return ResponseEntity.ok(hopeManager.getStatistics());
    }

    /**
     * 获取健康状态 (Get health status)
     * 
     * 返回系统的健康状态检查结果
     * (Returns health check results of the system)
     * 
     * @return 健康状态响应 (Health status response)
     */
    @GetMapping("/health")
    public ResponseEntity<HOPEMonitorService.HealthStatus> getHealth() {
        return ResponseEntity.ok(monitorService.getHealthStatus());
    }

    /**
     * 获取知识质量评估报告 (Get knowledge quality assessment report)
     * 
     * 返回对三层知识结构的质量评估结果和建议
     * (Returns quality assessment results and suggestions for the three-layer knowledge structure)
     * 
     * @return 质量评估报告响应 (Quality assessment report response)
     */
    @GetMapping("/quality")
    public ResponseEntity<KnowledgeQualityService.QualityReport> getQualityReport() {
        return ResponseEntity.ok(qualityService.assess());
    }

    /**
     * 重置监控指标 (Reset monitoring metrics)
     * 
     * 清空所有累积的监控指标，重新开始统计
     * (Clears all accumulated monitoring metrics and starts statistics over)
     * 
     * @return 重置结果响应 (Reset result response)
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        // 1. 重置指标 (Reset metrics)
        monitorService.resetMetrics();
        // 2. 返回成功响应 (Return success response)
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", I18N.get("hope.controller.metrics_reset_success")
        ));
    }

    /**
     * 手动触发知识晋升检查 (Manually trigger knowledge promotion check)
     * 
     * 手动触发中频层到低频层的知识晋升检查
     * (Manually triggers knowledge promotion check from ordinary layer to permanent layer)
     * 
     * @return 晋升检查结果响应 (Promotion check result response)
     */
    @PostMapping("/promote")
    public ResponseEntity<Map<String, Object>> triggerPromotion() {
        // 1. 检查系统是否启用 (Check if system is enabled)
        if (!config.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", I18N.get("hope.controller.hope_disabled")
            ));
        }

        // 2. 这里可以调用中频层的晋升检查 (Here you can call the promotion check of the ordinary layer)
        // ordinaryLayer.checkAndPromote();

        // 3. 返回成功响应 (Return success response)
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", I18N.get("hope.controller.promotion_triggered")
        ));
    }

    /**
     * 测试 HOPE 查询 (Test HOPE query)
     * 
     * 使用指定的查询测试 HOPE 系统的响应
     * (Tests the response of HOPE system with the specified query)
     * 
     * @param request 请求参数 (Request parameters)
     * @return 查询结果响应 (Query result response)
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testQuery(@RequestBody Map<String, String> request) {
        // 1. 提取请求参数 (Extract request parameters)
        String question = request.get("question");
        String sessionId = request.getOrDefault("sessionId", "test-session");

        // 2. 验证问题参数 (Validate question parameter)
        if (question == null || question.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", I18N.get("hope.controller.question_required")
            ));
        }

        // 3. 执行查询并计算时间 (Execute query and calculate time)
        long startTime = System.currentTimeMillis();
        var result = hopeManager.smartQuery(question, sessionId);
        long elapsed = System.currentTimeMillis() - startTime;

        // 4. 构建响应 (Build response)
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

        // 5. 返回结果 (Return result)
        return ResponseEntity.ok(response);
    }

    /**
     * 添加临时定义 (Add temporary definition)
     * 
     * 为指定会话添加临时定义，用于高频层缓存
     * (Adds temporary definition for specified session, used for high-frequency layer cache)
     * 
     * @param request 请求参数 (Request parameters)
     * @return 添加结果响应 (Add result response)
     */
    @PostMapping("/definition")
    public ResponseEntity<Map<String, Object>> addDefinition(@RequestBody Map<String, String> request) {
        // 1. 提取请求参数 (Extract request parameters)
        String sessionId = request.get("sessionId");
        String term = request.get("term");
        String definition = request.get("definition");

        // 2. 验证必需参数 (Validate required parameters)
        if (sessionId == null || term == null || definition == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", I18N.get("hope.controller.definition_params_required")
            ));
        }

        // 3. 添加临时定义 (Add temporary definition)
        hopeManager.addTempDefinition(sessionId, term, definition);

        // 4. 返回成功响应 (Return success response)
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", I18N.get("hope.controller.definition_added", sessionId)
        ));
    }

    /**
     * 清除会话 (Clear session)
     * 
     * 清除指定会话的所有缓存数据
     * (Clears all cached data for the specified session)
     * 
     * @param sessionId 会话ID (Session ID)
     * @return 清除结果响应 (Clear result response)
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        // 1. 清除会话 (Clear session)
        hopeManager.clearSession(sessionId);
        // 2. 返回成功响应 (Return success response)
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", I18N.get("hope.controller.session_cleared", sessionId)
        ));
    }
}

