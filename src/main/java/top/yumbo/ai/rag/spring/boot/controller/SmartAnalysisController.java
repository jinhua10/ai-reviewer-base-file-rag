package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.SmartAnalysisService;
import top.yumbo.ai.rag.spring.boot.strategy.AnalysisResult;
import top.yumbo.ai.rag.spring.boot.strategy.StrategyDispatcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能分析控制器
 * (Smart Analysis Controller)
 *
 * 提供智能多文档分析的 API 端点
 */
@RestController
@RequestMapping("/api/document-qa")
@Slf4j
public class SmartAnalysisController {

    @Autowired
    private SmartAnalysisService smartAnalysisService;

    /**
     * 智能分析接口
     * (Smart analysis endpoint)
     *
     * 前端调用此接口进行智能多文档分析
     */
    @PostMapping("/analyze-smart")
    public ResponseEntity<Map<String, Object>> analyzeSmart(@RequestBody SmartAnalysisRequestDTO request) {
        log.info("📊 Received smart analysis request: {} documents, goal: {}",
                request.getDocumentPaths() != null ? request.getDocumentPaths().size() : 0,
                request.getGoalId());

        try {
            // 构建服务请求
            SmartAnalysisService.SmartAnalysisRequest serviceRequest =
                    SmartAnalysisService.SmartAnalysisRequest.builder()
                            .documentPaths(request.getDocumentPaths())
                            .question(request.getQuestion())
                            .goalId(request.getGoalId())
                            .strategies(request.getStrategies())
                            .advancedParams(request.getAdvancedParams())
                            .language(request.getLanguage())
                            .maxTokens(request.getMaxTokens())
                            .useKnowledgeBase(request.isUseKnowledgeBase())
                            .build();

            // 执行分析
            AnalysisResult result = smartAnalysisService.analyzeSmartly(serviceRequest);

            // 转换为响应格式
            Map<String, Object> response = convertToResponse(result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Smart analysis failed", e);

            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取可用策略列表
     * (Get available strategies)
     */
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, Object>> getStrategies() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("strategies", smartAnalysisService.getAvailableStrategies());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取策略统计
     * (Get strategy statistics)
     */
    @GetMapping("/strategies/stats")
    public ResponseEntity<Map<String, Object>> getStrategyStats() {
        Map<String, Object> response = new LinkedHashMap<>();

        Map<String, StrategyDispatcher.StrategyStats> stats = smartAnalysisService.getStrategyStats();
        Map<String, Object> formattedStats = new LinkedHashMap<>();

        stats.forEach((id, stat) -> {
            Map<String, Object> statInfo = new LinkedHashMap<>();
            statInfo.put("totalExecutions", stat.totalExecutions);
            statInfo.put("successCount", stat.successCount);
            statInfo.put("successRate", String.format("%.2f%%", stat.getSuccessRate() * 100));
            statInfo.put("averageExecutionTime", stat.getAverageExecutionTime() + "ms");
            formattedStats.put(id, statInfo);
        });

        response.put("stats", formattedStats);
        return ResponseEntity.ok(response);
    }

    /**
     * 转换为响应格式
     */
    private Map<String, Object> convertToResponse(AnalysisResult result) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", result.isSuccess());

        if (result.isSuccess()) {
            response.put("answer", result.getAnswer());
            response.put("comprehensiveSummary", result.getComprehensiveSummary());
            response.put("finalReport", result.getFinalReport());

            if (result.getKeyPoints() != null && !result.getKeyPoints().isEmpty()) {
                response.put("keyPoints", result.getKeyPoints());
            }

            if (result.getRelations() != null && !result.getRelations().isEmpty()) {
                response.put("relations", result.getRelations());
            }

            if (result.getComparison() != null) {
                response.put("comparison", result.getComparison());
            }

            if (result.getCausalChain() != null && !result.getCausalChain().isEmpty()) {
                response.put("causalChain", result.getCausalChain());
            }

            response.put("strategiesUsed", result.getStrategiesUsed());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            response.put("tokensUsed", result.getTokensUsed());

            if (result.getMetadata() != null) {
                response.put("metadata", result.getMetadata());
            }
        } else {
            response.put("error", result.getErrorMessage());
        }

        return response;
    }

    /**
     * 请求DTO
     */
    @Data
    public static class SmartAnalysisRequestDTO {
        private List<String> documentPaths;
        private String question;
        private String goalId;
        private List<String> strategies;
        private Map<String, Object> advancedParams;
        private String language;
        private int maxTokens;
        private boolean useKnowledgeBase;
    }
}

